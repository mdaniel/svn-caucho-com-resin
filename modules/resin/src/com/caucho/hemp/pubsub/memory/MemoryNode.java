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

package com.caucho.hemp.pubsub.memory;

import com.caucho.xmpp.pubsub.PublishMessage;
import com.caucho.xmpp.pubsub.PubSubItem;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;

/**
 * pub/sub (xep-0060)
 * http://www.xmpp.org/extensions/xep-0060.html
 */
public class MemoryNode
{
  private static final Logger log
    = Logger.getLogger(MemoryNode.class.getName());

  private String _name;

  private MemoryPubSub _service;

  private ArrayList<String> _ownerList
    = new ArrayList<String>();

  private ArrayList<String> _subscriberList
    = new ArrayList<String>();

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public void setService(MemoryPubSub service)
  {
    _service = service;
  }

  public void addOwner(String owner)
  {
    _ownerList.add(owner);

    addSubscriber(owner);
  }

  public void addSubscriber(String subscriber)
  {
    _subscriberList.add(subscriber);
  }

  public ArrayList<String> getSubscribers()
  {
    return _subscriberList;
  }

  public void publish(PubSubItem []publishItems)
  {
    PubSubItem []items = new PubSubItem[publishItems.length];
    
    for (int i = 0; i < publishItems.length; i++) {
      PubSubItem item = publishItems[i];
      
      String id = item.getId();
      
      if (id == null)
	id = generateId();

      items[i] = new PubSubItem(id, item.getValue());
    }

    PublishMessage msg = new PublishMessage(getName(), items);

    for (String jid : _subscriberList) {
      _service.getBrokerStream().message(jid, _service.getJid(), msg);
    }
  }

  private String generateId()
  {
    CharBuffer cb = new CharBuffer();
    
    Base64.encode(cb, RandomUtil.getRandomLong());
    Base64.encode(cb, RandomUtil.getRandomLong());
    
    return cb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
