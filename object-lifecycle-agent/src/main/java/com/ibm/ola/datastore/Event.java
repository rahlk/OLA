package com.ibm.ola.datastore;

import java.io.Serializable;

public class Event implements Serializable {
	
	
	public static final boolean TYPE_NEW = true;
	public static final boolean TYPE_GC = false;

	public final long id;
	public final long time;
	public final boolean type;
	
	public Event(long id, long time, boolean type) {
		this.id = id;
		this.time = time;
		this.type = type;
	}
}
