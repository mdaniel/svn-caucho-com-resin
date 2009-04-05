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
import com.caucho.jms.queue.MessageException;
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
public class FileQueueImpl extends AbstractMemoryQueue<FileQueueEntry>
  implements Topic
{
  private static final Logger log
    = Logger.getLogger(FileQueueImpl.class.getName());

  private final FileQueueStore _store;

  private byte []_queueIdHash;

  private volatile boolean _isStartComplete;

  private Object _dispatchLock = new Object();

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
   * Backward compatibility
   */
  @Deprecated
  public void setPath(Path path)
  {
  }

  public Path getPath()
  {
    return null;
  }

  /**
   * Backward compatibility
   */
  @Deprecated
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

      _queueIdHash = digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
      
    _isStartComplete = _store.receiveStart(_queueIdHash, this);
  }
  
  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   *
   * @param msgId the queue's unique identifier for the message
   * @param payload the message payload to store
   * @param priority the message priority
   * @param expireTime the expires time
   */
  @Override
  public FileQueueEntry writeEntry(String msgId,
				   Serializable payload,
				   int priority,
				   long expireTime)
  {
    long id = _store.send(_queueIdHash, msgId, payload, priority, expireTime);

    long leaseTimeout = -1;

    FileQueueEntry entry = new FileQueueEntry(id, msgId, leaseTimeout,
					      priority, expireTime, payload);

    return entry;
  }

  protected void readPayload(FileQueueEntry entry)
  {
    Serializable payload = entry.getPayload();

    if (payload == null) {
      payload = entry.getPayloadRef();

      if (payload == null)
	payload = _store.readMessage(entry.getId());
      
      entry.setPayload(payload);
    }
  }

  @Override
  protected void acknowledge(FileQueueEntry entry)
  {
    _store.delete(entry.getId());
  }

  /**
   * Callback from startup
   */
  protected void addEntry(long id, String msgId, long leaseTimeout,
			  int priority, long expireTime,
			  Serializable payload)
  {
    FileQueueEntry entry = new FileQueueEntry(id, msgId, leaseTimeout,
					      priority, expireTime, payload);

    addQueueEntry(entry);
  }
}

