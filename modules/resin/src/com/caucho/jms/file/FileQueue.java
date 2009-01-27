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

package com.caucho.jms.file;

import java.io.*;
import java.util.logging.*;
import java.sql.*;

import javax.jms.*;
import javax.annotation.*;

import com.caucho.jms.queue.*;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.config.ConfigException;
import com.caucho.db.*;
import com.caucho.util.L10N;
import com.caucho.java.*;
import com.caucho.vfs.*;

/**
 * A JMS queue backed by a file-based database.
 *
 * The URL looks like
 * <pre>
 * file:name=my-name;path=file:/var/www/webapps/test/WEB-INF/jms
 * </pre>
 *
 * It is configured as:
 * <pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *             xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:FileQueue>
 *     &lt;resin:JndiName>jms/my-name&lt;/resin:JndiName>
 *
 *     &lt;resin:name>my-name&lt;/resin:name>
 *     &lt;resin:path>WEB-INF/jms&lt;/resin:path>
 *   &lt;/resin:FileQueue>
 *
 * &lt;/web-app>
 * </pre>
 */
public class FileQueue extends AbstractQueue implements Topic
{
  private static final L10N L = new L10N(FileQueue.class);
  private static final Logger log
    = Logger.getLogger(FileQueue.class.getName());

  private final FileQueueStore _store;

  private final Object _queueLock = new Object();

  private FileQueueEntry []_head = new FileQueueEntry[10];
  private FileQueueEntry []_tail = new FileQueueEntry[10];

  public FileQueue()
  {
    _store = new FileQueueStore(_messageFactory);
  }

  public FileQueue(String name)
  {
    this();
    
    setName(name);

    init();
  }

  //
  // Configuration
  //

  /**
   * Sets the path to the backing database
   */
  public void setPath(Path path)
  {
    _store.setPath(path);
  }

  public Path getPath()
  {
    return _store.getPath();
  }

  public void setTablePrefix(String prefix)
  {
    _store.setTablePrefix(prefix);
  }

  //
  // JMX configuration items
  //

  /**
   * Returns the JMS configuration url.
   */
  public String getUrl()
  {
    return "file:name=" + getName() + ";path=" + _store.getPath().getURL();
  }

  //
  // JMX stats
  //

  public int getQueueSize()
  {
    int count = 0;

    for (int i = 0; i < _head.length; i++ ){
      for (FileQueueEntry entry = _head[i];
	   entry != null;
	   entry = entry._next) {
	count++;
      }
    }

    return count;
  }

  /**
   * Initialize the queue
   */
  @PostConstruct
  public void init()
  {
    System.out.println("INIT: " + getName());
    
    _store.setName(getName());

    _store.init();

    _store.receiveStart(this);
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   *
   * @param msg the message to store
   * @param expires the expires time
   */
  @Override
  public void send(JmsSession session,
		   MessageImpl msg,
		   int priority,
		   long expires)
  {
    System.out.println("SEND: " + msg);
    
    synchronized (_queueLock) {
      long id = _store.send(msg, priority, expires);

      FileQueueEntry entry = addEntry(id, msg.getJMSMessageID(),
				      -1, priority, expires, null);

      entry.setMessage(msg);
    }

    notifyMessageAvailable();
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   *
   * @param msg the message to store
   * @param expires the expires time
   */
  public void sendBackup(JmsSession session,
			 MessageImpl msg,
			 long leaseTimeout,
			 int priority,
			 long expires)
  {
    synchronized (_queueLock) {
      long id = _store.send(msg, priority, expires);

      FileQueueEntry entry
	= addEntry(id, msg.getJMSMessageID(), leaseTimeout,
		   priority, expires, null);

      entry.setMessage(msg);
    }
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  @Override
  public MessageImpl receive(boolean isAutoAck)
  {
	  System.out.println("RECEIVE: " + isAutoAck);
	  
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
	for (FileQueueEntry entry = _head[i];
	     entry != null;
	     entry = entry._next) {
	  if (! entry.isLease())
	    continue;
	  
	  if (! entry.isRead()) {
	    entry.setRead(true);

	    MessageImpl msg = entry.getMessage();

	    if (msg == null) {
	      msg = _store.readMessage(entry.getId(), entry.getType());
	      entry.setMessage(msg);
	    }

	    if (log.isLoggable(Level.FINER))
	      log.finer(this + " receive " + msg + " auto-ack=" + isAutoAck);

	    if (isAutoAck) {
	      removeEntry(entry);
	      _store.delete(entry.getId());
	    }

	    return msg;
	  }
	}
      }

      return null;
    }
  }

  /**
   * Rollsback the message from the store.
   */
  @Override
  public void rollback(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
	for (FileQueueEntry entry = _head[i];
	     entry != null;
	     entry = entry._next) {
	  if (msgId.equals(entry.getMsgId())) {
	    if (entry.isRead()) {
	      entry.setRead(false);

	      MessageImpl msg = entry.getMessage();
        
	      if (msg != null)
		msg.setJMSRedelivered(true);
	    }
	    
	    return;
	  }
	}
      }
    }
  }

  /**
   * Rollsback the message from the store.
   */
  @Override
  public void acknowledge(String msgId)
  {
    synchronized (_queueLock) {
      for (int i = _head.length - 1; i >= 0; i--) {
	for (FileQueueEntry entry = _head[i];
	     entry != null;
	     entry = entry._next) {
	  if (msgId.equals(entry.getMsgId())) {
	    if (entry.isRead()) {
	      removeEntry(entry);
	      _store.delete(entry.getId());
	    }
	      
	    return;
	  }
	}
      }
    }
  }
  
  /**
   * Adds a message entry from startup.
   */
  FileQueueEntry addEntry(long id,
			  String msgId,
			  long leaseTimeout,
			  int priority,
			  long expire,
			  MessageType type)
  {
    if (priority < 0)
      priority = 0;
    else if (_head.length <= priority)
      priority = _head.length;

    System.out.println("ADD: " + id + " " + msgId);
      
    FileQueueEntry entry
      = new FileQueueEntry(id, msgId, leaseTimeout, priority, expire, type);

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
   * Rollsback the message from the store.
   */
  public void removeMessage(String msgId)
  {
    if (removeMessageEntry(msgId)) {
      _store.remove(msgId);
    }
  }

  /**
   * Rollsback the message from the store.
   */
  private boolean removeMessageEntry(String msgId)
  {
    synchronized (_queueLock) {
      loop:
      for (int i = _head.length - 1; i >= 0; i--) {
	for (FileQueueEntry entry = _head[i];
	     entry != null;
	     entry = entry._next) {
	  if (msgId.equals(entry.getMsgId())) {
	    if (log.isLoggable(Level.FINER))
	      log.finer(this + " remove " + msgId);
	    
	    removeEntry(entry);

	    return true;
	  }
	}
      }
    }

    return false;
  }

  private void removeEntry(FileQueueEntry entry)
  {
    int priority = entry.getPriority();
    FileQueueEntry prev = entry._prev;
    FileQueueEntry next = entry._next;
    
    if (prev != null)
      prev._next = next;
    else
      _head[priority] = next;

    if (next != null)
      next._prev = prev;
    else
      _tail[priority] = prev;
  }
}

