package com.ibm.ola.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.ibm.ola.agent.HeapsterAgent;
import com.ibm.ola.agent.HeapsterTransformer;
import com.ibm.ola.util.IOUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class ClassLoaderGenerator {

    static final String OUTPUT_JAR_NAME = "heapster-classloader.jar";
    static final String ASM_COPYRIGHT_NOTICE = "ASM-copyright-notice.txt";
    
    public static void printUsage() {
        printUsage(null);
    }
    
    public static void printUsage(String message) {
        StringBuilder buffer = new StringBuilder();
        if (message != null) {
            buffer.append(message);
            buffer.append("\n\n");
        }
        buffer.append("Usage:\n\t");
        buffer.append("java " + ClassLoaderGenerator.class.getName() + " <path to rt.jar> <output directory>");
        buffer.append("\n\n");
        buffer.append("or");
        buffer.append("\n\n");
        buffer.append("java -jar " + OUTPUT_JAR_NAME + " <path to rt.jar> <output directory>");
        System.out.println(buffer);
        System.exit(1);
    }
    
    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            printUsage();
        }
        else {
            try {
                File directory = new File(args[1]);
                if (!directory.exists()) 
                    printUsage("Directory " + directory.getAbsolutePath() + " does not exist.");
                else if (!directory.isDirectory()) 
                    printUsage("Path " + directory.getAbsolutePath() + " is not a directory.");
                
                File file = new File(args[0]);
                if (!file.exists()) 
                    printUsage("File " + file.getAbsolutePath() + " does not exist.");
                else if (!file.isFile()) 
                    printUsage("Path " + file.getAbsolutePath() + " is not a file.");
                    
                JarFile jar = new JarFile(file);
                ZipEntry entry = jar.getEntry("java/lang/ClassLoader.class");
                InputStream is = jar.getInputStream(entry);
                byte bytes[] = IOUtil.copy(is);
                is.close();
                
                byte newBytes[] = generate(bytes);
//                Util.checkBytes(newBytes);
                
                File outputFile = new File(directory, OUTPUT_JAR_NAME);
                FileOutputStream fos = new FileOutputStream(outputFile);
                JarOutputStream jos = new JarOutputStream(fos);
                entry = new ZipEntry("java/lang/ClassLoader.class");
                jos.putNextEntry(entry);
                jos.write(newBytes);
                
                addHeapsterResources(jos);
                
                jos.close();
                fos.close();
            }
            catch (Exception e) {
                printUsage("An unexpected error occured while creating " + OUTPUT_JAR_NAME + ".");
                e.printStackTrace();
            }
        }
    }
    
    private static final Pattern UNWANTED_HEAPSTER_CLASS_PATTERN = 
        Pattern.compile("ca/discotek/heapster/HeapsterAgent.*|ca/discotek/heapster/HeapsterTransformer.*|ca/discotek/heapstertest/.*");
    
    static void addHeapsterResources(JarOutputStream jos) throws IOException, URISyntaxException  {
        URL url = ClassLoaderGenerator.class.getProtectionDomain().getCodeSource().getLocation();
        String path = url.toURI().getPath();
        File file = new File(path);
        JarFile jar = new JarFile(file);
        Enumeration<JarEntry> entries = jar.entries();
        JarEntry entry;
        String name;
        InputStream is;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            name = entry.getName();
            if (UNWANTED_HEAPSTER_CLASS_PATTERN.matcher(name).matches()) continue;
            else {
                jos.putNextEntry(entry);
                is = jar.getInputStream(entry);
                IOUtil.transfer(is, jos);
                jos.closeEntry();
            }
        }
    }
    
    static byte[] generate(byte classLoaderBytes[]) {
        ClassReader cr = new ClassReader(classLoaderBytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassLoaderClassVisitor cv = new ClassLoaderClassVisitor(cw);
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}
