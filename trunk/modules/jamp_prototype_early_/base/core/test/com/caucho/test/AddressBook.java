package com.caucho.test;

public class AddressBook {
    
    private String foo;

    public AddressBook() {
        
    }
    
    @Override
    public String toString() {
        return "AddressBook [foo=" + foo + "]";
    }

    public AddressBook(String foo) {
        this.foo = foo;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

}
