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

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.mailbox.Mailbox;

/**
 * Broker is the hub which routes messages to actors.
 */
public class ManagedBrokerAdapter extends AbstractManagedBroker
{
  private final Broker _broker;
  
  public ManagedBrokerAdapter(Broker broker)
  {
    _broker = broker;
  }
  
  public static ManagedBroker create(Broker broker)
  {
    if (broker instanceof ManagedBroker)
      return (ManagedBroker) broker;
    else
      return new ManagedBrokerAdapter(broker);
  }

  @Override
  public String getAddress()
  {
    return _broker.getAddress();
  }

  @Override
  public boolean isClosed()
  {
    return _broker.isClosed();
  }

  @Override
  public Broker getBroker()
  {
    return this;
  }

  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    _broker.message(to, from, payload);
  }

  @Override
  public void messageError(String to,
                           String from, 
                           Serializable payload,
                           BamError error)
  {
    _broker.messageError(to, from, payload, error);
    
  }

  @Override
  public void query(long id, String to, String from, Serializable payload)
  {
    _broker.query(id, to, from, payload);
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable payload)
  {
    _broker.query(id,  to, from, payload);
  }

  @Override
  public void queryError(long id, 
                         String to, 
                         String from, 
                         Serializable payload,
                         BamError error)
  {
    _broker.queryError(id, to, from, payload, error);
  }

  @Override
  public Mailbox getBrokerMailbox()
  {
    return _broker.getBrokerMailbox();
  }

  @Override
  public Mailbox getMailbox(String address)
  {
    return _broker.getMailbox(address);
  }

  @Override
  public void addMailbox(String address, Mailbox mailbox)
  {
    _broker.addMailbox(address, mailbox);
  }

  @Override
  public void removeMailbox(Mailbox mailbox)
  {
    _broker.removeMailbox(mailbox);
  }

  @Override
  public void close()
  {
  }
}
