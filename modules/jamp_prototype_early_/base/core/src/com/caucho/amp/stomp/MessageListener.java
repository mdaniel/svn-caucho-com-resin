package com.caucho.amp.stomp;

interface  MessageListener {
    
    void onTextMessage(String text);
    
    void onBinaryMessage(String text);
}
