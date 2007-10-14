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

package com.caucho.jms2.queue;

import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms2.message.*;
import com.caucho.jms2.listener.*;
import com.caucho.jms2.connection.*;

import com.caucho.util.*;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue extends AbstractDestination
  implements javax.jms.Queue
{
  private static final L10N L = new L10N(AbstractQueue.class);
  private static final Logger log
    = Logger.getLogger(AbstractQueue.class.getName());

  private ListenerManager _listenerManager;
  private int _enqueueCount;

  protected AbstractQueue()
  {
    _listenerManager = new ListenerManager(this);
  }
  
  public void addListener(MessageListener listener)
  {
    _listenerManager.addListener(listener);
  }

  public boolean hasListener()
  {
    return _listenerManager.hasListener();
  }

  public void removeListener(MessageListener listener)
  {
    _listenerManager.removeListener(listener);
  }

  @Override
  public void send(SessionImpl session, Message msg, long timeout)
    throws JMSException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(L.l("{0}: sending message {1}", this, msg));
    
    long expires = Alarm.getCurrentTime() + timeout;
    
    MessageImpl queueMsg = _messageFactory.copy(msg);

    SendStatus status;

    ListenerManager listenerManager = _listenerManager;

    try {
      synchronized (listenerManager) {
        _enqueueCount++;
        
        if (_enqueueCount > 1)
          status = SendStatus.FAIL;
        else
          status = listenerManager.send(queueMsg);
      }

      if (status == SendStatus.FAIL) {
        enqueue(queueMsg, timeout);
      
        synchronized (listenerManager) {
          if (listenerManager.hasIdle()) {
            MessageImpl oldMsg = receive(0);

            if (oldMsg != null)
              listenerManager.send(oldMsg);
          }
        }
      }
    } finally {
      synchronized (listenerManager) {
        _enqueueCount--;
      }
    }
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   */
  protected void enqueue(MessageImpl msg, long expires)
    throws JMSException
  {
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  @Override
  public MessageImpl receive(long timeout)
    throws JMSException
  {
    return null;
  }

  public void close()
  {
    super.close();
    
    ListenerManager listenerManager = _listenerManager;
    _listenerManager = null;

    if (listenerManager != null)
      listenerManager.close();
  }
  
  /**
   * Creates a QueueBrowser to browse messages in the queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueBrowser createBrowser(SessionImpl session,
				    String messageSelector)
    throws JMSException
  {
    return new MessageBrowserImpl(this, messageSelector);
  }

  public String toString()
  {
    String className = getClass().getName();

    int p = className.lastIndexOf('.');
    
    return className.substring(p + 1) + "[" + getName() + "]";
  }
}

