package com.caucho.test;

import java.util.List;

public class Employee {

    private String name;
    private String phoneNumber;
    private boolean old;
    private List<AddressBook> books;

    
    
	public List<AddressBook> getBooks() {
        return books;
    }


    public void setBooks(List<AddressBook> books) {
        this.books = books;
    }


    @Override
    public String toString() {
        return "Employee [name=" + name + ", phoneNumber=" + phoneNumber
                + ", old=" + old + " ,\nbooks=" + books+ "\n]";
    }

	
	public Employee(String name, String phoneNumber, List<AddressBook> books) {
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.books = books;
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
