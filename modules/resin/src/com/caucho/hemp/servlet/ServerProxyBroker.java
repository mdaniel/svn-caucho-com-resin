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

package com.caucho.hemp.servlet;

import java.io.Serializable;

import com.caucho.bam.BamError;
import com.caucho.bam.broker.AbstractBroker;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.stream.MessageStream;


/**
 * Handles the requests to the server from the link, dispatching requests to
 * the link service and the broker.
 */
public class ServerProxyBroker extends AbstractBroker {
  private final Broker _broker;
  private final ClientStubManager _clientManager;
  private final Mailbox _toLinkMailbox;
  private final MessageStream _linkActor;

  public ServerProxyBroker(Broker broker,
                           ClientStubManager clientManager,
                           MessageStream linkActor)
  {
    _broker = broker;
    _clientManager = clientManager;
    _linkActor = linkActor;
    _toLinkMailbox = _clientManager.getToLinkMailbox();
  }
  
  private Mailbox getLinkMailbox()
  {
    return _toLinkMailbox;
  }

  @Override
  public String getAddress()
  {
    return null;
  }
  
  private boolean isActive()
  {
    return _clientManager.isActive();
  }
  
  public String getClientAddress()
  {
    return _clientManager.getAddress();
  }
  
  @Override
  public Mailbox getMailbox(String address)
  {
    return null;
  }

  /**
   * Sends a message to the link service if 'to' is null, else send it to the broker.
   */
  @Override
  public void message(String to,
                      String from,
                      Serializable payload)
  {
    try {
      if (to == null)
        _linkActor.message(to, from, payload);
      else if (isActive())
        _broker.message(to, getClientAddress(), payload);
      else
        super.message(to, from, payload);
    } catch (Throwable e) {
      getLinkMailbox().messageError(from, to, payload, BamError.create(e));
    }
  }

  /**
   * Handles a message
   */
  @Override
  public void messageError(String to,
                           String from,
                           Serializable payload,
                           BamError error)
  {
    if (to == null)
      _linkActor.messageError(to, from, payload, error);
    else if (isActive())
      _broker.messageError(to, getClientAddress(), payload, error);
    else
      super.messageError(to, from, payload, error);
  }

  /**
   * Handles a query.
   *
   * The query handler must respond with either
   * a QueryResult or a QueryError
   */
  @Override
  public void query(long id,
                    String to,
                    String from,
                    Serializable payload)
  {
    try {
      if (to == null)
        _linkActor.query(id, to, from, payload);
      else if (isActive()) {
        _broker.query(id, to, getClientAddress(), payload);
      }
      else {
        super.query(id, to, from, payload);
      }
    } catch (Throwable e) {
     getLinkMailbox().queryError(id, from, to, payload, BamError.create(e));
    }
  }
  
  @Override
  protected MessageStream getQueryErrorStream(String from)
  {
    return getLinkMailbox();
  }

  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  @Override
  public void queryResult(long id,
                          String to,
                          String from,
                          Serializable payload)
  {
    if (to == null)
      _linkActor.queryResult(id, to, from, payload);
    else if (isActive())
      _broker.queryResult(id, to, getClientAddress(), payload);
    else
      super.queryResult(id, to, from, payload);
  }

  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  @Override
  public void queryError(long id,
                         String to,
                         String from,
                         Serializable payload,
                         BamError error)
  {
    if (to == null)
      _linkActor.queryError(id, to, from, payload, error);
    else if (isActive())
      _broker.queryError(id, to, getClientAddress(), payload, error);
    else
      super.queryError(id, to, from, payload, error);
  }

  @Override
  public boolean isClosed()
  {
    return false;
  }

  public void close()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "," + _linkActor + "]";
  }
}
