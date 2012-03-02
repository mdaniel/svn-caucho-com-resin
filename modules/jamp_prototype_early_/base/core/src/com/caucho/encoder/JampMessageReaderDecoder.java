package com.caucho.encoder;

import java.io.BufferedReader;



/** Encodes an JAMP input object (bufferedreader) into a Message. */
public class JampMessageReaderDecoder implements Decoder {
	
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
    public Object decodeObject(Object objPayload) throws Exception {
        BufferedReader reader = (BufferedReader) objPayload;
        JampMessageDecoder decoder = new JampMessageDecoder();

        String str = readPayload(reader);
        
        return decoder.decodeObject(str);
    }
	
}
