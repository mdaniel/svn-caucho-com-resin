package example;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.naming.NamingException;
import javax.jms.JMSException;

public class MessageSenderServlet extends GenericServlet {
  static protected final Logger log = 
    Logger.getLogger(MessageSenderServlet.class.getName());

  Queue _queue;
  ConnectionFactory _factory;
  
  public void init()
    throws ServletException
  {
    super.init();

    // look up the objects.
    try {
      Context env = (Context) new InitialContext().lookup("java:comp/env");
      _queue = (Queue) env.lookup("jms/queue");
      if (_queue == null)
        throw new ServletException("`java:comp/env/jms/queue' lookup failed");

      _factory = (ConnectionFactory) env.lookup("jms/queue-connection-factory");
      if (_factory == null)
        throw new ServletException("`java:comp/env/jms/queue-connection-factory' lookup failed");

    } catch (NamingException ex) {
      throw new ServletException(ex);
    }
  }

  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    int count = 5;

    try {
      Connection connection = _factory.createConnection();
      int ackMode = Session.AUTO_ACKNOWLEDGE;
      Session jmsSession = connection.createSession(false, ackMode);

      MessageProducer producer = jmsSession.createProducer(_queue);

      for (int i = 1; i <= count; i++) {
        String text = "hello, world: message #" + String.valueOf(i);
        sendMessage(jmsSession,producer,text);
      }
    } catch (JMSException ex) {
      throw new ServletException(ex);
    }

    PrintWriter out = response.getWriter();
    out.print("Sent " + String.valueOf(count) + " messages.");
  }

  protected void sendMessage(Session jmsSession,
			     MessageProducer producer,
			     String text)
    throws JMSException
  {
    // create the message
    Message message = jmsSession.createTextMessage(text);

    // send the message
    producer.send(message);

    log.info("Sent message: " + text);
  }
}

