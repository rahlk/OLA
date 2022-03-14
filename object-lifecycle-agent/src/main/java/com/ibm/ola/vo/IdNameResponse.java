package com.ibm.ola.vo;

import java.io.Serializable;

import com.ibm.ola.datastore.Event;

public class IdNameResponse implements Serializable {

	public final String names[];
	
	public IdNameResponse(String names[]) {
		this.names = names;
	}
}
