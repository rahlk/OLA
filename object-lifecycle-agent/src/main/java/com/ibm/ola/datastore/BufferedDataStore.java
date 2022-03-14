package com.ibm.ola.datastore;

import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import com.ibm.ola.agent.Configuration;
import com.ibm.ola.agent.NameNotFoundException;

public class BufferedDataStore extends AbstractDataStore {

	boolean alive = true;
	
	LinkedBlockingDeque<NewObjectEvent> newObjectDeque = new LinkedBlockingDeque<NewObjectEvent>();
	LinkedBlockingDeque<GcObjectEvent> gcObjectDeque = new LinkedBlockingDeque<GcObjectEvent>();
	
	final AbstractDataStore internalStore;
	
	NewObjectProcessingThread newObjectProcessingThread;
	GcObjectProcessingThread gcObjectProcessingThread;
	
	public BufferedDataStore(Configuration configuration, AbstractDataStore internalStore) {
		super(configuration);
		this.internalStore = internalStore;
		
		newObjectProcessingThread = new NewObjectProcessingThread();
		newObjectProcessingThread.start();
		gcObjectProcessingThread = new GcObjectProcessingThread();
		gcObjectProcessingThread.start();
	}
	
	public boolean isErrorState() {
		return internalStore.isErrorState();
	}

	
	public Data getData(long start, long end, long interval) {
		return internalStore.getData(start, end, interval);
	}

	public Long getUid(String className) throws NameNotFoundException {
		return internalStore.getUid(className);
	}

	public String getClassName(Long uid) {
		return internalStore.getClassName(uid);
	}

	public long getDataStart() {
		return internalStore.getDataStart();
	}

	public long getDataEnd() {
		return internalStore.getDataEnd();
	}
	
	public void newObjectEvent(long classId, long time) {
		newObjectDeque.add(new NewObjectEvent(classId, time));
	}

	public long newObjectEvent(Object o, long time) throws NameNotFoundException {
        long id = getId(o);
		newObjectEvent(id, time);
		return id;
	}

	public void objectGcEvent(long classId, long time) {
		gcObjectDeque.add(new GcObjectEvent(classId, time));
	}

	
	class NewObjectEvent {
		public final long classId;
		public final long time;
		
		public NewObjectEvent(long classId, long time) {
			this.classId = classId;
			this.time = time;
		}
	}
	
	class GcObjectEvent {
		public final long classId;
		public final long time;
		
		public GcObjectEvent(long classId, long time) {
			this.classId = classId;
			this.time = time;
		}
	}
	
	class NewObjectProcessingThread extends Thread {
		
		public NewObjectProcessingThread() {
			setDaemon(true);
		}
		
		public void run() {
			while (alive) {
				try {
					NewObjectEvent e = newObjectDeque.take();
					internalStore.newObjectEvent(e.classId, e.time);
				} 
				catch (Exception e) {
					alive = false;
				}

			}
		}
	}
	
	class GcObjectProcessingThread extends Thread {
		
		public GcObjectProcessingThread() {
			setDaemon(true);
		}
		
		public void run() {
			while (alive) {
				try {
					GcObjectEvent e = gcObjectDeque.take();
					internalStore.objectGcEvent(e.classId, e.time);
				} 
				catch (Exception e) {
					alive = false;
				}

			}
		}
	}
}
