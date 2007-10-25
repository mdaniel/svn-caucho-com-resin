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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.memory;

import java.util.ArrayList;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.listener.*;
import com.caucho.jms.queue.*;

import com.caucho.util.Alarm;

/**
 * Implements a memory queue.
 */
public class MemoryQueue extends AbstractQueue
{
  private static final Logger log
    = Logger.getLogger(MemoryQueue.class.getName());

  private ArrayList<MessageImpl> _queueList = new ArrayList<MessageImpl>();

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   */
  @Override
  protected void enqueue(MessageImpl msg, long expires)
  {
    synchronized (_queueList) {
      _queueList.add(msg);
      _queueList.notifyAll();
    }
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  @Override
  public MessageImpl receive(long expires)
  {
    while (true) {
      synchronized (_queueList) {
        if (_queueList.size() > 0)
          return _queueList.remove(0);

        long now = Alarm.getCurrentTime();

        if (expires <= now || Alarm.isTest()) {
          return null;
        }

        try {
          _queueList.wait(expires - now);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  public String toString()
  {
    return "MemoryQueue[" + getName() + "]";
  }
}

