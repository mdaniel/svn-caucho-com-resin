package com.caucho.test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Employee {

    private String name;
    private String phoneNumber;
    private boolean old;
    private List<AddressBook> books;
    private Set<AddressBook> books2;
    private AddressBook [] books3;

    
    
	public List<AddressBook> getBooks() {
        return books;
    }


    public void setBooks(List<AddressBook> books) {
        this.books = books;
    }


	
	public Employee(String name, String phoneNumber, List<AddressBook> books) {
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.books = books;
	}

	   
    public Employee(String name, String phoneNumber, List<AddressBook> books, Set<AddressBook> books2, AddressBook[] books3) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.books = books;
        this.books2 = books2;
        this.books3 = books3;
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
//	public void setName(String name) {
//		this.name = name;
//	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	
	public void someMethod() {
		
	}


    @Override
    public String toString() {
        return "Employee [name=" + name + ", phoneNumber=" + phoneNumber
                + ", old=" + old + ", books=" + books + ", books2=" + books2
                + ", books3=" + Arrays.toString(books3) + "]";
    }


    public Set<AddressBook> getBooks2() {
        return books2;
    }


    public void setBooks2(Set<AddressBook> books2) {
        this.books2 = books2;
    }


    public AddressBook[] getBooks3() {
        return books3;
    }


    public void setBooks3(AddressBook[] books3) {
        this.books3 = books3;
    }
	
	
}
