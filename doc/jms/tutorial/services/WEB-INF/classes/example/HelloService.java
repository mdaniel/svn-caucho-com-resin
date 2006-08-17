package example;

import java.util.logging.Logger;

import javax.jws.WebService;
import javax.jws.WebMethod;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

@WebService
public class HelloService {
  private static final Logger log =
    Logger.getLogger(HelloService.class.getName());

  private ConnectionFactory _connectionFactory;
  private Destination _destination;
  private MessageProducer _producer;
  private Session _jmsSession;

  @WebMethod(exclude=true)
  public void setConnectionFactory(ConnectionFactory connectionFactory)
  {
    _connectionFactory = connectionFactory;
  }

  @WebMethod(exclude=true)
  public void setResultQueue(Destination destination)
  {
    _destination = destination;
  }

  @WebMethod(exclude=true)
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
  
  @WebMethod
  public int hello()
  {
    try {
      TextMessage textMessage = _jmsSession.createTextMessage();

      textMessage.setText("hello, world");

      _producer.send(textMessage);
    } catch (JMSException e) {
      log.warning(e.toString());
    }

    return 0;
  }
}
