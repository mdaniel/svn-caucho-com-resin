package com.caucho.amp;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.caucho.encoder.JampMethodEncoder;
import com.caucho.test.AddressBook;
import com.caucho.test.Employee;
import com.caucho.test.EmployeeService;
import com.caucho.test.EmployeeServiceImpl;


public class ServiceInvokerTest {

    @Test
    public void invokerTest() throws Exception {
        Object methodEncodedAsMessage = getMethodEncodedAsMessage();
        System.out.println(methodEncodedAsMessage);
        ServiceInvoker serviceInvoker = new ServiceInvoker(EmployeeServiceImpl.class);
        serviceInvoker.invokeMessage(methodEncodedAsMessage);
        
    }

    private Object getMethodEncodedAsMessage() throws NoSuchMethodException,
            Exception {
        JampMethodEncoder encoder = new JampMethodEncoder();
        Method method = EmployeeService.class.getDeclaredMethod("addEmployee", Employee.class, int.class, float.class, Integer.class, String.class);
        assertNotNull(method);
        
        List<AddressBook> books = new ArrayList<AddressBook>();
        books.add(new AddressBook("a"));
        books.add(new AddressBook("b"));
        
        Employee emp = new Employee("rick", "510-555-1212", books);
        emp.setOld(true);
        
        Object encodedObject = encoder.encodeMethodForSend(method, new Object[]{emp, 1, 1.0f, 2, "hello dolly"}, "to@me", "from@someoneelse");
        return encodedObject;
    }
}
