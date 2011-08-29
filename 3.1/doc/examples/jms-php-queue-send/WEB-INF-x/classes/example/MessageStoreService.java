package example;

import java.io.*;
import java.util.*;
import javax.jms.*;
import javax.ejb.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Custom singleton service to store messages
 */
public class MessageStoreService {
  private LinkedList<String> _messageLog = new LinkedList<String>();

  public void addMessage(String message)
  {
    synchronized (_messageLog) {
      if (_messageLog.size() > 10)
	_messageLog.remove(0);

      _messageLog.add(message);
    }
  }

  public ArrayList<String> getMessages()
  {
    synchronized (_messageLog) {
      return new ArrayList<String>(_messageLog);
    }
  }
}

