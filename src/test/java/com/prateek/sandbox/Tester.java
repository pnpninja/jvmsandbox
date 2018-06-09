package com.prateek.sandbox;

import org.apache.commons.io.IOUtils;
import com.prateek.sandbox.classloader.MyClassLoader;

import com.prateek.sandbox.runtime.Account;

import java.io.FileReader;
import java.io.Reader;
import java.io.StringWriter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class Tester {
    static HashMap<String, byte[]> classMap = new HashMap<String, byte[]>();

    static MyClassLoader bcl = new MyClassLoader(classMap);

    public static String run(final String className, final long maxMemory, final long maxBytecodes) throws Exception {
        System.out.println("================================" + className + "================================");
        Object key = Account.get().push(maxMemory, maxBytecodes);
        Account.bcl = bcl; // TODO(TFK): Remove this hack.
        String resultString = "";
        try {
            Class c = bcl.loadClass(className);
            Method m = c.getMethod("main");

            resultString = "Result: " + m.invoke(null);
        } catch (InvocationTargetException e) {
            resultString = "Caught (unwrapped) " + e.getCause();
        } catch (Exception e) {
            resultString = "Caught " + e.getCause();
        } finally {
            Account.get().pop(key);
        }

        return resultString;
    }

    public static void main(String[] args) throws Exception {
        //prepareFile("InfiniteLoop");
        prepareFile("BasicMemoryDemos");
        prepareFile("InfiniteMemory");
        prepareFile("BigInstructionBlock");
        //prepareFile("ScriptsInfiniteLoop");
        prepareFile("ScriptsInfiniteMemory");
        prepareFile("InfiniteCatch");
        prepareFile("FileTest");
        prepareFile("SocketTest");
        prepareFile("ScriptsGood");
        prepareFile("GarbageCollectionPass");
        prepareFile("GarbageCollectionFail");

        System.setProperty("java.security.policy", "resources/Test.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        System.out.println(run("BasicMemoryDemos", 100000000, 100000000));
        System.out.println(run("GarbageCollectionPass", 1000000, Long.MAX_VALUE));
        try {
            System.out.println(run("GarbageCollectionFail", 1000000, Long.MAX_VALUE));
        } catch (Exception e) {
            System.out.println("PASSED - Expected exception caught.");
        }
        System.out.println(run("SocketTest", 100000, 10000000));
        System.out.println(run("FileTest", 100000, 1000000));
        //System.out.println(run("InfiniteLoop", 50000, 50000));
        System.out.println(run("InfiniteMemory", 100000, 100000));
        System.out.println(run("BigInstructionBlock", 50000, 10000));
        //System.out.println(run("ScriptsInfiniteLoop", Long.MAX_VALUE, 100000));
        System.out.println(run("ScriptsGood", 100000000, 100000000));

        //System.out.println(run("ScriptsInfiniteMemory", 100000, Long.MAX_VALUE));
//        System.out.println(run("InfiniteCatch", 50000, 50000));

    }

    public static void prepareFile(String className) throws Exception {
        //System.out.println("Preparing " + className);
        classMap.put(
                className,
                Compiler.compile(
                        className,
                        loadFile("src/main/resources/" + className + ".java")
                )
        );
    }

    public static String loadFile(String name) throws Exception {
        Reader input = new FileReader(name);
        StringWriter output = new StringWriter();
        try {
            IOUtils.copy(input, output);
        } finally {
            input.close();
        }
        String fileContents = output.toString();
        return fileContents;
    }
}

