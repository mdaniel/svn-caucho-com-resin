package com.caucho.jms.queue;

import com.caucho.jms.message.MessageImpl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides abstract implementation for a memory queue.
 */
public interface MessageQueue
{
  /**
   * Sends a message to the destination
   */
  public void send(String msgId,
		   Serializable msg,
		   int priority,
		   long expires)
    throws MessageException;
  
  /**
   * Synchronous/blocking message receiving.
   * Listen for a message from the queue, until a message is received
   * or the timeout occurs.
   */
  public QueueEntry receiveEntry(long timeout, boolean isAutoAck)
    throws MessageException;
    
  /**
   * Rollback a message read
   */
  public void rollback(String msgId);
    
  /**
   * Acknowledges the receipt of a message
   */
  public void acknowledge(String msgId);
  
  /**
   * Registers a message callback with the queue.  Each message callback
   * will receive messages one at a time until the messages complete.
   */
  public void addMessageCallback(MessageCallback callback)
    throws MessageException;

  /**
   * Removes the callback when messages are done listening
   */
  public void removeMessageCallback(MessageCallback callback);

  /**
   * Browsing
   */
  // public ArrayList<String> getMessageIds();
}
