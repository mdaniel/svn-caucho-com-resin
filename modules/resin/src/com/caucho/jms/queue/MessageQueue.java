package com.caucho.jms.queue;

import com.caucho.jms.message.MessageImpl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides abstract implementation for a memory queue.
 */
public interface MessageQueue extends MessageDestination
{
  /**
   * Listen for a message from the queue, registering the callback
   * until a message is received or removeMessageCallBack removes it.
   *
   * When the message is received, listen automatically removes the callback.
   */
  public boolean listen(MessageCallback callback)
    throws MessageException;

  /**
   * Removes the callback, e.g. for a timeout
   */
  public void removeMessageCallback(MessageCallback callback);
    
  /**
   * Rollback a message read
   */
  public void rollback(String msgId);
    
  /**
   * Acknowledges the receipt of a message
   */
  public void acknowledge(String msgId);

  /**
   * Browsing
   */
  // public ArrayList<String> getMessageIds();
}
