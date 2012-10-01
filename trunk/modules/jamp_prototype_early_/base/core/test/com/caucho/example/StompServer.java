package com.caucho.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** This is only for testing JAMP, this is not a serious STOMP Server.
 * This implementation assumes all messages are content-type/plain-text.
 * Again it is only for testing.
 *  */
public class StompServer {

    private int poolSize = 2;

    private int maxPoolSize = 2;

    private long keepAliveTime = 10;

    private boolean debug = true;

    ThreadPoolExecutor threadPool = null;
    private static int port = 6666;
    private static int maxConnections = 0;
    private String userName = "rick";
    private String password = "rick";
    private BigDecimal version = new BigDecimal("1.1");
    private BigDecimal[] versionsSupported = new BigDecimal[] {
            new BigDecimal("1.0"), version };

    private Map<String, BlockingQueue<String>> queues = Collections
            .synchronizedMap(new HashMap<String, BlockingQueue<String>>());

    private List<MessageListener> listeners = Collections
            .synchronizedList(new ArrayList<MessageListener>());

    private BlockingQueue<String> getQueue(String queueName) {
        BlockingQueue<String> queue = this.queues.get(queueName);

        if (queue == null) {

            queue = new ArrayBlockingQueue<String>(2000);
            this.queues.put(queueName, queue);
            System.out.println("Created new queue");
        } else {
            System.out.println("Found queue");
        }

        return queue;
    }

    private void run() {

        threadPool = new ThreadPoolExecutor(poolSize, maxPoolSize,
                keepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(5));

        int i = 0;

        try {
            ServerSocket listener = new ServerSocket(port);

            while ((i++ < maxConnections) || (maxConnections == 0)) {

                Socket socket = listener.accept();
                StompConnection stompConnection = new StompConnection(socket);
                Thread t = new Thread(stompConnection);
                t.start();
            }
        } catch (IOException ioe) {
            System.out.println("IOException on socket listen: " + ioe);
            ioe.printStackTrace();
        }

    }

    // Listen for incoming connections and handle them
    public static void main(String[] args) {
        StompServer server = new StompServer();
        server.run();
    }

    public class MessageListener {
        String id;
        StompConnection connection;
        String destination;

        public void handleMessage(String message) throws IOException {
            connection.sendMessage(message, id);
        }

        @Override
        public String toString() {
            return "MessageListener [id=" + id + ", connection=" + connection
                    + ", destination=" + destination + "]";
        }
    }

    public class StompConnection implements Runnable {
        Socket socket;
        BufferedReader reader;
        PrintStream out;

        Map<String, MessageListener> listenerIdMap = new HashMap<String, MessageListener>();

        private boolean connected = false;

        public StompConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));

        }

        public void sendMessage(String message, String id) throws IOException {
            this.sendCommandWithBody("MESSAGE", message, String.format("subscription:%s",id));
        }

        @Override
        public void run() {

            try {
                // Get input from the client
                out = new PrintStream(socket.getOutputStream());
                String line = null;
                while ((line = reader.readLine()) != null) {
                    
                    if ("".equals(line)) {
                        continue;
                    }
                    if ("CONNECT".equals(line) && !connected) {
                        handleConnect();
                        if (!connected) {
                            break;
                        }
                    } else if ("SEND".equals(line) && connected) {
                        handleSend();
                    } else if ("SUBSCRIBE".equals(line) && connected) {
                        handleSubscribe();
                    }else if ("UNSUBSCRIBE".equals(line) && connected) {
                        handleUnsubscribe();
                    }else if ("DISCONNECT".equals(line) && connected) {
                        handleDisconnect();
                    } else {
                        //ACK, NACK, BEGIN, COMMIT, ABORT, are not supported yet
                        //They will be when the client supports them and I need something to test.
                        //I don't plan on ever supporting BEGIN, COMMIT or ABORT.
                        if (connected)
                        error("NOT SUPPORTED YET (ACK, NACK, BEGIN, COMMIT, ABORT are not supported) unknown command: " + line);
                    }
                }

            } catch (SocketException se) {
                if (se.getMessage().contains("Connection reset")) {
                    return; //this is ok... it just means that the client disconnected.
                } else {
                    se.printStackTrace();
                }
            } catch (IOException ioe) {
                // don't care
                ioe.printStackTrace();
            } finally {

                try {
                    socket.close();
                } catch (IOException e) {
                }

            }

        }

        private void handleDisconnect() throws IOException {
            this.connected = false;
            
            Properties headers = this.readHeaders();
            String receiptId = headers.getProperty("receipt");
            
            if (receiptId!=null) {
                sendCommand("RECEIPT", String.format("receipt-id:%s",receiptId));
                if (debug)System.out.printf("receipt found = %s\n", receiptId);

            } else {
                if (debug)System.out.println("receipt not found");
            }
            
            this.out.flush();

        }

        private void handleUnsubscribe() throws IOException {
            Properties headers = this.readHeaders();
            final String id = headers.getProperty("id");
            MessageListener messageListener = listenerIdMap.get(id);

            listeners.remove(messageListener);

            if (verifyStop()) {
            } else {
                error("Did not find stop bit for recieve");
            }

        }

        private void handleSubscribe() throws IOException {
            Properties headers = this.readHeaders();
            final String destination = headers.getProperty("destination");
            final String id = headers.getProperty("id");
            final String ack = headers.getProperty("ack");
            
            if (id==null) {
                error("id is a required header");
            }
            
            if (destination==null) {
                error("destination is a required field");
            }
            
            if (ack!=null && !ack.equals("auto")) {
                error("currently this test server only supports auto ack, other modes will be needed for testing, but for now, just auto");
            }
            
            
            MessageListener messageListener = new MessageListener();
            messageListener.connection = this;
            messageListener.destination = destination;
            messageListener.id = id;
            listeners.add(messageListener);
            listenerIdMap.put(id, messageListener);
            
            /* This might be first subscriber so go ahead and send him some messages. */
            sendAll(destination);


            if (verifyStop()) {
            } else {
                error("Did not find stop bit for recieve");
            }

        }

        private boolean verifyStop() throws IOException {
            char ch = (char) reader.read();

            if (ch == 0) {

                ch = (char) reader.read();

                if (ch == '\n') {
                    return true;
                } else {
                    error("protocol error, newline missing");
                    return false;
                }
            } else {
                error("protocol error, null missing to terminate message");
                return false;
            }
        }

        private void handleSend() throws IOException {
            Properties headers = this.readHeaders();
            final String destination = headers.getProperty("destination");
            final String slength = headers.getProperty("content-length");
            int length = 0;

            if (destination == null) {
                error("You must specify a destination, see STOMP specification");
                return;
            }
            
            if (slength!=null) {
                length = Integer.parseInt(slength);
            }


            final StringBuilder body = readBody(length);

            if (body!=null) {
                BlockingQueue<String> queue = getQueue(destination);
                queue.offer(body.toString());
            }
            char ch = (char) reader.read();

            if (ch != '\n') {
                error("protocol error expecting line feed");
            }

            if (debug)
                System.out.printf("READ BODY %s\n", body);

            sendAll(destination);
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
                    error(String.format("unable to read content-length:%d from stream", length));
                    return null;
                }
                int ch = reader.read();
                if (ch!=0) {
                    error(String.format("protocol unable to read content-length:%d from stream", length));
                    return null;
                }
                return body;
            }
        }

        private void handleConnect() throws IOException {

            if (debug)
                System.out.println("CONNECT");
            Properties props = readHeaders();

            BigDecimal version = getHighestVersion(props);
            if (version.longValue() == -1) {
                error("Supported Protocols are 1.0 1.1", "version:1.0,1.1");
                return;
            }

            if (debug)
                System.out.printf("version agreed on %s\n", version);

            if (userName.equals(props.getProperty("login"))) {
                if (debug)
                    System.out.printf("User matches%s\n", userName);

                if (!password.equals(props.getProperty("passcode"))) {
                    error("Authentication failed");
                    return;
                } else {
                    if (debug)
                        System.out.printf("passcode matches%s\n", password);

                }

            } else {
                error("Unkown login");
                return;
            }

            if (verifyStop()) {
                sendCommand("CONNECTED", String.format("version:%s", version), "server:StompTester/0.01 *not for production use, just for testing");
                this.connected = true;
            } else {
                if (debug)
                    System.out.println("Unable to connect");
            }

        }

        private void error(String message, String... headers)
                throws IOException {

            List<String> list = new ArrayList<String>();
            for (String header : headers) {
                list.add(header);
            }
            list.add("content-type:text/plain");
            sendCommandWithBody("ERROR", message,
                    list.toArray(new String[list.size()]));
            out.flush();
        }

        private BigDecimal getHighestVersion(Properties props) {
            String property = props.getProperty("accept-version", "99");
            String[] allowedVersions = property.split(",");

            BigDecimal highest = BigDecimal.ZERO;
            for (int index = 0; index < allowedVersions.length; index++) {
                BigDecimal current = new BigDecimal(allowedVersions[index]);
                BigDecimal max = highest.max(current);

                /* The highest has to be less than the version. */
                int compareTo = max.compareTo(StompServer.this.version);
                if (compareTo == -1 || compareTo == 0) {
                    highest = max;
                }
            }

            for (int index = 0; index < StompServer.this.versionsSupported.length; index++) {
                if (StompServer.this.versionsSupported[index].equals(highest)) {
                    return highest;
                }
            }

            return BigDecimal.valueOf(-1);
        }

        private Properties readHeaders() throws IOException {
            Properties props = new Properties();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("".equals(line)) {
                    break;
                } else {
                    String[] split = line.split(":");
                    props.put(split[0], split[1]);
                }
            }

            if (debug)
                System.out.println("HEADERS" + props);

            return props;

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

    }

    private void sendAll(final String destination) {
        threadPool.execute(new Runnable() {

            @Override
            public void run() {

                synchronized (listeners) {

                    Iterator<MessageListener> iterator = listeners.iterator();
                    while (iterator.hasNext()) {
                        MessageListener messageListener = iterator.next();

                        if (messageListener.connection.socket.isClosed()) {
                            iterator.remove();
                        }
                    }
                    for (MessageListener listener : listeners) {
                        BlockingQueue<String> queue = getQueue(destination);

                        String body = "";

                        while (body != null) {
                            try {
                                // See if there is a message already waiting
                                body = queue.poll(1, TimeUnit.NANOSECONDS);
                            } catch (InterruptedException e) {
                                body = null;
                            }
                            if (body != null) {
                                try {
                                    listener.handleMessage(body);
                                } catch (IOException e) {

                                    System.err
                                            .println("UNABLE TO SEND MESSAGE "
                                                    + listener + "\n");
                                    listeners.remove(listener);
                                }
                            }
                        }

                    }
                }
            }
        });
    }

}
