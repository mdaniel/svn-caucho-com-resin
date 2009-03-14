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

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Topic;

import com.caucho.jms.connection.JmsSession;
import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.queue.AbstractMemoryQueue;
import com.caucho.jms.queue.QueueEntry;
import com.caucho.loader.Environment;
import com.caucho.server.cluster.Server;
import com.caucho.vfs.Path;

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
public class FileQueueImpl extends AbstractMemoryQueue implements Topic
{
  private static final Logger log
    = Logger.getLogger(FileQueueImpl.class.getName());

  private final FileQueueStore _store;

  private byte []_hash;

  private volatile boolean _isStartComplete;

  public FileQueueImpl()
  {
    _store = FileQueueStore.create();
  }

  public FileQueueImpl(String name)
  {
    this();
    
    setName(name);

    init();
  }

  FileQueueImpl(Path path, String name, String serverId)
  {
    try {
      path.mkdirs();
    } catch (Exception e) {
    }

    if (serverId == null)
      serverId = "anon";
    
    _store = new FileQueueStore(path, serverId);

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
  }

  public Path getPath()
  {
    return null;
  }

  public void setTablePrefix(String prefix)
  {
    
  }

  /**
   * Returns the JMS configuration url.
   */
  public String getUrl()
  {
    return "file:name=" + getName();
  }

  /**
   * Initialize the queue
   */
  public void init()
  {
    try {
      // calculate a unique hash for the queue
      
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      String env = Environment.getEnvironmentName();

      digest.update(env.getBytes());
      if (Server.getCurrent() != null)
	digest.update(Server.getCurrent().getServerId().getBytes());
      digest.update(getClass().getSimpleName().getBytes());
      digest.update(getName().getBytes());

      _hash = digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
      
    _isStartComplete = _store.receiveStart(_hash, this);
  }

  /**
   * 
   * @param entry
   * @return        Payload associated with the entry.
   */
  protected MessageImpl getPayload(QueueEntry entry) 
  { 
    if (entry == null) {
      return null;
    }

    MessageImpl msg = (MessageImpl) entry.getPayload();

    // Read it from the backed-up medium.
    if (msg == null) {
      msg = (MessageImpl) _store.readMessage(((FileQueueEntry)entry).getId());
      entry.setPayload(msg);
    }
    return msg;
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
    long id = _store.send(_hash, msg.getJMSMessageID(),
			  msg, priority, expires);

    addEntry(id, msg.getJMSMessageID(), -1, priority, expires, msg);

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
    long id = _store.send(_hash, msg.getJMSMessageID(), msg,
			  priority, expires);

    addEntry(id, msg.getJMSMessageID(), leaseTimeout,
	     priority, expires, msg);
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  @Override
  public MessageImpl receive(boolean isAutoAck)
  {
    FileQueueEntry entry = (FileQueueEntry)receiveImpl(isAutoAck);

    if (entry != null) {
      try {
	MessageImpl msg = (MessageImpl) entry.getPayload();

	if (msg == null) {
	  msg = (MessageImpl) _store.readMessage(entry.getId());
	  entry.setPayload(msg);
	}
	
	msg.setReceive();

	if (log.isLoggable(Level.FINER))
	  log.finer(this + " receive " + msg + " auto-ack=" + isAutoAck);

	if (isAutoAck || msg == null) {
	  synchronized (_queueLock) {
	    removeEntry(entry);
	  }
	
	  _store.delete(entry.getId());
	}

	if (msg != null)
	  return msg;
      } catch (Exception e) {
	e.printStackTrace();
      }
    }

    // System.out.println("ENTRY: " + isEntryAvailable + " " + _isStartComplete);
    if (! _isStartComplete) {
      synchronized (this) {
	if (! _isStartComplete)
	  _isStartComplete = _store.receiveStart(_hash, this);
      }

      return receive(isAutoAck);
    }

    return null;
  }

  /**
   * Acknowledges the receipt of a message.
   * Removes the message from the store.
   */
  @Override
  public void acknowledge(String msgId)
  {
    FileQueueEntry entry = (FileQueueEntry)acknowledgeImpl(msgId);

    if (entry != null)
      _store.delete(entry.getId());
  }
  
  /**
   * Adds a message entry from startup.
   */
  FileQueueEntry addEntry(long id,
			  String msgId,
			  long leaseTimeout,
			  int priority,
			  long expire,
			  Serializable payload)
  {
    if (priority < 0)
      priority = 0;
    else if (_head.length <= priority)
      priority = _head.length;

    FileQueueEntry entry
      = new FileQueueEntry(id, msgId, leaseTimeout,
			   priority, expire, payload);

    return (FileQueueEntry)addEntry(entry);
  }
}

