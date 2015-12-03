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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;
import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.broker.Broker;
import com.caucho.hmtp.AuthQuery;
import com.caucho.hmtp.AuthResult;
import com.caucho.hmtp.NonceQuery;

/**
 * ServerLinkActor handles link messages, i.e. to=null, which is primarily
 * authentication.
 */

public class ServerLinkActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ServerLinkActor.class.getName());
  
  private final ClientStubManager _clientManager;
  
  private final ServerAuthManager _authManager;
  private final String _ipAddress;
  
  private String _clientAddress;
  
  public ServerLinkActor(Broker toLinkBroker,
                         ClientStubManager clientManager,
                         ServerAuthManager authManager,
                         String ipAddress)
  {
    super(null, toLinkBroker);
    
    _clientManager = clientManager;
    _authManager = authManager;
    _ipAddress = ipAddress;
  }

  //
  // message handling
  //

  @Query
  public void getNonce(long id, String to, String from,
                       NonceQuery query)
  {
    NonceQuery result = _authManager.generateNonce(query);

    getBroker().queryResult(id, from, to, result);
  }

  @Query
  public void authLogin(long id, String to, String from, LoginQuery query)
  {
    login(id, to, from, query.getAuth(), query.getAddress());
  }

  @Query
  public void authLogin(long id, String to, String from, AuthQuery query)
  {
    login(id, to, from, query, _ipAddress);
  }

  private void login(long id, String to, String from,
                     AuthQuery query, String ipAddress)
  {
    String uid = query.getUid();
    Object credentials = query.getCredentials();

    try {
      _authManager.authenticate(query.getUid(), credentials, ipAddress);
    } catch (BamException e) {
      log.log(Level.FINE, e.toString(), e);
    
      getBroker().queryError(id, from, to, query,
                                 e.createActorError());

      return;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    
      getBroker().queryError(id, from, to, query,
                                 new BamError(BamError.TYPE_AUTH,
                                              BamError.NOT_AUTHORIZED,
                                              e.getMessage()));
      return;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    _clientManager.login(uid, query.getResource());
    
    notifyValidLogin(from);
    
    AuthResult result = new AuthResult(_clientManager.getAddress());
    getBroker().queryResult(id, from, to, result);
  }
  
  protected void onClose()
  {
    _clientManager.logout();
  }
  
  protected void notifyValidLogin(String address)
  {
  }
}
