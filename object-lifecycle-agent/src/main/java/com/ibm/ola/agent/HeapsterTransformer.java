package com.ibm.ola.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.ibm.ola.recorder.Recorder;

public class HeapsterTransformer extends ClassVisitor {
	
	/**
	 * JVM doesn't like it when you call any method between object creation and its <init> method. I thought it was just
	 * the compiler, but I get errors, when I attempt this. Hence, I must make the dup call when I create the object,
	 * but only call "Recorder.record(...)" after <init> is called. This requires using both the visitInsn and visitMethodInsn methods.
	 */

	public static final String RECORDER_SLASH_NAME = Recorder.class.getName().replace('.', '/');
	
	String className = null;
	String superName = null;
	
	public HeapsterTransformer(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}
		
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
		this.superName = superName;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new HeapsterMethodVisitor(name, access, desc, super.visitMethod(access, name, desc, signature, exceptions));
	}
	
	boolean isIgnorableConstructorCall(String className, String containingMethodName, String owner, String superName) {
		if (owner.equals(className) && containingMethodName.equals("<init>")) return true;
		else if (owner.equals("java/lang/Object")) return true;
		else return superName.equals(owner);	
	}

	class HeapsterMethodVisitor extends LocalVariablesSorter {
		
		String methodName;

		public HeapsterMethodVisitor(String methodName, int access, String desc, MethodVisitor mv) {
			super(Opcodes.ASM5, access, desc, mv);
			this.methodName = methodName;
		}
				
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>") && 
					!isIgnorableConstructorCall(className, methodName, owner, superName)) {// we don't care about constructors calling constructors
					mv.visitMethodInsn(opcode,  owner,  name,  desc, isInterface);
					addRecorderDotRecord();
			}
			else if (opcode == Opcodes.INVOKEVIRTUAL && 
					(owner.equals("java/lang/Class") && name.equals("newInstance")) ||
					(owner.equals("java/lang/Object") && name.equals("clone")) ||
					(owner.equals("java/io/ObjectInputStream") && name.equals("readObject"))
					) {
				mv.visitMethodInsn(opcode,  owner,  name,  desc, isInterface);
                addDup();
                addRecorderDotRecord();
			}
		    else {
		        mv.visitMethodInsn(opcode,  owner,  name,  desc, isInterface);
		    }
		}
		
		public void visitTypeInsn(int opcode, String type) {
			mv.visitTypeInsn(opcode,  type);
			if (opcode == Opcodes.NEW) {
				addDup();
			}
			else if (opcode == Opcodes.ANEWARRAY) {
				addDup();
				addRecorderDotRecord();
			}
	    }
		
		public void visitMultiANewArrayInsn(String desc, int dims) {
            mv.visitMultiANewArrayInsn(desc,  dims);
            addDup();
            addRecorderDotRecord();
		}
		
		public void visitIntInsn(int opcode, int operand)  {
            mv.visitIntInsn(opcode,  operand);

			if (opcode == Opcodes.NEWARRAY || opcode == Opcodes.MULTIANEWARRAY) {
				addDup();
				addRecorderDotRecord();
			}
		}
		
		void addDup() {
			mv.visitInsn(Opcodes.DUP);
		}
		
		void addRecorderDotRecord() {
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, Recorder.class.getName().replace('.', '/'), "record", "(Ljava/lang/Object;)V", false);
		}
	}
}
