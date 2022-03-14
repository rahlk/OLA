package com.ibm.ola.vo;

import java.io.Serializable;

public class DataRequest implements Serializable {

	public final long start;
	public final long end;
	public final long interval;
	
	public DataRequest(long start, long end, long interval) {
		this.start = start;
		this.end = end;
		this.interval = interval;
	}
}
