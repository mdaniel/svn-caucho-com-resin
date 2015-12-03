/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.amp.impl;

import java.util.concurrent.ConcurrentHashMap;

import com.caucho.amp.actor.ActorRefImpl;
import com.caucho.amp.actor.AmpActorRef;
import com.caucho.amp.actor.NullActorRef;
import com.caucho.amp.broker.AbstractAmpBroker;
import com.caucho.amp.mailbox.AmpMailbox;

/**
 * AmpRouter routes messages to mailboxes.
 */
public class HashMapAmpBroker extends AbstractAmpBroker
{
  private final ConcurrentHashMap<String,AmpMailbox> _mailboxMap
    = new ConcurrentHashMap<String,AmpMailbox>();

  @Override
  public AmpActorRef getActorRef(String address)
  {
    AmpMailbox mailbox = getMailbox(address);
    
    if (mailbox != null) {
      return new ActorRefImpl(address, mailbox, mailbox.getActorContext());
    }
    else {
      return new NullActorRef(address);
    }
  }

  @Override
  public AmpActorRef addMailbox(String address, AmpMailbox mailbox)
  {
    _mailboxMap.put(address, mailbox);
    
    return new ActorRefImpl(address, mailbox, mailbox.getActorContext());
  }
  
  protected AmpMailbox getMailbox(String address)
  {
    return _mailboxMap.get(address);
  }
}
