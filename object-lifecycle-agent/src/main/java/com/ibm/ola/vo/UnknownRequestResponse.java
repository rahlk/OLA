package com.ibm.ola.vo;

import java.io.Serializable;

public class UnknownRequestResponse implements Serializable {

	public final Serializable request;
	
	public UnknownRequestResponse(Serializable request) {
		this.request = request;
	}
}
