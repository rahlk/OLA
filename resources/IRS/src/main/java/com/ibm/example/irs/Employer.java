package com.ibm.example.irs;

import java.util.List;

public class Employer{

	private int employerId;
	String employerName;
	private List<Employee> employees;
	
	public Employer() {}
	   
	public Employer(int employerId, String employerName, List<Employee> employees){  
		
		System.out.println("In Employer, Employer()");
		//ClusterObjectManager.printTable();
		
	      this.employerId = employerId; 
	      this.employerName = employerName;
	      this.employees = employees;
	}
	
	
	public int getEmployerId() {
		return employerId;
	}

	public void setEmployerId(int employerId) {
		this.employerId = employerId;
	}

	public String getEmployerName() {
		return employerName;
	}

	public void setEmployerName(String employerName) {
		this.employerName = employerName;
	}

	public List<Employee> getEmployees() {
		return employees;
	}

	public void setEmployees(List<Employee> employees) {
		this.employees = employees;
	}	
}
