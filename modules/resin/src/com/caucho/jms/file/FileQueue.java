/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
import com.caucho.config.ConfigException;
import com.caucho.db.*;
import com.caucho.util.L10N;
import com.caucho.java.*;
import com.caucho.vfs.*;

/**
 * Implements a file queue.
 */
public class FileQueue extends AbstractQueue implements Topic
{
  private static final L10N L = new L10N(FileQueue.class);
  private static final Logger log
    = Logger.getLogger(FileQueue.class.getName());

  private final FileQueueStore _store;

  private final Object _queueLock = new Object();

  private FileQueueEntry _head;
  private FileQueueEntry _tail;

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

  public void setPath(Path path)
  {
    _store.setPath(path);
  }

  public void setTablePrefix(String prefix)
  {
    _store.setTablePrefix(prefix);
  }

  @PostConstruct
  public void init()
  {
    _store.setName(getName());

    _store.init();

    _store.receiveStart(this);
  }

  /**
   * Adds a message entry from startup.
   */
  FileQueueEntry addEntry(long id, long expire, MessageType type)
  {
    synchronized (_queueLock) {
      FileQueueEntry entry = new FileQueueEntry(id, expire, type);
      
      entry._prev = _tail;

      if (_tail != null)
	_tail._next = entry;
      else
	_head = entry;

      _tail = entry;

      return entry;
    }
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   *
   * @param msg the message to store
   * @param expires the expires time
   */
  @Override
  protected void enqueue(MessageImpl msg, long expires)
  {
    synchronized (_queueLock) {
      long id = _store.send(msg, expires);

      FileQueueEntry entry = addEntry(id, expires, null);

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
    synchronized (_queueLock) {
      for (FileQueueEntry entry = _head;
	   entry != null;
	   entry = entry._next) {
	if (! entry.isRead()) {
	  entry.setRead(true);

	  MessageImpl msg = entry.getMessage();

	  if (msg == null) {
	    msg = _store.readMessage(entry.getId(), entry.getType());
	    entry.setMessage(msg);
	  }

	  if (isAutoAck) {
	    removeEntry(entry);
	    _store.delete(entry.getId());
	  }

	  return msg;
	}
      }

      return null;
    }
  }

  /**
   * Rollsback the message from the store.
   */
  @Override
  public void rollback(MessageImpl msg)
  {
    synchronized (_queueLock) {
      for (FileQueueEntry entry = _head;
	   entry != null;
	   entry = entry._next) {
	if (entry.getMessage() == msg && entry.isRead()) {
	  entry.setRead(false);
	  msg.setJMSRedelivered(true);
	  return;
	}
      }
    }
  }

  /**
   * Rollsback the message from the store.
   */
  @Override
  public void acknowledge(MessageImpl msg)
  {
    synchronized (_queueLock) {
      for (FileQueueEntry entry = _head;
	   entry != null;
	   entry = entry._next) {
	if (entry.getMessage() == msg && entry.isRead()) {
	  removeEntry(entry);
	  _store.delete(entry.getId());
	  return;
	}
      }
    }
  }

  private void removeEntry(FileQueueEntry entry)
  {
    FileQueueEntry prev = entry._prev;
    FileQueueEntry next = entry._next;
    
    if (prev != null)
      prev._next = next;
    else
      _head = next;

    if (next != null)
      next._prev = prev;
    else
      _tail = prev;
  }
}

