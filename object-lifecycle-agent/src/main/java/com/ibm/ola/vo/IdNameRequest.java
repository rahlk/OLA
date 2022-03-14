package com.ibm.ola.vo;

import java.io.Serializable;

public class IdNameRequest implements Serializable {

	public final long ids[];
	
	public IdNameRequest(long ids[]) {
		this.ids = ids;
	}
}
