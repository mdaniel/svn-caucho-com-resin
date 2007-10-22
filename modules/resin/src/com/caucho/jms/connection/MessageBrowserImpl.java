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

package com.caucho.jms.connection;

import com.caucho.jms2.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;
import com.caucho.log.Log;
import com.caucho.util.*;

import javax.jms.*;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A basic message consumer.
 */
public class MessageBrowserImpl
  implements QueueBrowser
{
  static final Logger log
    = Logger.getLogger(MessageBrowserImpl.class.getName());
  static final L10N L = new L10N(MessageBrowserImpl.class);

  private AbstractQueue _queue;
  private String _messageSelector;

  public MessageBrowserImpl(AbstractQueue queue, String messageSelector)
  {
    _queue = queue;
  }
  
  public Queue getQueue()
    throws JMSException
  {
    return _queue;
  }
  
  public String getMessageSelector()
    throws JMSException
  {
    return _messageSelector;
  }

  public Enumeration getEnumeration()
    throws JMSException
  {
    return NullEnumeration.create();
  }

  public void close()
    throws JMSException
  {
  }

  public String toString()
  {
    return "MessageBrowserImpl[" + _queue + "]";
  }
}

