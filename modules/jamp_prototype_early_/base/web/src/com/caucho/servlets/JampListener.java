package com.caucho.servlets;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;

import com.caucho.amp.Message;
import com.caucho.amp.ServiceInvoker;
import com.caucho.encoder.JampMessageReaderDecoder;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketListener;

public class JampListener implements WebSocketListener {
    
    private ServiceInvoker serviceInvoker;
    
    public JampListener() {
        serviceInvoker = new ServiceInvoker(EmployeeServiceImpl.class);
        serviceInvoker.setMessageDecoder(new JampMessageReaderDecoder());
    }
    

    @Override
    public void onReadText(WebSocketContext context, Reader aReader)
            throws IOException {

        PrintWriter out = context.startTextMessage();
        
        out.println("emp data...");

        try {
            System.out.println("ON READ TEXT ***********************************************");
            Message message = serviceInvoker.invokeMessage(new BufferedReader(aReader));

            System.out.println("SERVICE INVOKDER WAS INVOKED ********************** \n" + message);
            out.println(message.toString());
            
            out.flush();
            out.close();
        
        } catch (Exception ex) {
            out.println("PROBLEM " + ex.getMessage());
            ex.printStackTrace(out);
            ex.printStackTrace();
        }


    }


    @Override
    public void onClose(WebSocketContext arg0) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnect(WebSocketContext arg0) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReadBinary(WebSocketContext arg0, InputStream arg1)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStart(WebSocketContext arg0) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTimeout(WebSocketContext arg0) throws IOException {
        // TODO Auto-generated method stub

    }

}
