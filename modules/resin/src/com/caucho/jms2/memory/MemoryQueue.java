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

package com.caucho.jms2.memory;

import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms2.message.*;
import com.caucho.jms2.listener.*;
import com.caucho.jms2.queue.*;

/**
 * Implements a memory queue.
 */
public class MemoryQueue extends AbstractQueue
{
  private static final Logger log
    = Logger.getLogger(MemoryQueue.class.getName());

  private String _name = "default";

  private MessageFactory _messageFactory = new MessageFactory();
  private ListenerManager _listenerManager = new ListenerManager();

  public String getName()
  {
    return _name;
  }
  /*
  public TextMessage createTextMessage()
  {
    return _messageFactory.createTextMessage();
  }
  */

  public TextMessage createTextMessage(String msg)
    throws JMSException
  {
    return _messageFactory.createTextMessage(msg);
  }

  public void addListener(MessageListener listener)
  {
    _listenerManager.addListener(listener);
  }

  public void removeListener(MessageListener listener)
  {
    _listenerManager.removeListener(listener);
  }

  public boolean hasListener()
  {
    return _listenerManager.hasListener();
  }

  public void send(Message msg, long timeout)
    throws JMSException
  {
    MessageImpl msgCopy = _messageFactory.copy(msg);
    
    _listenerManager.send(msgCopy);
  }

  public String toString()
  {
    return "MemoryQueue[" + getName() + "]";
  }
}

