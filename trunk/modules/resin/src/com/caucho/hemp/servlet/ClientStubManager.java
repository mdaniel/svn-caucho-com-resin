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

import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.util.L10N;

/**
 * Manages the client stub for the link.
 */
public class ClientStubManager {
  private static final L10N L = new L10N(ClientStubManager.class);
  
  private final ManagedBroker _broker;
  private final Mailbox _toLinkMailbox;

  private Mailbox _clientStub;

  public ClientStubManager(ManagedBroker broker,
                           Mailbox toLinkMailbox)
  {
    _broker = broker;
    _toLinkMailbox = toLinkMailbox;
  }
  
  public Mailbox getToLinkMailbox()
  {
    return _toLinkMailbox;
  }

  public String getAddress()
  {
    if (_clientStub != null)
      return _clientStub.getAddress();
    else
      throw new IllegalStateException(L.l("{0}: Client stub has not been registered",
                                          this));
  }
  
  public boolean isActive()
  {
    return _clientStub != null;
  }
  
  public void login(String uid, String resource)
  {
    if (_clientStub != null)
      throw new IllegalStateException(L.l("{0}: Client stub alread exists",
                                          this));
    
    _clientStub = _broker.createClient(_toLinkMailbox, uid, resource);
  }
  
  public void logout()
  {
    Mailbox clientStub = _clientStub;
    _clientStub = null;
    
    if (clientStub != null)
      _broker.removeMailbox(clientStub);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _toLinkMailbox + "]";
  }
}
