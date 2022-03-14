package com.ibm.ola.vo;

import java.io.Serializable;

import com.ibm.ola.datastore.Event;

public class DataResponse implements Serializable {

	public final long ids[];
	public final ObjectCounter counters[];
	
	public DataResponse(long ids[], ObjectCounter counters[]) {
		this.ids = ids;
		this.counters = counters;
	}
}
