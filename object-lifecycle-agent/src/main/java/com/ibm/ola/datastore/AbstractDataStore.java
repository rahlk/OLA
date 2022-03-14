package com.ibm.ola.datastore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ola.agent.Configuration;
import com.ibm.ola.agent.NameNotFoundException;
import com.ibm.ola.vo.ObjectCounter;

public abstract class AbstractDataStore {

	Map<Long, Long> objectCountMap = new HashMap<Long, Long>(); 
	
	Map<Long, String> uidClassNameMap = new HashMap<Long, String>();
	Map<String, Long> classNameUidMap = new HashMap<String, Long>();
	
	final Configuration configuration;
	
	public AbstractDataStore(Configuration configuration) {
		this.configuration = configuration;
	}
	
	protected Map<String, String> typeNameMap = new HashMap<String, String>();

	
	protected long getId(Object o) throws NameNotFoundException {
		boolean isClass = o instanceof Class;
		Class type = isClass ? (Class) o : o.getClass();
		String typeName = isClass ? type.getName() + " [class]" : type.getName();
		String typePrettyName = typeNameMap.get(typeName);
		if (typePrettyName == null) {
			typePrettyName = isClass ? toPrettyName(type) + " [class]" : toPrettyName(type);
			typeNameMap.put(typeName, typePrettyName);
		}
		
		Long id = getCachedUid(typePrettyName);
		if (id == null) {
			id = getUid(typePrettyName);
			if (id > -1)
			    updateCaches(id, typePrettyName);
		}

		return id;
	}
	
	protected static String toPrettyName(Class type) {
		StringBuffer buffer = new StringBuffer();
		Class compType = type;
        while (compType.isArray()) {
        	buffer.append("[]");
        	compType = compType.getComponentType();
        }
        buffer.insert(0, compType.getName());
		
		return buffer.toString();
	}
	
	public Long getCachedUid(String className) {
		return classNameUidMap.get(className);
	}
	
	public String getCachedClassName(Long uid) {
		return uidClassNameMap.get(uid);
	}
	
	public void updateCaches(Long uid, String className) {
		uidClassNameMap.put(uid,  className);
		classNameUidMap.put(className, uid);
	}
	
	public void updateObjectCountMap(long classId, boolean add) {
		Long count = objectCountMap.get(classId);
		if (count == null) count = 0l; 
		else count = add ? count + 1 : count - 1;
		objectCountMap.put(classId, count);
	}
	
	public Long getObjectCount(Long classId) {
		return objectCountMap.get(classId);
	}
	
	public abstract Data getData(long start, long end, long interval);
	
    public Data getData(Event events[], long start, long end, long interval) {
        ObjectCounter counters[] = interval == 0 ? new ObjectCounter[0] : new ObjectCounter[1 + (int) ((end - start) / interval) ];
        
        Set<Long> idSet = new HashSet<Long>();
        
        ObjectCounter previousCounter = null;
        for (int i=0; i<events.length; i++) {
            if (events[i].time < start) 
                continue;
            else if (events[i].time > end) 
                break;
            else {
                idSet.add(events[i].id);
                int index = (int) ((events[i].time - start) / interval);
                if (counters[index] == null) {
                    long intervalStart = start + index * interval;
                    long intervalEnd = intervalStart + interval - 1;
                    counters[index] = previousCounter == null ? 
                        new ObjectCounter(intervalStart, intervalEnd) : previousCounter.cloneForNextInterval(intervalStart, intervalEnd);
                    previousCounter = counters[index];
                }

                counters[index].updateCount(events[i].id, events[i].type);
            }
        }

        Long idObjects[] = idSet.toArray(new Long[idSet.size()]);
        long ids[] = new long[idSet.size()];
        for (int i=0; i<ids.length; i++)
            ids[i] = idObjects[i];
        return new Data(ids, counters);
    }
    
	public abstract boolean isErrorState();
	
	public abstract Long getUid(String className) throws NameNotFoundException;
	
	public abstract String getClassName(Long uid);
	
	public abstract long getDataStart();
	public abstract long getDataEnd();
	
	
	/* these implementations should be asynchronous */
	public abstract void newObjectEvent(long classId, long time);
	public abstract void objectGcEvent(long classId, long time);
}
