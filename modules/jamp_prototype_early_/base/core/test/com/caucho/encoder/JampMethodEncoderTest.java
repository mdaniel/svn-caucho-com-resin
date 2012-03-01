package com.caucho.encoder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import com.caucho.test.Employee;
import com.caucho.test.EmployeeService;


public class JampMethodEncoderTest {
    
    @Test
    public void simpleMethodEncoderTest() throws Exception {
        JampMethodEncoder encoder = new JampMethodEncoder();
        Method method = EmployeeService.class.getDeclaredMethod("addEmployee", Employee.class, int.class, float.class, Integer.class, String.class);
        assertNotNull(method);
        
        Employee emp = new Employee("rick", "510-555-1212");
        
        Object encodedObject = encoder.encodeMethodForSend(method, new Object[]{emp, 1, 1.0f, 2, "hello dolly"}, "to@me", "from@someoneelse");

        assertNotNull(encodedObject);
        
        System.out.println(encodedObject);
        
        assertTrue(encodedObject.equals("[\"send\",\"to@me\",\"from@someoneelse\",\"addEmployee\"," +
        		"[{\"java_type\":\"com.caucho.test.Employee\",\"name\":\"rick\",\"old\":false," +
        				"\"phoneNumber\":\"510-555-1212\"},1,1.0,2,\"hello dolly\"]]"));
    }

}
