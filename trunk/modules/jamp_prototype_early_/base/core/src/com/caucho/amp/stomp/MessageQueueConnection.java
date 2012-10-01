package com.caucho.amp.stomp;

import java.io.Closeable;
import java.io.IOException;

public interface MessageQueueConnection extends Closeable{

    public abstract void connect(String connectionString, String login, String passcode) throws IOException;

    public abstract void send(String destination, String message)
            throws IOException;

    public abstract void subscribe(String destination,
            MessageListener messageListener) throws IOException;

    public abstract void unsubscribe(String destination,
            MessageListener messageListener) throws IOException;

    public abstract void close() throws IOException;

}