package com.ibm.ola.datastore;

import com.ibm.ola.vo.ObjectCounter;

public class Data {

	public final long ids[];
	public final ObjectCounter objectCounters[];
	
	public Data(long ids[], ObjectCounter objectCounters[]) {
		this.ids = ids;
		this.objectCounters = objectCounters;
	}
}
