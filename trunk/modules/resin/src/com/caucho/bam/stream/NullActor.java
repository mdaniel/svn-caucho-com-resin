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

package com.caucho.bam.stream;

import com.caucho.bam.actor.Actor;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;


/**
 * NullActorStream always ignores messages and returns errors for RPC calls.
 */
public class NullActor extends NullMessageStream implements Actor, ActorHolder
{
  private Mailbox _mailbox;
  
  public NullActor()
  {
    this("null");
  }
  
  public NullActor(String address)
  {
    super();
    
    setAddress(address);
  }
  
  public NullActor(String address, Broker broker)
  {
    super();
    
    setAddress(address);
    setBroker(broker);
  }

  @Override
  public void setAddress(String address)
  {
    super.setAddress(address);
  }
  
  @Override
  public void setBroker(Broker broker)
  {
    super.setBroker(broker);
  }

  @Override
  public Mailbox getMailbox()
  {
    return _mailbox;
  }

  @Override
  public void setMailbox(Mailbox mailbox)
  {
    _mailbox = mailbox;
  }

  @Override
  public Actor getActor()
  {
    return this;
  }
}
