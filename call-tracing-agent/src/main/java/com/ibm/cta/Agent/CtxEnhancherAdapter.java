package com.ibm.cta.Agent;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

import static com.ibm.cta.Agent.Transformer.*;

public class CtxEnhancherAdapter extends ClassVisitor {
    private String className;
    private boolean optInstrumentCGE;
    private ClassLoader loader;
    private String superName;

    private static Map<ClassLoader, Set<String>> seenClasses = new ConcurrentHashMap<>();

    // Dummy class loader object representing the "null" system class loader.
    private static ClassLoader nullLoader;
    static {
        try {
            nullLoader = new URLClassLoader(new URL[] { new URL("http://dummy") });
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public CtxEnhancherAdapter(ClassWriter cw, String className, ClassLoader loader) {
        super(Opcodes.ASM5, cw);
        this.className = className;
        this.loader = loader;
    }

    // Remember the superclass. This information is used by method
    // visitors to find this()/super() inside constructors.
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.superName = superName;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access,
            String name,
            String desc,
            String signature,
            String[] exceptions) {
        MethodVisitor defaultVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (defaultVisitor == null)
            stopWithError("NULL default method visitor");

        // Native methods are ignored.
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            debugMessage("Ignoring native method: " + name + " in " + className);
            return defaultVisitor;
        }
        if (!canTransformClass(className, loader)) {
            debugMessage("Ignoring class " + className);
            return defaultVisitor;
        }
        debugMessage("Visiting method: " + className + "." + name + desc);

        // Non-static, non-abstract methods can be subject to
        // call-graph edge instrumentation.
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        boolean instrCGE = (!isStatic) && (!isAbstract) && optInstrumentCGE;

        return new MethodEntryAdapter(access, name, desc, defaultVisitor, className, superName, instrCGE, isStatic,
                loader);

    }

    private static boolean canTransformClass(String name, ClassLoader loader) {
        synchronized (seenClasses) {
            String nameDots = name.replace("/", ".");

            // Use a dummy object for the bootstrap loader.
            if (loader == null)
                loader = nullLoader;
            // Get the loaded classes for this loader and check that the
            // class has not already been transformed.
            Set<String> classesForLoader = seenClasses.get(loader);
            if (classesForLoader == null) {
                classesForLoader = new HashSet<>();
                seenClasses.put(loader, classesForLoader);
            } else if (classesForLoader.contains(name)) {
                debugMessage("Class has already been transformed, is no longer interesting: " + name);
                return false;
            }

            if (nameDots.startsWith("Instrumentation"))
                return false;
            if (nameDots.startsWith("heapdl"))
                return false;
            if (nameDots.equals("java.lang.Integer"))
                return false;
            if (nameDots.equals("java.lang.String"))
                return false;
            // if (nameDots.equals("java.lang.StringBuilder"))
            // return false;
            debugMessage("interesting class: " + nameDots + " [loader=" + loader + ']');
            classesForLoader.add(nameDots);
            return true;
        }
    }

    static class MethodEntryAdapter extends AdviceAdapter {
        private String className, methName, desc;

        // Instruction index (first, second, ...). Not bytecode index.
        private int instrNum = -1;

        public MethodEntryAdapter(int access, String methName, String desc,
                MethodVisitor mv, String className,
                String superName, boolean instrCGE,
                boolean isStatic, ClassLoader loader) {
            super(Opcodes.ASM5, mv, access, methName, desc);
            this.className = className;
            this.methName = methName;
            this.desc = desc;
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            instrNum++;
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            instrNum++;
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitInsn(int opcode) {
            instrNum++;
            super.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            instrNum++;
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            instrNum++;
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            instrNum++;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            instrNum++;
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            instrNum++;
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            instrNum++;
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            instrNum++;
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitMethodInsn(int opcode,
                String owner,
                String name,
                String desc,
                boolean itf) {

            // Ignore existing calls to this tracing agent.
            if (owner.contains("com/ibm/cta") || name.equals("<init>")) {
                // if (owner.contains("com/ibm/cta")) {
                debugMessage("Ignoring invocations of `com.ibm.cta` classes and `init` classes.");
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            } else {

                // Record the call using com.ibm.cta.Recorder.Recorder::recordCall(),
                // with caller and callee as their arguments.

                String caller = getMethName();
                String callee = owner.replace("/", ".") + "." + name + desc;
                ;
                debugMessage("Found invocation instruction from method:" + methName + " to method: " + name);

                recordCall(caller, callee);

                // Generate the original invocation instruction.
                super.visitMethodInsn(opcode, owner, name, desc, itf);

                // FIXME: Is there a better way to handle return instruction?
                // Record the return using com.ibm.cta.Recorder.Recorder::recordCall(),
                // but now with callee and caller as their arguments.
                recordCall(callee, caller);

            }

        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            instrNum++;
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            instrNum++;
            super.visitMultiANewArrayInsn(desc, dims);
        }

        private String getMethName() {
            return className.replace("/", ".") + "." + methName + desc;
        }

        // Records call graph edge
        private void recordCall(String caller, String callee) {
            super.visitLdcInsn(caller); // Push caller at stack id N - this will be arg-0 (top of the stack)
            super.visitLdcInsn(callee); // Push callee at stack id N+1 - this will be arg-1
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/ibm/cta/Recorder/Recorder",
                    "recordCall",
                    "(Ljava/lang/String;Ljava/lang/String;)V", false);
        }

        @Override
        public void visitEnd() {
            debugMessage("End of " + getMethName() +
                    ", instrNum = " + instrNum);
            super.visitEnd();
        }
    }
}
