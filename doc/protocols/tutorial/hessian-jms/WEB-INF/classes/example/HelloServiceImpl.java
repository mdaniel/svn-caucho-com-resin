package example;

import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

public class HelloServiceImpl implements HelloService {
  private static final Logger log =
    Logger.getLogger(HelloService.class.getName());

  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private MessageProducer _producer;
  private Session _jmsSession;

  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  public void setResultQueue(Destination destination)
  {
    _destination = destination;
  }

  public void init()
  {
    try {
      Connection connection = _connectionFactory.createConnection();
      _jmsSession = 
        connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      _producer = _jmsSession.createProducer(_destination);
    } catch (JMSException e) {
      log.warning(e.toString());
    }
  }
  
  public void hello()
  {
    try {
      TextMessage textMessage = _jmsSession.createTextMessage();

      textMessage.setText("hello, world");

      _producer.send(textMessage);
    } catch (JMSException e) {
      log.warning(e.toString());
    }
  }
}
