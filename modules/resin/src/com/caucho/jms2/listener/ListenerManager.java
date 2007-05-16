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

package com.caucho.jms2.listener;

import java.util.*;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms2.message.*;
import com.caucho.util.ThreadPool;

/**
 * Manages the listeners for a queue.
 */
public class ListenerManager
{
  private static final Logger log
    = Logger.getLogger(ListenerManager.class.getName());

  private ListenerEntry []_idleStack = new ListenerEntry[0];
  private int _idleTop;

  public void addListener(MessageListener listener)
  {
    synchronized (this) {
      ListenerEntry []newIdle = new ListenerEntry[_idleStack.length + 1];

      System.arraycopy(_idleStack, 0, newIdle, 0, _idleStack.length);

      _idleStack = newIdle;

      _idleStack[_idleTop++] = new ListenerEntry(listener);
    }
  }

  public boolean hasIdle()
  {
    return _idleTop > 0;
  }

  public SendStatus send(MessageImpl msg)
  {
    ListenerEntry entry = null;
    
    synchronized (this) {
      if (_idleTop > 0) {
	entry = _idleStack[--_idleTop];
      }
    }

    if (entry != null) {
      entry.send(msg);
      return SendStatus.OK;
    }
    else
      return SendStatus.FAIL;
  }

  void toIdle(ListenerEntry entry)
  {
    synchronized (this) {
      _idleStack[_idleTop++] = entry;
    }
  }

  public void close()
  {
  }

  class ListenerEntry implements Runnable {
    private MessageListener _listener;
    
    private Message _msg;

    ListenerEntry(MessageListener listener)
    {
      _listener = listener;
    }

    /**
     * Queue the message for sending.
     */
    void send(MessageImpl msg)
    {
      _msg = msg;

      ThreadPool.getThreadPool().schedule(this);
    }

    /**
     * Runnable to process the message.
     */
    public void run()
    {
      try {
	Message msg = _msg;
	_msg = null;
	
	_listener.onMessage(msg);
      } finally {
	toIdle(this);
      }
    }
  }
}

