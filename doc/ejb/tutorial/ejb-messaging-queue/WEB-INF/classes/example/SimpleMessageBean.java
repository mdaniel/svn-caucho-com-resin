package example;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class SimpleMessageBean implements MessageDrivenBean, MessageListener {
  static protected final Logger log = 
    Logger.getLogger(SimpleMessageBean.class.getName());

  public void ejbCreate()
    throws EJBException
  {
    log.fine("ejbCreate()");
  }

  public void setMessageDrivenContext(MessageDrivenContext cxt)
    throws EJBException
  {
    log.fine("setMessageDrivenContext()");
  }

  public void onMessage(Message msg)
  {
    // process the message
    String text = null;

    if (msg instanceof TextMessage) {
      try {
        text = ((TextMessage) msg).getText();
      } catch (Exception ex) {
        log.log(Level.WARNING,null,ex);
      }
    }
    else {
      text = msg.toString();
    }

    log.info("onMessage(): " + text);
  }

  public void ejbRemove()
    throws EJBException
  {
    log.fine("ejbRemove()");
  }
}

