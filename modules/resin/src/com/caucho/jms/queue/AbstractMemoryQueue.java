package com.caucho.jms.queue;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.jms.message.MessageImpl;

/**
 * Provides abstract implementation for a memory queue.
 * 
 */
public abstract class AbstractMemoryQueue extends AbstractQueue
{
  
  protected final Object _queueLock = new Object();

  protected QueueEntry []_head = new QueueEntry[10];
  protected QueueEntry []_tail = new QueueEntry[10];
  
  //
  // JMX statistics
  //

  /**
   * Returns the queue size
   */
  public int getQueueSize()
  {
    int count = 0;

    for (int i = 0; i < _head.length; i++) {
      for (QueueEntry entry = _head[i];
           entry != null;
           entry = entry._next) {
        count++;
      }
    }

    return count;
  }
  
  @Override
  public ArrayList<MessageImpl> getBrowserList()
  {

    ArrayList<MessageImpl> browserList = new ArrayList<MessageImpl>();
    for (int i = 0; i < _head.length; i++) {
      for (QueueEntry entry = _head[i];
           entry != null;
           entry = entry._next) {
        browserList.add((MessageImpl)getPayload(entry));
      }
    }

    return browserList;    
  }
  
  /**
   * Returns true if a message is available.
   */
  @Override
  public boolean hasMessage()
  {
    return getQueueSize() > 0;
  }  
  
  /**
   * 
   * @param entry
   * @return        Payload associated with the entry.
   */
  protected MessageImpl getPayload(QueueEntry entry) 
  {    
    return (MessageImpl)entry.getPayload();
  }  
    
  /**
   * Acknowledges the receipt of a message
   */
  @Override
  public void acknowledge(String msgId)
  {
    acknowledgeImpl(msgId);
  }

  /**
   * Removes message.
   */
  protected QueueEntry acknowledgeImpl(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
        for (QueueEntry entry = _head[i];
             entry != null;
             entry = entry._next) {
          if (msgId.equals(entry.getMsgId())) {
            if (entry.isRead()) {
              removeEntry(entry);
              return entry;
            }
              
            return null;
          }
        }
      }
    }

    return null;
  }
  
  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  protected QueueEntry receiveImpl(boolean isAutoAck)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
        for (QueueEntry entry = _head[i];
             entry != null;
             entry = entry._next) {

          if (! entry.isLease()) {
            continue;
          }

          if (entry.isRead()) {
            continue;
          }
          
          entry.setRead(true);
          
          if (isAutoAck) {
            removeEntry(entry);
          }

          return entry;
        }
      }
    }

    return null;
  }
    
  
  /**
   * Rolls back the receipt of a message
   */
  @Override
  public void rollback(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
        for (QueueEntry entry = _head[i];
             entry != null;
             entry = entry._next) {
          if (msgId.equals(entry.getMsgId())) {
            if (entry.isRead()) {
              entry.setRead(false);

              MessageImpl msg = (MessageImpl) getPayload(entry);
        
              if (msg != null)
                msg.setJMSRedelivered(true);
            }
            
            return;
          }
        }
      }
    }
  }

  protected QueueEntry addEntry(QueueEntry entry) {
    
    int priority = entry.getPriority();
    
    synchronized (_queueLock) {
      entry._prev = _tail[priority];

      if (_tail[priority] != null)
        _tail[priority]._next = entry;
      else
        _head[priority] = entry;

      _tail[priority] = entry;

      return entry;
    }
  }
  
  protected void removeEntry(QueueEntry entry)
  {
    int priority = entry.getPriority();
    
    QueueEntry prev = entry._prev;
    QueueEntry next = entry._next;
    
    if (prev != null)
      prev._next = next;
    else if (_head[priority] == entry)
      _head[priority] = next;

    if (next != null)
      next._prev = prev;
    else if (_tail[priority] == entry)
      _tail[priority] = prev;
  }  
}
