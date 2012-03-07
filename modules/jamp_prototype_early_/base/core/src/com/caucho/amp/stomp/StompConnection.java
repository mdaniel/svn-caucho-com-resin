package com.caucho.amp.stomp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This only works with Strings for now, because that is all we need for this
 * iteration. Needs logging badly.
 */
public class StompConnection implements Closeable, MessageQueueConnection {

    enum Version {
        ONE, ONE_ONE
    };

    Version version;
    int currentSubscription;
    Map<Integer, MessageListener> subscriptions = Collections.synchronizedMap(new HashMap<Integer, MessageListener>());
    Map<MessageListener, Integer> subscriptionsIds = Collections.synchronizedMap(new HashMap<MessageListener,Integer>());
    String host;
    int port;
    Socket socket;
    PrintWriter out;
    BufferedReader reader;// This is somewhat limiting since Stomp can do both
                          // bytes and strings, but this is it for now, just
                          // text
    
    BlockingQueue<String> closeQueue = new ArrayBlockingQueue<String>(1);
    boolean debug = true;
    Pattern stompURLMatcher = Pattern
            .compile("^stomp://([a-zA-Z0-9_]*):([0-9]*)//?.*");
    boolean connected;
    static ThreadPoolExecutor threadPool = null;
    int receiptNumber;

    /**
     * This should be called before connect is ever called. Not a requirement to
     * ever call this, but if you do, do it before connect.
     */
    public static void setThreadPool(ThreadPoolExecutor athreadPool) {
        threadPool = athreadPool;

    }

    @Override
    public void connect(String connectionString, String login, String passcode) throws IOException {

        if (connected) {
            throw new IOException("Already connected.");
        }

        /* Parse connectString URL for host, and port. */
        Matcher matcher = stompURLMatcher.matcher(connectionString);
        if (!matcher.matches()) {
            throw new IOException("Not a valid stomp connection string");
        }
        host = matcher.group(1);
        port = Integer.parseInt(matcher.group(2));

        if (debug)
            System.out.printf("host %s, port %s \n", host, port);


        /* Create a new socket. */
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

        /* Send the connect command to authenticate. */
        sendCommand("CONNECT", String.format("host:%s", host), "accept-version:1.1,1.0",
                String.format("login:%s", login), String.format("passcode:%s",passcode));
        
        /* Look for CONNECTED string. */
        String line = reader.readLine();

        if (!line.equals("CONNECTED")) {
            throw new IOException("Expecting CONNECTED handshake");
        }

        /* Read the headers and see if we are compatible with the server's version. */
        Properties headers = readHeaders();
        String sversion = headers.getProperty("version");

        if (sversion.equals("1.1")) {
            version = Version.ONE_ONE;
        } else if (version.equals("1")) {
            version = Version.ONE;
        } else {
            throw new IOException("Unsupported Stomp version number = "
                    + sversion);
        }

        if (debug)
            System.out.printf("Version is %s \n", sversion);

        /* Clear out and validate the rest of the message. */
        readResult();

        connected = true;

        runMessageDispatchLoop();
    }

    private void runMessageDispatchLoop() {
        
        /* Create the pool if it does not exist. */
        synchronized (StompConnection.class) {
            if (threadPool == null) {
                threadPool = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(10));
            }
        }

        threadPool.execute(new Runnable() {

            @Override
            public void run() {
                dispatchLoop();
            }
        });

    }

    /** The actual dispatch look that handles command frames from the server. */
    private void dispatchLoop() {
        try {

            String line = null;
            while (connected && socket!=null && socket.isConnected() && (line = reader.readLine()) != null) {
                    
                    /* Ignore heart beat */
                    if ("".equals(line.trim())) {
                        continue;
                    }
                    
                    if ("MESSAGE".equals(line) ) {
                        handleMessage();
                    } else if ("RECEIPT".equals(line)) {
                        handleReceipt();
                    } else if ("ERROR".equals(line) ) {
                        handleError();
                    } else {
                        if (debug)
                            System.out.println("Unknown command " + line);
                    }
                
            }

        } catch (SocketException se) {
            if (se.getMessage().contains("Socket closed")) {
                return; // this is ok... it just means that the server
                        // disconnected.
            } else {
                se.printStackTrace();
            }
        } catch (IOException ioe) {
            // don't care
            ioe.printStackTrace();
        } finally {

            try {
                if (socket!=null)
                socket.close();
            } catch (IOException e) {
            }

        }

    }

    private void handleError() throws IOException {
        this.readHeaders();

        StringBuilder readBody = readBody(0);
        
        System.err.printf("ERROR from server:: %s", readBody);

    }

    private void handleReceipt() throws IOException {
        Properties headers = this.readHeaders();
        String receipt = headers.getProperty("receipt-id");
        
        if (debug) System.out.printf("GOT RECEIPT %s\n", receipt);
        
        if (receipt != null && receipt.startsWith("close-")) {
            String[] split = receipt.split("-");
            closeQueue.offer(split[1]);
        }
        
        readBody(0);
        

    }

    private void handleMessage() throws IOException {
        Properties headers = this.readHeaders();
        
        String subscription = headers.getProperty("subscription");
        String messageId = headers.getProperty("message-id");
        String destination = headers.getProperty("destination");
        int length = 0;

        if (subscription == null) {
            System.err.println("subscription missing from message");
        }
        
        if (messageId == null) {
            //throw new IOException("messageId missing from message");
        }

        if (destination == null) {
            //throw new IOException("destination missing from message");
        }
   
        
     
        String slength = headers.getProperty("content-length");

        if (slength!=null) {
            length = Integer.parseInt(slength);
        }
        
        if (debug) System.out.printf("handleMessage %s %s %s %s \n", subscription, messageId, destination, length);

        final StringBuilder body = readBody(length);
        
        synchronized (this) {
            if (subscription!=null){
                MessageListener messageListener = this.subscriptions.get(Integer.parseInt(subscription));
                if (messageListener!=null) {
                    try {
                        messageListener.onTextMessage(body.toString());
                    } catch (Exception ex) {
                        //you need to start logging stuff
                    }
                }
            }
        }
        

    }

    private StringBuilder readBody(int length) throws IOException {
        
        if (length==0) {
            final StringBuilder body = new StringBuilder();

            for (char ch = (char) reader.read(); ch != 0; ch = (char) reader
                    .read()) {
                body.append(ch);
            }
            return body;
        } else {
            final StringBuilder body = new StringBuilder();
            char[] buffer = new char[length];
            int actual = reader.read(buffer, 0, length);
            body.append(buffer);
            if (actual != length) {
                throw new IOException(String.format("unable to read content-length:%d from stream", length));
                
            }
            int ch = reader.read();
            if (ch!=0) {
                throw new IOException(String.format("unable to read content-length:%d from stream", length));
            }
            return body;
        }
    }


    private String readResult() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (char ch = (char) reader.read(); ch != 0; ch = (char) reader.read()) {
            builder.append(ch);
        }

        String result = builder.toString();
        return result;
    }

    void sendCommand(String command, String... headers) throws IOException {

        this.sendCommandWithBody(command, null, headers);

    }

    void sendCommandWithBody(String command, String body, String... headers)
            throws IOException {

        out.printf("%s\n", command);
        for (String header : headers) {
            out.printf("%s\n", header);
        }
        out.println();
        if (body != null) {
            out.print(body);
        }
        out.printf("\u0000\n");

    }

    private Properties readHeaders() throws IOException {
        Properties props = new Properties();
        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line)) {
                break;
            } else {
                String[] split = line.split(":");
                if (split.length==2)
                props.put(split[0], split[1]);
            }
        }

        if (debug)
            System.out.println("HEADERS = " + props);

        return props;

    }

    /* (non-Javadoc)
     * @see com.caucho.amp.stomp.MessageQueueConnection#send(java.lang.String, java.lang.String)
     */
    @Override
    public void send(String destination, String message) throws IOException {
        if (!connected)
            throw new IOException("Not connected");
        
        this.sendCommandWithBody("SEND", message, 
                String.format("destination:%s", destination), 
                "content-type:text/plain", 
                String.format("content-length:%d", 
                        message.length()));


    }

    /* (non-Javadoc)
     * @see com.caucho.amp.stomp.MessageQueueConnection#subscribe(java.lang.String, com.caucho.amp.stomp.MessageListener)
     */
    @Override
    public void subscribe(String destination, MessageListener messageListener)
            throws IOException {
        if (!connected)
            throw new IOException("Not connected");
        
        int id = 0;
        synchronized(this) {
            this.currentSubscription++;
            id = this.currentSubscription;
            this.subscriptions.put(id, messageListener);          
            this.subscriptionsIds.put(messageListener, id);
        }

        this.sendCommand("SUBSCRIBE", String.format("id:%s", id),  String.format("destination:%s", destination));

    }

    /* (non-Javadoc)
     * @see com.caucho.amp.stomp.MessageQueueConnection#unsubscribe(java.lang.String, com.caucho.amp.stomp.MessageListener)
     */
    @Override
    public void unsubscribe(String destination, MessageListener messageListener)
            throws IOException {
        if (!connected)
            throw new IOException("Not connected");

        int id = 0;
        synchronized(this) {
            this.subscriptions.remove(messageListener);
            id = this.subscriptionsIds.get(messageListener);
            subscriptionsIds.remove(id);
        }
        this.sendCommand("UNSUBSCRIBE", String.format("id:%s", id));
    }

    /* (non-Javadoc)
     * @see com.caucho.amp.stomp.MessageQueueConnection#close()
     */
    @Override
    public void close() throws IOException {
        
        
        int latest = 0;
        synchronized (this) {//not really needed
            latest = this.receiptNumber++;
        }
        String receipt = "close-" + latest;
        
        this.sendCommand("DISCONNECT", String.format("receipt:%s", receipt));
        
        try {
            String receiptFromServer = closeQueue.poll(10, TimeUnit.SECONDS);
            if (("" + latest).equals(receiptFromServer)) {
                if (debug)System.out.println("Shutting down properly");
                this.socket.close();
                this.socket = null;
            } 
        } catch (InterruptedException e) {            
            throw new IOException("Unable to shutdown properly from server");
        } finally {
            this.connected = false;
            
            try {
                if (this.socket!=null) {
                    this.socket.close();
                }
            } catch (IOException ioe) {
            }

        }
        


    }

}
