package example;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.jms.*;
import javax.ejb.*;
import javax.naming.*;

public class AdProducer 
  implements MessageDrivenBean, MessageListener {

  private static final Logger log =
    Logger.getLogger(AdProducer.class.getName());

  private MessageDrivenContext _messageDrivenContext;
  private Random _random = new Random();
  private Session _jmsSession;
  private Connection _connection;
  private ConnectionFactory _connectionFactory;
  private SessionContext _sessionContext;
  private MessageProducer _producer;

  private static final String[] _ads = {
    "Buy widgets",
    "Watch this movie",
    "Eat at Joe's",
    "Learn a new trade",
    "Find your mate"
  };

  public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext)
    throws EJBException
  {
    _messageDrivenContext = messageDrivenContext;
  }

  public void ejbCreate()
  {
    try {
      Context context = (Context) new InitialContext().lookup("java:comp/env");

      ConnectionFactory connectionFactory = 
        (ConnectionFactory) context.lookup("jms/ConnectionFactory");

      Destination destination = (Destination) context.lookup("jms/AdQueue");

      _connection = connectionFactory.createConnection();
      _jmsSession = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      _producer = _jmsSession.createProducer(destination);

      // Initialize with a single ad
      TextMessage outgoingMessage = _jmsSession.createTextMessage();

      String ad = _ads[_random.nextInt(_ads.length)];

      outgoingMessage.setText(ad);

      _producer.send(outgoingMessage);
    } catch (Exception e) {
      log.fine(e.toString());
    }
  }

  public void onMessage(Message incomingMessage)
  {
    try {
      TextMessage outgoingMessage = _jmsSession.createTextMessage();

      String ad = _ads[_random.nextInt(_ads.length)];

      outgoingMessage.setText(ad);

      _producer.send(outgoingMessage);
    } catch (JMSException e) {
      _messageDrivenContext.setRollbackOnly();
    }
  }

  public void ejbRemove()
  {
    try {
      _connection.close();
    } catch (JMSException e) {
      log.fine(e.toString());
    }
  }
}

