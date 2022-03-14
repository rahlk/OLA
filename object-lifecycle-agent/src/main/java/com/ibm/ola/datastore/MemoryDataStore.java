package com.ibm.ola.datastore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ola.agent.Configuration;
import com.ibm.ola.agent.Logger;
import com.ibm.ola.server.Server;
import com.ibm.ola.vo.ObjectCounter;

public class MemoryDataStore extends AbstractDataStore {
	
	List<Event> eventList = Collections.synchronizedList(new ArrayList<Event>());
	
	long uidCounter = 0;
	
	Logger logger = new Logger();
	
	public MemoryDataStore(Configuration configuration) {
		super(configuration);
		try {
			Server server = new Server(configuration, this);
			server.start();
		} 
		catch (Exception e) {
			throw new Error(e);
		}
	}
	
	public boolean isErrorState() {
		return false;
	}

	public Long getUid(String className) {
		Long id = getCachedUid(className);
		if (id == null) {
			id = uidCounter++;
			updateCaches(id, className);
		}
		
		return id;
	}

	public String getClassName(Long uid) {
		return getCachedClassName(uid);
	}

	public long getDataStart() {
	    logger.trace("MemoryDataStore.getDataStart: " + eventList.size() + ": " + eventList.get(0).time);
		if (eventList.size() == 0) return -1;
		else return eventList.get(0).time;
	}
	
	public long getDataEnd() {
	    logger.trace("MemoryDataStore.getDataEnd: " + eventList.size() + ": " + eventList.get(eventList.size()-1).time);
		int size = eventList.size();
		if (size == 0) return -1;
		else return eventList.get(size - 1).time;
	}

	public void newObjectEvent(long classId, long time) {
	    logger.trace("New Object Event: " + getClassName(classId));
		eventList.add(new Event(classId, time, true));
		updateObjectCountMap(classId, true);
	}

	public void objectGcEvent(long classId, long time) {
	    logger.trace("GC Object Event: " + getClassName(classId));
		eventList.add(new Event(classId, time, false));
		updateObjectCountMap(classId, false);
	}
	
	public Data getData(long start, long end, long interval) {
		return getData(eventList.toArray(new Event[eventList.size()]), start, end, interval);
	}
	
}
