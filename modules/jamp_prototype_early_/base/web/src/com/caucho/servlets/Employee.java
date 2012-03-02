package com.caucho.servlets;

public class Employee {
    
	@Override
    public String toString() {
        return "Employee [name=" + name + ", phoneNumber=" + phoneNumber
                + ", old=" + old + "]";
    }

    private String name;
	private String phoneNumber;
	private boolean old;
	
	public Employee(String name, String phoneNumber) {
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	public Employee() {
		
	}

	public boolean isOld() {
		return old;
	}

	public void setOld(boolean isOld) {
		this.old = isOld;
	}

	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public void someMethod() {
		
	}
}
