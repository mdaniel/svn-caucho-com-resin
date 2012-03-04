package com.caucho.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.caucho.test.AddressBook;
import com.caucho.test.Employee;
import com.caucho.test.EmployeeService;


public class JampMethodEncoderTest {
    
    @Test
    public void simpleMethodEncoderTest() throws Exception {
        JampMethodEncoder encoder = new JampMethodEncoder();
        Method method = EmployeeService.class.getDeclaredMethod("addEmployee", Employee.class, int.class, float.class, Integer.class, String.class);
        assertNotNull(method);
        
        
        List<AddressBook> books = new ArrayList<AddressBook>();
        books.add(new AddressBook("a"));
        books.add(new AddressBook("b"));

        Employee emp = new Employee("rick", "510-555-1212", books);
        
        Object encodedObject = encoder.encodeMethodForSend(method, new Object[]{emp, 1, 1.0f, 2, "hello dolly"}, "to@me", "from@someoneelse");

        assertNotNull(encodedObject);
        
        System.out.println(encodedObject);
        
        assertEquals("[\"send\",\"to@me\",\"from@someoneelse\",\"addEmployee\",[{\"java_type\":\"com.caucho.test.Employee\"," +
        		"\"name\":\"rick\",\"books\":[{\"java_type\":\"com.caucho.test.AddressBook\",\"foo\":\"a\"},{\"java_type\":" +
        		"\"com.caucho.test.AddressBook\",\"foo\":\"b\"}],\"old\":false,\"phoneNumber\":\"510-555-1212\",\"books2\":null,\"books3\":null},1,1.0,2,\"hello dolly\"]]", encodedObject);
    }

}
