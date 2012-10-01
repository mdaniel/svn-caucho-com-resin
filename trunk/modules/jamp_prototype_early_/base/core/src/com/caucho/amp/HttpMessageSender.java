package com.caucho.amp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpMessageSender implements AmpMessageSender {
    
    private String _url;

    public HttpMessageSender (String url) {
        this._url = url;
    }

    @Override
    public void sendMessage(String name, Object oPayload, String toInvoker,
            String fromInvoker) throws Exception {
        
        
        URL url = new URL(_url);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        
        DataOutputStream outStream = null;
        
        try {
        
            if (oPayload instanceof String) {
                String payload = (String) oPayload;
                connection.setRequestProperty("Content-Type", 
                "application/json");
        
                connection.setRequestProperty("Content-Length", "" + 
                         Integer.toString(payload.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");                
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
            
                outStream = new DataOutputStream (
                        connection.getOutputStream ());
    
                outStream.writeBytes(payload);
    
            
            } else if (oPayload instanceof byte[]) {
                byte[] payload = (byte[]) oPayload;
                connection.setRequestProperty("Content-Type", 
                "application/hessian");
        
                connection.setRequestProperty("Content-Length", "" + 
                         Integer.toString(payload.length));
                connection.setRequestProperty("Content-Language", "en-US");                
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                
                outStream = new DataOutputStream (
                        connection.getOutputStream ());
    
                outStream.write(payload);
                
            }
        
        } finally {
            //Send request
            outStream.flush ();
            outStream.close ();
        }
        //Get Response    
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer(); 
        while((line = rd.readLine()) != null) {
          response.append(line);
          response.append('\r');
        }
        rd.close();
        System.out.println(response);
        
    }

}
