/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.Iterator;
import java.util.Enumeration;
import java.util.Collections;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.JMSException;

import com.caucho.util.L10N;
import com.caucho.util.NullEnumeration;

import com.caucho.log.Log;

import com.caucho.jms.AbstractDestination;

import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;

import com.caucho.jms.session.SessionImpl;

/**
 * A basic queue.
 */
public class MemoryQueueBrowser implements QueueBrowser  {
  static final Logger log = Log.open(MemoryQueueBrowser.class);
  static final L10N L = new L10N(MemoryQueueBrowser.class);

  private SessionImpl _session;
  protected MemoryQueue _queue;
  private String _messageSelector;
  private Selector _selector;
  
  MemoryQueueBrowser(SessionImpl session,
		     MemoryQueue queue,
		     String messageSelector)
    throws JMSException
  {
    _session = session;
    _queue = queue;
    _messageSelector = messageSelector;
    if (_messageSelector != null) {
      SelectorParser parser = new SelectorParser();
      _selector = parser.parse(messageSelector);
    }
  }

  /**
   * Returns the browser's queue.
   */
  public Queue getQueue()
    throws JMSException
  {
    return (Queue) _queue;
  }

  /**
   * Returns the message selector.
   */
  public String getMessageSelector()
    throws JMSException
  {
    return _messageSelector;
  }

  /**
   * Returns an enumeration of the matching messages.
   */
  public Enumeration getEnumeration()
    throws JMSException
  {
    if (_session.isActive())
      return _queue.getEnumeration(_selector);
    else
      return NullEnumeration.create();
  }

  public void close()
    throws JMSException
  {
  }
}

