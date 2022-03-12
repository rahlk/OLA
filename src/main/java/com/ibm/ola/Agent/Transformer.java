/*
 * 
 */
package com.ibm.ola.Agent;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class Transformer implements ClassFileTransformer {

    public static final String CTXT_AGENT = "Context-Agent: ";
    private final String outDir = "out";

    public boolean someLibraryMethod() {
        return true;
    }
}
