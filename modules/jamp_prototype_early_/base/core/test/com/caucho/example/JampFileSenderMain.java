package com.caucho.example;

import java.util.ArrayList;
import java.util.List;

import com.caucho.amp.AmpProxyCreator;
import com.caucho.amp.FileMessageSender;
import com.caucho.encoder.JampMethodEncoder;
import com.caucho.test.AddressBook;
import com.caucho.test.Employee;
import com.caucho.test.EmployeeService;

public class JampFileSenderMain {
	

	public static void main (String [] args) throws Exception {
		
		AmpProxyCreator ampProxy = new AmpProxyCreator(new JampMethodEncoder(), new FileMessageSender("/Users/rick/test/file_invoker") );
        EmployeeService service = (EmployeeService) ampProxy.createProxy(EmployeeService.class, "to", "from");
		
        List<AddressBook> books = new ArrayList<AddressBook>();
        books.add(new AddressBook("a"));
        books.add(new AddressBook("b"));
        
		service.addEmployee(new Employee("Rick Hightower", "5205551212", books), 7, 9.99f, 8, "love's love but no love says");
        service.addEmployee(new Employee("Rick1", "5205551212", books), 7, 9.99f, 8, "rocket");
        service.addEmployee(new Employee("Rick2", "5205551213", books), 7, 9.99f, 8, "socket");
        service.addEmployee(new Employee("Rick3", "5205551214", books), 7, 9.99f, 8, "nine");
        service.addEmployee(new Employee("Rick4", "5205551215", books), 7, 9.99f, 8, "ten");
        service.addEmployee(new Employee("Rick5", "5205551216", books), 7, 9.99f, 8, "eleven");
        service.addEmployee(new Employee("Rick6", "5205551217", books), 7, 9.99f, 8, "twelve");

	}
}
