package com.ibm.ola.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import com.ibm.ola.classloader.ClassLoaderClassVisitor;
import com.ibm.ola.datastore.AbstractDataStore;
import com.ibm.ola.recorder.Recorder;
import com.ibm.ola.util.IOUtil;

public class HeapsterAgent implements ClassFileTransformer{

	static Configuration configuration;
	static boolean initializationFailed = false;
	
	static File SAVE_INSTRUMENTED_CLASS_DIR;
	
	static Logger logger = new Logger();
	
	public static void premain(String agentArgs, Instrumentation inst) {
		configure(agentArgs, inst);
	}
	
	public static void agentmain(String agentArgs, Instrumentation inst) {
		configure(agentArgs, inst);
	}
	
	static void configure(String agentArgs, final Instrumentation inst) {
		if (agentArgs == null || agentArgs.trim().length() == 0)
			System.out.println("Agent configuration not specified. Agent initialization aborted.");
		InputStream is = null;
		try {
			File file = new File(agentArgs);
			if (file.exists()) 
				is = new FileInputStream(file);
			else 
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream(agentArgs);
			
			if (is == null) {
				initializationFailed = true;
				System.out.println("Agent configuration not found. Agent initialization aborted.");
			}
			else {
				configuration = new Configuration();
				configuration.load(is);
				is.close();
				Logger.setLevel(configuration);
				String dataStoreImplementationClass = configuration.getDataStoreImplementationClass();
				if (dataStoreImplementationClass == null) { 
					System.out.println("No data-store defined. Aborting configuration.");
					initializationFailed = true;
					return;
				}
				else logger.info("Installing data store type: " + dataStoreImplementationClass);
				
				try {
					inst.addTransformer(new HeapsterAgent());
					
					Class dataStoreClass = Class.forName(dataStoreImplementationClass);
					Constructor<AbstractDataStore> c = dataStoreClass.getConstructor(new Class[]{Configuration.class});
					AbstractDataStore dataStore = c.newInstance(new Object[]{configuration});
					Recorder.start(configuration, dataStore);
					Logger.setLevel(configuration);
					
					logger.info("Agent configuration successful.");

					String s = configuration.getStringValue("save-instrumented-class-dir");
					if (s != null) {
					    SAVE_INSTRUMENTED_CLASS_DIR = new File(s);
					    logger.info("Save Instrumented Class Dir set to: " + SAVE_INSTRUMENTED_CLASS_DIR.getAbsolutePath());
					}
				} 
				catch (Exception e) {
					logger.error("Agent initialization error.", e);
					System.out.println("An agent initialization error occured. Aborting initialization.");
					initializationFailed = true;
				}
			}
		} 
		catch (IOException e) {
			System.out.println("Could not configure agent. Agent initialization aborted.");
			logger.error("Could not configure agent.", e);
			initializationFailed = true;
		}
		finally {
			if (is != null) {
				try { is.close(); } catch (IOException e) { /* nothing to do */ }
			}
		}
	}


	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		
		if (initializationFailed) return null;
		
		if (className.startsWith("java"))
			return null;
		else if (className.startsWith("sun"))
			return null;
		
		if (className.equals("java/lang/Enum") ||
			className.equals("sun/launcher/LauncherHelper") ||
			className.equals("java/lang/Long$LongCache") ||
			className.equals("java/util/concurrent/locks/ReentrantReadWriteLock$Sync$ThreadLocalHoldCounter") ||
			className.equals("sun/misc/Cleaner") ||
			className.equals("java/lang/Shutdown")
				) return null;
		
		try {
			String dotName = className.replace('/', '.');
			if (!isAgentClass(dotName) && configuration.isIncluded(dotName)) {
				logger.info("Instrumenting: " + dotName);

				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				HeapsterTransformer transformer = new HeapsterTransformer(writer);
				ClassReader reader = new ClassReader(classfileBuffer);
				reader.accept(transformer, ClassReader.SKIP_FRAMES);
				
				if (SAVE_INSTRUMENTED_CLASS_DIR != null) {
				    FileOutputStream fos = null;
				    try {
	                    File f = new File(SAVE_INSTRUMENTED_CLASS_DIR, className + ".class");
	                    f.getParentFile().mkdirs();
	                    fos = new FileOutputStream(f);
	                    fos.write(writer.toByteArray());
	                    logger.trace("Saved instrumented class: " + f.getAbsolutePath());
                    }
                    catch (Exception e) {
                        logger.error("Could not save instrumented file " + className, e);
                    }
				    finally {
				        if (fos != null) {
				            try { fos.close(); } 
				            catch (IOException e) { logger.error("Could not close FileOutputStream for " + className, e); }
				        }
				    }
				}
				
				return writer.toByteArray();
			}
			else {
			    logger.trace("Not instrumenting: " + dotName);
				return null;
			}
		} 
		catch (Error e) {
		    logger.error("Unexpected agent error.", e);
			throw e;
		}
		catch (RuntimeException e) {
		    logger.error("Unexpected agent exception.", e);
			throw e;
		}
	}
	
	boolean isAgentClass(String className) {
		return className.startsWith("ca.discotek.org.objectweb.asm") || className.startsWith("com.ibm.ola");
	}
}
