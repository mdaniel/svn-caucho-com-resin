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

package com.caucho.bam.actor;

import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.MessageStream;


/**
 * Abstract implementation of a BAM actor.
 */
abstract public class AbstractActorHolder implements ActorHolder
{
  private Broker _broker;
  private Mailbox _mailbox;

  /**
   * Returns the stream to the Actor from the link so
   * messages from other Actors can be delivered.
   */
  @Override
  abstract public Actor getActor();

  @Override
  public String getAddress()
  {
    return getActor().getAddress();
  }
  
  @Override
  public void setAddress(String address)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  @Override
  public Broker getBroker()
  {
    return _broker;
  }

  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  @Override
  public void setBroker(Broker broker)
  {
    _broker = broker;
  }

  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  @Override
  public Mailbox getMailbox()
  {
    return _mailbox;
  }

  /**
   * The stream to the link is used by the Actor to send messages to
   * all other Actors in the system.
   */
  @Override
  public void setMailbox(Mailbox mailbox)
  {
    _mailbox = mailbox;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getActor().getAddress() + "]";
  }
}
