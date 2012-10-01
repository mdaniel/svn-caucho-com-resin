package com.caucho.stomp;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class StompServerTest {
    
  
    Socket socket;
    
    PrintWriter out;

    BufferedReader in;
    
    @Before
    public void before() throws IOException {
        socket = new Socket("localhost", 6666);
        out = new PrintWriter(socket.getOutputStream(), 
                true);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

    }
    
    @After
    public void after() throws IOException {
        if (socket != null) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    void sendCommand(String command, String body, String... headers) throws IOException {
        out.printf("%s\n", command);
        for (String header : headers) {
            out.printf("%s\n",header);            
        }
        out.println();
        if (body!=null) {
            out.print(body);
        }
        out.printf("\u0000\n");
        
    }

    private String readResult() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (char ch = (char) in.read(); ch != 0; ch=(char)in.read()){
            builder.append(ch);
        }
        
        String result = builder.toString();
        return result;
    }

    @Test(timeout=1000)
    public void connectTestHappy() throws IOException {

        sendCommand("CONNECT", null, "host:localhost", "accept-version:1.1", "login:rick", "passcode:rick");
        
        String result = readResult();
        
        
        assertEquals(join("CONNECTED", "version:1.1", "server:StompTester/0.01 *not for production use, just for testing", "", ""), result);
        
        drainMessageTerminationChar();
    }

    private void drainMessageTerminationChar() throws IOException {
        char c = (char) in.read();
        assertEquals('\n', c);
    }

    String join(String... args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg);
            builder.append('\n');
        }
        builder.deleteCharAt(builder.length()-1);
        
        return builder.toString();
    }
    
    @Test(timeout=1000)
    public void askForAWeirdVersion() throws IOException {

        
        sendCommand("CONNECT", null, "host:localhost", "accept-version:2.7", "login:rick", "passcode:rick");
        
        String result = readResult();
        
        
        assertEquals(join("ERROR", "version:1.0,1.1", "content-type:text/plain", "", "Supported Protocols are 1.0 1.1"),
                result);
        
        drainMessageTerminationChar();
    }

    @Test(timeout=1000)
    public void connectBadUser() throws IOException {

        
        sendCommand("CONNECT", null, "host:localhost", "accept-version:2.7", "login:rick", "passcode:rick");
        
        String result = readResult();
        
        
        assertTrue(!("CONNECTED\nversion:1.1\n".equals(result)));
        
        drainMessageTerminationChar();
    }

    
    @Test(timeout=1000)
    public void connectAndSend() throws IOException {

        sendCommand("CONNECT", null, "host:localhost", "accept-version:1.0,1.1", "login:rick", "passcode:rick");
        
        
        String result = readResult();
        
        
        assertEquals(join("CONNECTED", "version:1.1", "server:StompTester/0.01 *not for production use, just for testing", "", ""), result);
        
        drainMessageTerminationChar();
        
        String message = "This is the message";
        sendCommand("SEND", message, "destination:queue/foo", "content-type:text/plain", String.format("content-length:%d", message.length()));

        
        sendCommand("SUBSCRIBE", null, "destination:queue/foo", "id:foo");
 
        result = readResult();        
        
        assertEquals(join("MESSAGE",  "", "This is the message"), result);
        
        drainMessageTerminationChar();
        
        sendCommand("UNSUBSCRIBE", null, "id:foo");

        sendCommand("DISCONNECT", null, "receipt:77");

        result = readResult();        
        
        assertEquals(join("RECEIPT",  "receipt-id:77", "", ""), result);
        
        drainMessageTerminationChar();
    
    }

}
