package com.ibm.ola.vo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ObjectCounter implements Serializable {

	public final long start;
	public final long end;
	
	Map<Long, Long> map = new HashMap<Long, Long>();
	
	public ObjectCounter(long start, long end) {
		this.start = start;
		this.end = end;
	}
		
	public void updateCount(Long id, boolean add) {
		Long count = map.get(id);
		if (count == null)  count = 0l;
		count = new Long(new Long(count + (add ? 1 : -1))); 
		map.put(id, count);
	}
	
	public long getCount(long id) {
	    Long count = map.get(id);
	    return count == null ? 0 : count;
	}
	
	public Long[] getIds() {
		return map.keySet().toArray(new Long[map.size()]);
	}
	
	public long getTotalObjectCount() {
		Iterator<Long> it = map.values().iterator();

		long total = 0;
		while (it.hasNext()) {
			total += it.next();

		}
		return total;
	}
	
	public ObjectCounter cloneForNextInterval(long start, long end) {
	    ObjectCounter clone = new ObjectCounter(start, end);
	    Set<Entry<Long, Long>> set = map.entrySet();
	    Iterator<Entry<Long, Long>> it = set.iterator();
	    Entry<Long, Long> entry;
	    while (it.hasNext()) {
	        entry = it.next();
            clone.map.put(entry.getKey(), entry.getValue());
	    }
	    return clone;
	}
}