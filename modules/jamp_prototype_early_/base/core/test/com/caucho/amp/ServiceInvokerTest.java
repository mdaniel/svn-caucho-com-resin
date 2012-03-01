package com.caucho.amp;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

import com.caucho.encoder.JampMethodEncoder;
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
        
        Employee emp = new Employee("rick", "510-555-1212");
        emp.setOld(true);
        
        Object encodedObject = encoder.encodeMethodForSend(method, new Object[]{emp, 1, 1.0f, 2, "hello dolly"}, "to@me", "from@someoneelse");
        return encodedObject;
    }
}
