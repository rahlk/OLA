package com.ibm.ola.classloader;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import com.ibm.ola.agent.Logger;
import com.ibm.ola.recorder.Recorder;

public class ClassLoaderClassVisitor extends ClassVisitor {
	
    Logger logger = new Logger();
    
	static List<String> includedMethodNameList = new ArrayList<String>();
	static {
		includedMethodNameList.add("defineClass0");
		includedMethodNameList.add("defineClass1");
		includedMethodNameList.add("defineClass2");
	}
	
	public ClassLoaderClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}
		
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new MyMethodVisitor(name, access, desc, super.visitMethod(access, name, desc, signature, exceptions));
	}
	
	class MyMethodVisitor extends LocalVariablesSorter {
		
		String methodName;

		public MyMethodVisitor(String methodName, int access, String desc, MethodVisitor mv) {
			super(Opcodes.ASM5, access, desc, mv);
			this.methodName = methodName;
		}
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			mv.visitMethodInsn(opcode,  owner,  name,  desc, isInterface);
			
			if (opcode == Opcodes.INVOKEVIRTUAL &&
				includedMethodNameList.contains(name)) {
				logger.info("Instrumenting method invocation: " + owner + "." + name + ": " + desc);

				int variableIndex = newLocal(Type.getType(Class.class));
				visitVarInsn(Opcodes.ASTORE, variableIndex);
				visitVarInsn(Opcodes.ALOAD, variableIndex);
				addRecorderDotRecord();
				visitVarInsn(Opcodes.ALOAD, variableIndex);
			}
		}
		
		void addRecorderDotRecord() {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Recorder.class.getName().replace('.', '/'), "record", "(Ljava/lang/Object;)V", false);
		}
	}
	
}
