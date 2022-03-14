package com.ibm.ola.recorder;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import com.ibm.ola.agent.Configuration;
import com.ibm.ola.agent.Logger;
import com.ibm.ola.agent.NameNotFoundException;
import com.ibm.ola.datastore.AbstractDataStore;
import com.ibm.ola.datastore.BufferedDataStore;

public class Recorder {

	static BufferedDataStore dataStore = null;
	
	static Logger logger = new Logger();
	
	public static void start(Configuration configuration, AbstractDataStore dataStore) {
		Recorder.dataStore = new BufferedDataStore( configuration, dataStore );
		(new RemoverThread()).start();
	}
	
	
	static Map<PhantomReference<Object>, Long> map = Collections.synchronizedMap( new IdentityHashMap<PhantomReference<Object>, Long>() );

	@SuppressWarnings({ "unchecked" })
	static ReferenceQueue<? super Object> queue = (ReferenceQueue<? super Object>) new ReferenceQueue();
	
	static boolean alive = true;

	public static void record(Object o) {
		if (dataStore != null) {
			try {
				if (!dataStore.isErrorState()) {
					long classId = dataStore.newObjectEvent(o, System.currentTimeMillis());
					PhantomReference<Object> ref = new PhantomReference<Object>(o, (ReferenceQueue<? super Object>) queue);
					map.put(ref, classId);
				}
			} 
			catch (NameNotFoundException e) {
				logger.error("Dropping data: " + o.getClass().getName(), e);
			}
		}
	}
	
	public static void stop() {
		alive = false;
	}
	
	static class RemoverThread extends Thread {
		
		public RemoverThread() {
			setDaemon(true);
		}
		
		public void run() {
			while (alive) {
				try {
					PhantomReference<?> ref = (PhantomReference<?>) queue.remove();
					Long classId = map.remove(ref);
					
					if (dataStore != null && !dataStore.isErrorState())
					    dataStore.objectGcEvent(classId, System.currentTimeMillis());
				} 
				catch (Exception e) {
					logger.error("Error in GC event loop.", e);
				}
			}
		}
	}
}
