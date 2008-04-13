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

package com.caucho.hemp.servlet;

import com.caucho.hmpp.HmppError;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.hmpp.*;
import com.caucho.hmpp.auth.*;
import com.caucho.hmpp.spi.*;
import com.caucho.hemp.service.*;
import com.caucho.hessian.io.*;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class AuthInboundStream extends AbstractHmppStream
{
  private static final Logger log
    = Logger.getLogger(AuthInboundStream.class.getName());

  private ServerInboundStream _manager;
  private HmppStream _broker;

  AuthInboundStream(ServerInboundStream manager, HmppStream server)
  {
    _manager = manager;
    _broker = server;
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  @Override
  public boolean sendQueryGet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    _broker.sendQueryError(id, from, to, value, 
		           new HmppError(HmppError.TYPE_CANCEL,
				         HmppError.FORBIDDEN));
      
    return true;
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  @Override
  public boolean sendQuerySet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    if (value instanceof AuthQuery) {
      AuthQuery auth = (AuthQuery) value;

      String jid = _manager.login(auth.getUid(),
				  auth.getCredentials(),
				  auth.getResource());

      if (jid != null)
	_broker.sendQueryResult(id, from, to, new AuthResult(jid));
      else
	_broker.sendQueryError(id, from, to, value,
			       new HmppError(HmppError.TYPE_AUTH,
					     HmppError.FORBIDDEN));
    }
    else {
      // XXX: auth
      _broker.sendQueryError(id, from, to, value,
			     new HmppError(HmppError.TYPE_CANCEL,
				           HmppError.FORBIDDEN));
    }
    
    return true;
  }
}
