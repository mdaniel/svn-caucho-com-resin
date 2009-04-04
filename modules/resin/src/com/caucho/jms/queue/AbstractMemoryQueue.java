package com.caucho.jms.queue;

import com.caucho.jms.message.MessageImpl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides abstract implementation for a memory queue.
 * 
 */
public abstract class AbstractMemoryQueue<E extends QueueEntry>
  extends AbstractQueue
{
  protected final Object _queueLock = new Object();

  private ArrayList<MessageCallback> _callbackList
    = new ArrayList<MessageCallback>();

  protected QueueEntry []_head = new QueueEntry[10];
  protected QueueEntry []_tail = new QueueEntry[10];

  //
  // Abstract/stub methods to be implemented by the Queue
  //
  /**
   * Sends a message to the queue
   */
  @Override
  abstract public void send(String msgId,
			    Serializable payload,
			    int priority,
			    long expires)
    throws MessageException;

  protected Serializable readPayload(E entry)
  {
    return null;
  }

  protected void acknowledge(E entry)
  {
  }

  //
  // send implementation
  //

  protected void addQueueEntry(E entry)
  {
    addEntry(entry);

    dispatchMessage();
  }

  //
  // receive implementation
  //

  public boolean listen(MessageCallback callback)
    throws MessageException
  {
    registerMessageCallback(callback);

    dispatchMessage();

    return true;
  }

  protected void dispatchMessage()
  {
    ArrayList<MessageCallback> callbackList = _callbackList;

    if (callbackList.size() == 0)
      return;

    // find entry and mark it read
    E entry = readEntry();

    if (entry == null)
      return;

    Serializable payload = entry.getPayload();

    if (payload == null) {
      payload = readPayload(entry);

      entry.setPayload(payload);
    }

    boolean isAck = false;
    
    synchronized (callbackList) {
      if (callbackList.size() == 0) {
	// if all callbacks have been removed during the readEntry,
	// mark the entry as unread
	entry.rollback();
	return;
      }

      MessageCallback callback = callbackList.remove(0);

      isAck = callback.messageReceived(entry.getMsgId(), entry.getPayload());
    }

    if (isAck)
      acknowledge(entry.getMsgId());
  }
  
  public void addMessageCallback(MessageCallback callback)
  {
    registerMessageCallback(callback);

    dispatchMessage();
  }
  
  /**
   * Adds a MessageAvailableListener to receive notifications for new
   * messages.  The listener will spawn or wake a thread to process
   * the message.  The listener MUST NOT use the event thread to
   * process the message.
   * 
   * @param listener notification listener
   */
  protected void registerMessageCallback(MessageCallback callback)
  {
    synchronized (_callbackList) {
      if (! _callbackList.contains(callback))
	_callbackList.add(callback);
    }
  }
  
  public void removeMessageCallback(MessageCallback callback)
  {
    synchronized (_callbackList) {
      _callbackList.remove(callback);
    }
  }

  protected boolean messageReceived(String msgId, Serializable message)
  {
    synchronized (_callbackList) {
      int size = _callbackList.size();
      
      for (int i = 0; i < size; i++) {
	MessageCallback callback = _callbackList.get(i);

	if (callback.messageReceived(msgId, message)) {
	  _callbackList.remove(i);
	  
	  return true;
	}
      }

      return false;
    }
  }
    
  /**
   * Acknowledges the receipt of a message
   */
  @Override
  public void acknowledge(String msgId)
  {
    E entry = removeEntry(msgId);

    if (entry != null)
      acknowledge(entry);
  }
  
  //
  // Queue size statistics
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
  
  /**
   * Returns true if a message is available.
   */
  @Override
  public boolean hasMessage()
  {
    return getQueueSize() > 0;
  }
  

  //
  // queue management
  //

  /**
   * Add an entry to the queue
   */
  protected E addEntry(E entry)
  {
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

  /**
   * Remove an entry from the queue
   */
  protected void removeEntry(E entry)
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
  
  /**
   * Returns the next entry from the queue
   */
  protected E readEntry()
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

          return (E) entry;
        }
      }
    }

    return null;
  }

  /**
   * Removes message.
   */
  protected E removeEntry(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
	QueueEntry prev = null;
	
        for (QueueEntry entry = _head[i];
             entry != null;
             entry = entry._next) {
          if (msgId.equals(entry.getMsgId())) {
	    if (prev != null)
	      prev._next = entry._next;
	    else
	      _head[i] = entry._next;
	    
	    return (E) entry;
          }

	  prev = entry;
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

	      /*
              MessageImpl msg = (MessageImpl) getPayload(entry);
        
              if (msg != null)
                msg.setJMSRedelivered(true);
	      */
            }
            
            return;
          }
        }
      }
    }
  }
  
  public ArrayList<String> getMessageIds()
  {
    ArrayList<String> browserList = new ArrayList<String>();

    synchronized (_queueLock) {
      for (int i = 0; i < _head.length; i++) {
	for (QueueEntry entry = _head[i];
	     entry != null;
	     entry = entry._next) {
	  browserList.add(entry.getMsgId());
	}
      }
    }

    return browserList;    
  }
}
