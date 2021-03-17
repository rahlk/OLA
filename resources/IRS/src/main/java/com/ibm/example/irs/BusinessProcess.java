package com.ibm.example.irs;

import java.io.File;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

//import org.apache.commons.lang.RandomStringUtils;


public class BusinessProcess{
	
	
	
	//initiate databases
	public List<Employer> getAllEmployers()
	{
		List<Employer> employerList = new ArrayList<Employer>();
		System.out.println("initialize employee list: ");
		//ClusterObjectManagerEmployer.printTable();
		Employee employee1 = new Employee(11, "Alice", 40, 32);
		System.out.println("1st employee created: ");
		System.out.println("1st employee rate: "+String.valueOf(employee1.getRate()));
		//ClusterObjectManagerEmployer.printTable();
		Employee employee2 = new Employee(22, "Bob", 40, 45);
		System.out.println("2nd employee created: ");
		System.out.println("2nd employee rate: "+String.valueOf(employee2.getRate()));
		System.out.println("Employee static level is: "+Employee.getLevel());
		//ClusterObjectManagerEmployer.printTable();
		
		List<Employee> employeeList = new ArrayList<Employee>();
		employeeList.add(employee1);
		employeeList.add(employee2);
		
		Employer employer = new Employer(1, "ACME", employeeList);
		
		Employee.setLevel("ABC");
		
		Employee employee3 = new Employee(45, "Charles", 40, 47);
		System.out.println("3rd employee rate: "+String.valueOf(employee3.getRate()));
		Employee employee4 = new Employee(25, "Dana", 40, 50);
		System.out.println("4th employee rate: "+String.valueOf(employee4.getRate()));
		System.out.println("Employee static level is: "+Employee.getLevel());
		
		List<Employee> employeeList1 = new ArrayList<Employee>();
		employeeList1.add(employee3);
		employeeList1.add(employee4);
		
		Employer employer1 = new Employer(2, "EMCA", employeeList);
		
		employerList.add(employer);
		employerList.add(employer1);
		
		
		return employerList;
		
	}
	
	
	public void genSalarySlip(IRS irsInst) {
		
		List<Employer> employerList = getAllEmployers();
		System.out.println("after getAllEmployers(): "+irsInst.getYear());
		List<Salary> salaryList = irsInst.getSalaryList();
		System.out.println("after getSalaryList(): "+irsInst.getYear());
		
		for(Employer employer: employerList){
			int employerId = employer.getEmployerId();
			List<Employee> employeeList = employer.getEmployees();
			System.out.println("in employer loop, getEmployees(): "+irsInst.getYear());
			for(Employee employee:employeeList) {
				int employeeId = employee.getEmployeeId();
				Salary instSalary = new Salary();
				System.out.println("in employee list loop: "+irsInst.getYear());
				instSalary.setEmployerId(employerId);
				instSalary.setEmployeeId(employeeId);
				instSalary.setSalary(employee.getRate()*employee.getHours());
				salaryList.add(instSalary);
			}			
		}
		
		System.out.println("before setSalaryList: "+irsInst.getYear());
		irsInst.setSalaryList(salaryList);
		System.out.println("after setSalaryList: "+irsInst.getYear());
		
	}
	
	public void testMain(){
		//BusinessProcess instBP = new BusinessProcess();
		//
		test_2();
	}
	
	
	public static void main(String[] args) throws IOException, NamingException, InterruptedException{
		BusinessProcess instBP = new BusinessProcess();
		
//		do {
//			TimeUnit.SECONDS.sleep(10);
//		}
//		while(ClusterObjectManagerEmployer.context == null);
		
		instBP.test_2();
		
	}
	
	private void test_1(){
		IRS irsInst = new IRS();
		irsInst.setYear(100);
		irsInst.setSalaryList(new ArrayList<Salary>());
		irsInst.getSalaryList();
	}
	
	
	
	private void test_2(){
		
		
		
		IRS irsInst = new IRS();
		irsInst.setYear(100);
		irsInst.setSalaryList(new ArrayList<Salary>());
		System.out.println("1: "+irsInst.getYear());
		this.genSalarySlip(irsInst);
		
		
		//ClusterObjectManagerEmployer.printTable();
		System.out.println("2: "+irsInst.getYear());
		List<Salary> salaryList = irsInst.getSalaryList();
		System.out.println("3: "+irsInst.getYear());
		System.out.println("In BuisnessProcess, test_2");
		//ClusterObjectManagerEmployer.printTable();
		for(Salary salary:salaryList) {
			System.out.println(String.valueOf(salary.getEmployerId())+","+String.valueOf(salary.getEmployeeId())+","+String.valueOf(salary.getSalary()));
		}
		
		Employee employeeKate = new Employee(33, "Kate", 40, 32);
		salaryList = irsInst.getSalaryList();
		Salary salaryKate = new Salary();
		salaryKate.setEmployerId(1);
		salaryKate.setEmployeeId(employeeKate.getEmployeeId());
		salaryKate.setSalary(employeeKate.getRate()*employeeKate.getHours());
		salaryList.add(salaryKate);
		irsInst.setSalaryList(salaryList);
		System.out.println("In BuisnessProcess, test_2");
		for(Salary salary:salaryList) {
			System.out.println(String.valueOf(salary.getEmployerId())+","+String.valueOf(salary.getEmployeeId())+","+String.valueOf(salary.getSalary()));
		}
		
		//employeeAlice.getEmployeeId();
		salaryKate = irsInst.getSalaryfromIRS(employeeKate.getEmployeeId());
		System.out.println("Salary for "+employeeKate.getName()+": "+String.valueOf(salaryKate.getSalary()));
//		
		
		
	}
	
}
