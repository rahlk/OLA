package com.ibm.example.irs;

import java.util.List;

public class IRS{
	
	
	private int yearr;
	
	private List<Salary> salaryList;

	public int getYear()
	{
		return yearr;
	}
	public void setYear(int yearr)
	{
		this.yearr = yearr;
	}
	
	public List<Salary> getSalaryList() {
		return salaryList;
	}

	public void setSalaryList(List<Salary> salaryList) {
		this.salaryList = salaryList;
	}

	public Salary getSalaryfromIRS(int employeeId) {
		
		System.out.println("in IRS, getSalaryfromIRS");
		//ClusterObjectManagerIRS.printTable();
		for(Salary salary:this.salaryList)
		{
			
			System.out.println(String.valueOf(salary));
			if(salary.getEmployeeId()==employeeId)
			{
				return salary;
			}
		}
		
		return null;
	}
	
}
