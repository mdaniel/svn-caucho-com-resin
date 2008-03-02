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

package com.caucho.jms.xmpp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.memory.*;
import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.connection.*;

/**
 * Implements an xmpp topic.
 */
public class XmppPubSubLeaf
{
  private static final Logger log
    = Logger.getLogger(XmppPubSubLeaf.class.getName());

  private String _name;

  private ArrayList<BlockingQueue> _queueList
    = new ArrayList<BlockingQueue>();
  
  public XmppPubSubLeaf(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void addQueue(BlockingQueue queue)
  {
    synchronized (_queueList) {
      if (! _queueList.contains(queue))
	_queueList.add(queue);
    }
  }

  public void removeQueue(BlockingQueue queue)
  {
    synchronized (_queueList) {
      _queueList.remove(queue);
    }
  }

  public void send(Message msg)
  {
    synchronized (_queueList) {
      for (int i = 0; i < _queueList.size(); i++) {
	BlockingQueue queue = (BlockingQueue) _queueList.get(i);

	queue.offer(msg);
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}

