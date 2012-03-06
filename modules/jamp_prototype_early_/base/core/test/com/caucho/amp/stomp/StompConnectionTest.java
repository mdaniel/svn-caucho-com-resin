package com.caucho.amp.stomp;

import java.io.IOException;

import org.junit.Test;


public class StompConnectionTest {

    @Test
    public void testProtocolOnlyStomp () throws IOException {
        StompConnection connection = new StompConnection();
        connection.connect("stomp://localhost:6666/foo");
        connection.close();
    }
    
    @Test(timeout=15000)    
    public void testSend () throws IOException {
        StompConnection connection = new StompConnection();
        connection.connect("stomp://localhost:6666/foo");
        
        connection.send("queue/bob", "love_rocket");
        
        connection.subscribe("queue/bob", new MessageListener() {
            
            @Override
            public void onTextMessage(String text) {
                
                
            }
            
            @Override
            public void onBinaryMessage(String text) {
                 
            }
        });
        
        for (int index=0; index < 100; index++) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            connection.send("queue/bob", "love_rocket");
        }

    }
    
}
