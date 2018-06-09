package com.prateek.sandbox.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import com.prateek.sandbox.agent.JavaAgent;
import com.prateek.sandbox.runtime.Recorder;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the class which actually performs the Instrumentation of the
 * Java bytecode using ASM. It is activated in sandbox.agent.JavaAgent
 */
public class Transformer implements ClassFileTransformer {
    public Transformer() {
    }

    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> cls,
                            ProtectionDomain protectionDomain,
                            byte[] origBytes) {

        Map<String, BasicBlocksRecord> methodBasicBlocksMap;

        Recorder.disabled.enable();
        //Recorder.disabled_cl.enable();
        //Recorder.disabled_ic.enable();
        /**
         * Skip instrumenting classes which cause problems.
         *
         * Not sure why all these guys need to be skipped, but they cause
         * red words to appear if they're not. Ideally we would figure out why.
         */
        if (className.startsWith("java/lang/Shutdown") ||
                className.startsWith("java/lang/Thread") || // needed to prevent infinite recursion, due to usage of .currentThread()
                className.startsWith("sun/security/provider/PolicyFile$PolicyEntry") || // this seems to take forever
                className.startsWith("sandbox/") || // don't want to instrument ourselves to avoid infinite recursion
                className.startsWith("com/sun/tools") || // to make the initial compilation faster
                className.startsWith("com/sun/source")// to make the initial compilation faster
                ) {

            if (com.prateek.sandbox.Compiler.VERBOSE) {
                System.out.println("Skipping");
            }
            return origBytes;
        }
        if (com.prateek.sandbox.Compiler.VERBOSE) {
            System.out.println("Transforming " + className);
        }
        /* Instantiate method basic blocks hashmap for this class */
        methodBasicBlocksMap = new HashMap<String, BasicBlocksRecord>();

        /* First pass instrumentation */
        instrument(origBytes, loader, methodBasicBlocksMap);
        /* Second pass instrumentation */
        byte[] result = instrument(origBytes, loader, methodBasicBlocksMap);
        Recorder.disabled.disable();
        //Recorder.disabled_cl.disable();
        //Recorder.disabled_ic.disable();
        return result;
    }

    /**
     * Given the bytes representing a class, go through all the bytecode in it and
     * instrument any occurences of new/newarray/anewarray/multianewarray with
     * pre- and post-allocation hooks.  Even more fun, intercept calls to the
     * reflection API's Array.newInstance() and instrument those too.
     *
     * @param originalBytes the original <code>byte[]</code> code.
     * @return the instrumented <code>byte[]</code> code.
     */
    private static byte[] instrument(byte[] originalBytes, ClassLoader loader, Map<String, BasicBlocksRecord> methodBasicBlocksMap) {
        try {

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassAdapter adapter = new ClassAdapter(cw, methodBasicBlocksMap);
            ClassReader cr = new ClassReader(originalBytes);
            cr.accept(adapter, ClassReader.SKIP_FRAMES);

            byte[] output = cw.toByteArray();

            return output;
        } catch (RuntimeException e) {
            //System.out.println("Failed to instrument class: " + e);
            //e.printStackTrace();
            throw e;
        } catch (Error e) {
            //System.out.println("Failed to instrument class: " + e);
            //e.printStackTrace();
            throw e;
        }
    }
}
