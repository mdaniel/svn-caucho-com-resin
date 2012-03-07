package com.caucho.encoder;

import com.caucho.amp.Message;

/** Encodes an JAMP input object (now just string) into a Message. */
public class JampMessageDecoder implements Decoder<Message, String>{
	enum ParseMode{messageType, to, from, methodName, done};
	

    @Override
    public Message decodeObject(String payload) throws Exception {
        ParseMode mode = ParseMode.messageType;
        char[] cs = payload.toCharArray();
        if(cs[0]!='[' || cs[1]!='"' || cs[cs.length-1]!=']') {
            throw new IllegalStateException("Not JSON JAMP format " + payload);
        }
        StringBuilder messageType = new StringBuilder();
        StringBuilder to  = new StringBuilder();
        StringBuilder from  = new StringBuilder();
        StringBuilder methodName  = new StringBuilder();

        for (int index = 2; index < cs.length; index++){
            char c = cs[index];
            if (c==',') {
                switch (mode) {
                case messageType:
                    mode=ParseMode.to;
                    break;
                case to:
                    mode=ParseMode.from;
                    break;
                case from:
                    mode=ParseMode.methodName;
                    break;
                case methodName:
                    return new Message(messageType.toString(), to.toString(), from.toString(), methodName.toString(), payload.substring(index+1, payload.length()-1));
                }
                continue;
            }
            
            if (c==' ' || c=='\t' || c=='"') continue;
            
            switch (mode) {
            case messageType:
                messageType.append(c);
                break;
            case to:
                to.append(c);
                break;
            case from:
                from.append(c);
                break;
            case methodName:
                methodName.append(c);
                break;
            }
        }
        
        throw new IllegalStateException("Unable to process JAMP message, format error");
        
    }
	
}
