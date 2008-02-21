package example;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.MessageListener;

public class MyListener implements MessageListener {
  private static final Logger log
    = Logger.getLogger(MyListener.class.getName());
  private static String _lastMessage;

  /**
   * Returns the last received message.
   */
  public static String getLastMessage()
  {
    return _lastMessage;
  }
  
  /**
   * Receives the message.
   */
  public void onMessage(Message message)
  {
    try {
      ObjectMessage objMessage = (ObjectMessage) message;

      log.info("received: " + objMessage.getObject());

      _lastMessage = (String) objMessage.getObject();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
