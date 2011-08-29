package example;

import javax.jms.*;
import javax.webbeans.*;

public class MyListener implements MessageListener {
  @In private MessageStoreService _messageStore;

  public void onMessage(Message message)
  {
    try {
      if (message instanceof TextMessage) {
	String text = ((TextMessage) message).getText();
	
	_messageStore.addMessage(text);
      }
      else if (message instanceof ObjectMessage) {
	Object value = ((ObjectMessage) message).getObject();
	
	_messageStore.addMessage(String.valueOf(value));
      }
      else
	_messageStore.addMessage(String.valueOf(message));
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }
}

