/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.queue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.Alarm;
import com.caucho.util.ThreadPool;

/**
 * Provides abstract implementation for a memory queue.
 * 
 */
public abstract class AbstractMemoryQueue<E extends QueueEntry>
  extends AbstractQueue
{
  private static final Logger log
    = Logger.getLogger(AbstractMemoryQueue.class.getName());
  
  protected final Object _queueLock = new Object();

  private ArrayList<EntryCallback> _callbackList
    = new ArrayList<EntryCallback>();

  protected QueueEntry []_head = new QueueEntry[10];
  protected QueueEntry []_tail = new QueueEntry[10];

  private ThreadPool _threadPool = ThreadPool.getThreadPool();
  
  private final AtomicLong _readSequenceGenerator = new AtomicLong();
  
  // stats
  private AtomicInteger _receiverCount = new AtomicInteger();
  private AtomicInteger _listenerCount = new AtomicInteger();   

  //
  // Abstract/stub methods to be implemented by the Queue
  //
  /**
   * Sends a message to the queue
   */
  @Override
  public final void send(String msgId,
			 Serializable payload,
			 int priority,
			 long expires)
    throws MessageException
  {
    E entry = writeEntry(msgId, payload, priority, expires);
      
    addQueueEntry(entry);
  }

  //
  // send implementation
  //

  abstract protected E writeEntry(String msg,
				  Serializable payload,
				  int priority,
				  long expires);

  protected void addQueueEntry(E entry)
  {
    addEntry(entry);

    dispatchMessage();
  }

  //
  // receive implementation
  //

  /**
   * Primary message receiving, registers a callback for any new
   * message.
   */
  public E receiveEntry(long expireTime, boolean isAutoAck)
    throws MessageException
  {
    _receiverCount.incrementAndGet();
    
    try {
      E entry = null;

      synchronized (_queueLock) {
        if (_callbackList.size() == 0) {
  	entry = readEntry();
        }
      }

      if (entry != null) {
        readPayload(entry);
  
        if (isAutoAck)
  	acknowledge(entry.getMsgId());
          
        return entry;
      }
  
      if (expireTime <= Alarm.getCurrentTime()) {              
        return null;
      }
  
      ReceiveEntryCallback callback = new ReceiveEntryCallback(isAutoAck);

      return (E) callback.waitForEntry(expireTime);      
    } finally {
      _receiverCount.decrementAndGet();  
    }
  }
  
  public EntryCallback addMessageCallback(MessageCallback callback,
					  boolean isAutoAck)
  {
    _listenerCount.incrementAndGet();
    
    ListenEntryCallback entryCallback
      = new ListenEntryCallback(callback, isAutoAck);

    listen(entryCallback);

    return entryCallback;
  }

  public void removeMessageCallback(EntryCallback callback)
  {
    ListenEntryCallback listenerCallback
      = (ListenEntryCallback) callback;

    listenerCallback.close();

    synchronized (_queueLock) {
      _callbackList.remove(listenerCallback);
    }
    
    _listenerCount.decrementAndGet();
  }

  //
  // abstract receive stubs
  //

  protected void acknowledge(E entry)
  {
  }

  protected void readPayload(E entry)
  {
  }
  
  public void removeMessageCallback(MessageCallback callback)
  {
    /*
    synchronized (_callbackList) {
      _callbackList.remove(callback);
    }
    */
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

  public boolean listen(EntryCallback callback)
    throws MessageException
  {
    E entry = null;

    synchronized (_queueLock) {
      if (_callbackList.size() > 0 || (entry = readEntry()) == null) {
	_callbackList.add(callback);
	return false;
      }
    }

    readPayload(entry);

    if (callback.entryReceived(entry)) {
      acknowledge(entry.getMsgId());
    }

    return true;
  }

  protected void dispatchMessage()
  {
    while (true) {
      E entry = null;
      EntryCallback callback = null;
      
      synchronized (_queueLock) {
	if (_callbackList.size() == 0 || (entry = readEntry()) == null) {
	  return;
	}

	callback = _callbackList.remove(0);
      }

      readPayload(entry);

      if (callback.entryReceived(entry)) {
	acknowledge(entry.getMsgId());
      }
    }
  }
  
  //
  // Queue statistics JMX
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
  
  @Override  
  public int getConsumerCount()
  {
    return _listenerCount.get();
  }

  @Override  
  public int getReceiverCount()
  {
    return _receiverCount.get();
  }
  
  //
  // queue management
  //

  /**
   * Add an entry to the queue
   */
  private E addEntry(E entry)
  {
    int priority = entry.getPriority();
    
    synchronized (_queueLock) {
      if (_tail[priority] != null)
        _tail[priority]._next = entry;
      else
        _head[priority] = entry;

      _tail[priority] = entry;

      return entry;
    }
  }
  
  /**
   * Returns the next entry from the queue
   */
  protected E readEntry()
  {
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
          
	entry.setReadSequence(_readSequenceGenerator.incrementAndGet());

	return (E) entry;
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
        QueueEntry entry = _head[i];

	while (entry != null) {
	  QueueEntry next = entry._next;
	  
          if (msgId.equals(entry.getMsgId())) {
	    if (prev != null)
	      prev._next = entry._next;
	    else
	      _head[i] = entry._next;

	    if (_tail[i] == entry)
	      _tail[i] = prev;
	    
	    return (E) entry;
          }

	  prev = entry;
	  entry = next;
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
              entry.setReadSequence(0);

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

  /**
   * Synchronous timeout receive
   */
  class ReceiveEntryCallback implements EntryCallback {
    private boolean _isAutoAck;
    
    private Thread _thread;
    private volatile QueueEntry _entry;

    ReceiveEntryCallback(boolean isAutoAck)
    {
      _isAutoAck = isAutoAck;
      _thread = Thread.currentThread();
    }
    
    public boolean entryReceived(QueueEntry entry)
    {
      _entry = entry;

      LockSupport.unpark(_thread);

      return _isAutoAck;
    }

    public QueueEntry waitForEntry(long expireTime)
    {
      listen(this);
      
      long timeout;
      
      while (_entry == null
	     && (timeout = expireTime - Alarm.getCurrentTime()) > 0) {
        
	LockSupport.parkNanos(timeout * 1000000L);
      }
      
      synchronized (_queueLock) {
        _callbackList.remove(this);
      }
      
      return _entry;
    }
  }

  /**
   * Async listen receive
   */
  class ListenEntryCallback implements EntryCallback, Runnable {
    private boolean _isAutoAck;
    private MessageCallback _callback;
    private ClassLoader _classLoader;

    private boolean _isClosed;
    
    private volatile QueueEntry _entry;

    ListenEntryCallback(MessageCallback callback, boolean isAutoAck)
    {
      _isAutoAck = isAutoAck;
      _callback = callback;
      _classLoader = Thread.currentThread().getContextClassLoader();
    }
    
    public boolean entryReceived(QueueEntry entry)
    {
      _entry = entry;

      _threadPool.schedule(this);

      return false;
    }

    public void run()
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      boolean isValid = false;
      long readSequence = _entry.getReadSequence();
      
      try {
	thread.setContextClassLoader(_classLoader);

	_callback.messageReceived(_entry.getMsgId(), _entry.getPayload());
	isValid = true;
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
	isValid = true;
      } catch (Throwable t) {
        log.log(Level.SEVERE, t.toString(), t);
      } finally {
	thread.setContextClassLoader(oldLoader);
	
	if (readSequence == _entry.getReadSequence()){
	  acknowledge(_entry.getMsgId());
	}
      }

      if (! _isClosed && isValid)
	listen(this);
    }

    public void close()
    {
      _isClosed = true;
    }
  }
}
