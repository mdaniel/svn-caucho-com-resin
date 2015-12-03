package com.caucho.jms.queue;


/**
 * Provides abstract API for a queue.
 */
public interface MessageTopicSubscriber<E>
{
  /**
   * Sends a message to the destination
   */
  public void send(String msgId,
                   E msg,
                   int priority,
                   long expireTime,
                   String publisherId)
    throws MessageException;
}
