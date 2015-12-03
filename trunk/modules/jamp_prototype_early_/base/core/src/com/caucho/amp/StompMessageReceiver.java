package com.caucho.amp;

import java.io.IOException;

import com.caucho.amp.stomp.MessageListener;
import com.caucho.amp.stomp.StompConnection;

public class StompMessageReceiver {
    
    StompConnection connection;
    String destination;
    private SkeletonServiceInvoker invoker;
    
    public StompMessageReceiver(String connectionString, String login, String passcode, String destination, Class<?> serviceClass, Object instance) throws IOException {
        connection = new StompConnection();
        connection.connect(connectionString, login, passcode);
        this.destination = destination;
        
        
        connection.subscribe(destination, new MessageListener() {
            
            @Override
            public void onTextMessage(String text) throws Exception {
                    handleMessage(text);
            }
            
            @Override
            public void onBinaryMessage(String text)  throws Exception {
                
            }
        });
        
        if (serviceClass!=null) {
            invoker = AmpFactory.factory().createJampServerSkeleton(serviceClass);
        } else {
            invoker = AmpFactory.factory().createJampServerSkeleton(instance);
            
        }
        
    }
    
    void handleMessage (String text) throws Exception{
        System.out.println("HANDLE MESSAGE " + text);
        invoker.invokeMessage(text);
    }


}
