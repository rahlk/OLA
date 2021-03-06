package com.ibm.cta.Agent;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class Transformer implements ClassFileTransformer {

    // Debugging flags.
    private final static boolean debug = true;
    private final static boolean saveBytecode = true;

    public static final String CTXT_AGENT = "Call-Trace-Agent: ";
    private final String outDir = "out";

    public Transformer() {
        if (debug)
            System.err.println("Call-Trace-Agent: Debugging has been turned on");
        if (saveBytecode)
            System.err.println("Call-Trace-Agent: Transformed bytecode is saved under \"" + outDir + "\"");
    }

    public static synchronized void premain(String args, Instrumentation inst)
            throws ClassNotFoundException, IOException {
        Transformer t = new Transformer();
        inst.addTransformer(t);
        if (inst.isNativeMethodPrefixSupported()) {
            String nativePrefix = "Recorder$record$";
            debugMessage("Using native prefix: \"" + nativePrefix + "\"");
            inst.setNativeMethodPrefix(t, nativePrefix);
        }
    }

    private static boolean isLibraryClass(String name) {
        return name.contains("java") || // <- All java.* classes
                name.contains("sun") || // <- All sun.* classes
                name.contains("com/ibm/cta"); // <- All instrumenter classes
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain pd, byte[] classFile) throws IllegalClassFormatException {

        if (isLibraryClass(className))
            return null;

        debugMessage("Transforming: " + className + " [loader=" + loader + ']');

        // Save original bytecode.
        if (saveBytecode)
            debugWriteClass(className + ".orig", classFile);

        ClassReader reader = new ClassReader(classFile);
        // If using COMPUTE_FRAMES, also enable the JSRInlinerAdapter
        // in CtxEnhancherAdapter.
        int wFlags = ClassWriter.COMPUTE_MAXS; // | ClassWriter.COMPUTE_FRAMES;
        ClassWriter writer = new ContextClassWriter(loader, wFlags);
        ClassVisitor ctxAdapter = new CtxEnhancherAdapter(writer, className, loader);
        try {
            ClassNode cn = new ClassNode(Opcodes.ASM5);
            reader.accept(cn, ClassReader.EXPAND_FRAMES);
            cn.accept(ctxAdapter);
        } catch (RuntimeException ex) {
            System.err.println(CTXT_AGENT + "ASM error visiting class " + className);
            ex.printStackTrace();
            System.exit(-1);
        }

        byte[] ret = writer.toByteArray();

        // Save transformed bytecode.
        if (saveBytecode)
            debugWriteClass(className, ret);
        // Check bytecode for correctness.
        if (debug)
            checkBytecode(loader, className, ret, wFlags);

        return ret;
    }

    // Check bytecode using ASM's CheckClassAdapter.
    private void checkBytecode(ClassLoader loader, String className,
            byte[] bc, int wFlags) {
        ClassReader debugReader = new ClassReader(bc);
        ClassWriter debugWriter = new ContextClassWriter(loader, wFlags);
        try {
            ClassNode debugNode = new ClassNode(Opcodes.ASM5);
            debugReader.accept(debugNode, ClassReader.EXPAND_FRAMES);
            debugNode.accept(new CheckClassAdapter(debugWriter));
            // debugReader.accept(new CheckClassAdapter(debugWriter), 0);
        } catch (RuntimeException ex) {
            System.err.println("Bytecode check failed for " + className + ":");
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private void debugWriteClass(String className, byte[] bytecode) {
        try {
            (new java.io.File(outDir)).mkdir();
            OutputStream out = new FileOutputStream(outDir + "/" + className.replace("/", "_") + ".class");
            out.write(bytecode);
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static void debugMessage(String msg) {
        if (debug) {
            System.err.println(CTXT_AGENT + msg);
            System.err.flush();
        }
    }

    public static void stopWithError(String msg) {
        System.err.println(CTXT_AGENT + msg);
        System.exit(-1);
    }
}
