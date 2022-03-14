package com.ibm.ola.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class IOUtil {
    
    public static void transfer(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024 * 1000];
        int length;
        
        while ( (length = is.read(buffer)) > -1) {
            os.write(buffer, 0, length);
        }
    }
    
    public static byte[] copy(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int length;
        byte bytes[] = new byte[1024];
        while ( (length = is.read(bytes)) > 0)
            bos.write(bytes, 0, length);
        
        return bos.toByteArray();
    }
    
    public static byte[] copy(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        byte bytes[] = copy(is);
        is.close();
        return bytes;
    }
    
    public static void deleteDirectory(File directory) {
        deleteDirectory(directory, true);
    }
    
    public static void deleteDirectory(File directory, boolean recursive) {
        if (recursive) {
            File files[] = directory.listFiles();
            for (int i=0; i<files.length; i++) {
                if (files[i].isDirectory()) deleteDirectory(files[i], true);
            }
            
            for (int i=0; i<files.length; i++) {
                if (!files[i].isDirectory()) {
                    files[i].deleteOnExit();
                    files[i].delete();
                }
            }
        }
        
        directory.deleteOnExit();
        directory.delete();
    }
    
    public static void copyToFile(byte bytes[], String file) throws IOException {
        copyToFile(bytes, new File(file));
    }
    
    public static void copyToFile(byte bytes[], File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.close();
    }
}
