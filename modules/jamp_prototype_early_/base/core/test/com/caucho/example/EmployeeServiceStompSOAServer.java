package com.caucho.example;

import java.io.IOException;

import com.caucho.amp.StompMessageReceiver;
import com.caucho.test.EmployeeServiceImpl;

public class EmployeeServiceStompSOAServer {
    public static void main (String [] args) throws IOException {
        new StompMessageReceiver("stomp://localhost:6666/foo", 
                "rick", "rick", "queue/empService", EmployeeServiceImpl.class, null);
    }
}
