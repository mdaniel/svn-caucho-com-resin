package com.caucho.encoder;

import java.io.BufferedReader;

import com.caucho.amp.Message;



/** Encodes an JAMP input object (bufferedreader) into a Message. */
public class JampMessageReaderDecoder implements Decoder <Message, BufferedReader> {
	
    private static String readPayload(BufferedReader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        
        
        try {
            String line = null;
            
            while ((line=reader.readLine())!=null) {
                builder.append(line);
            }
        }finally{
            if (reader!=null)reader.close();
        }
        
        return builder.toString();
    }


    @Override
    public Message decodeObject(BufferedReader reader) throws Exception {
        JampMessageDecoder decoder = new JampMessageDecoder();

        String str = readPayload(reader);
        
        return decoder.decodeObject(str);
    }
	
}
