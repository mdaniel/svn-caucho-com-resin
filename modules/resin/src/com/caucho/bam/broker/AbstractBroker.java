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

package com.caucho.bam.broker;

import com.caucho.bam.mailbox.Mailbox;
import com.caucho.cloud.bam.BamQueueFullHandler;


/**
 * Broker is the hub which routes messages to actors.
 */
abstract public class AbstractBroker 
  extends AbstractBrokerStream
  implements Broker
{
  private BamQueueFullHandler _queueFullHandler = BamQueueFullHandler.DEFAULT;
  
  /**
   * Returns the broker's address, i.e. the virtual host domain name.
   */
  @Override
  public String getAddress()
  {
    return getClass().getSimpleName() + ".localhost";
  }
  
  /**
   * The broker returns itself for the broker.
   */
  @Override
  public Broker getBroker()
  {
    return this;
  }
  
  public BamQueueFullHandler getQueueFullHandler()
  {
    return _queueFullHandler;
  }
  
  public void setQueueFullHandler(BamQueueFullHandler handler)
  {
    if (handler == null) {
      throw new NullPointerException();
    }
    
    _queueFullHandler = handler;;
  }
  
  /**
   * Returns a mailbox to the broker itself
   */
  @Override
  public Mailbox getBrokerMailbox()
  {
    throw new UnsupportedOperationException(getClass().getName());
    // return new NullActorStream(getAddress(), this);
  }
  
  @Override
  public Mailbox getMailbox(String address)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public void addMailbox(String address, Mailbox mailbox)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public void removeMailbox(Mailbox mailbox)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if the broker has been closed
   */
  @Override
  public boolean isClosed()
  {
    return false;
  }
  
  @Override
  public void close()
  {
  }
}
