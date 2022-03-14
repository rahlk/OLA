package com.ibm.ola.vo;

import java.io.Serializable;

public class DataPeriodResponse implements Serializable {

	public final long start;
	public final long end;
	
	public DataPeriodResponse(long start, long end) {
		this.start = start;
		this.end = end;
	}
}
