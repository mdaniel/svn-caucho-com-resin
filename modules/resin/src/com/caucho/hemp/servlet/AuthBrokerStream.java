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

import com.caucho.bam.ActorStream;
import com.caucho.bam.AbstractActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.hmtp.AuthResult;
import com.caucho.bam.hmtp.AuthQuery;
import java.io.*;
import java.util.logging.*;
import javax.servlet.*;

import com.caucho.hessian.io.*;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class AuthBrokerStream extends AbstractActorStream
{
  private static final Logger log
    = Logger.getLogger(AuthBrokerStream.class.getName());

  private ServerFromLinkStream _manager;
  private ActorStream _broker;

  AuthBrokerStream(ServerFromLinkStream manager, ActorStream server)
  {
    _manager = manager;
    _broker = server;
  }

  public String getJid()
  {
    return null;
  }

  public ActorStream getBrokerStream()
  {
    return _broker;
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  @Override
  public void queryGet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    _broker.queryError(id, from, to, value, 
		           new ActorError(ActorError.TYPE_CANCEL,
				         ActorError.FORBIDDEN));
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  @Override
  public void querySet(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    if (value instanceof AuthQuery) {
      AuthQuery auth = (AuthQuery) value;
      String ipAddress = null;

      String jid = _manager.login(auth.getUid(),
				  auth.getCredentials(),
				  auth.getResource(),
				  ipAddress);

      if (jid != null)
	_broker.queryResult(id, from, to, new AuthResult(jid));
      else
	_broker.queryError(id, from, to, value,
			       new ActorError(ActorError.TYPE_AUTH,
					     ActorError.FORBIDDEN));
    }
    else {
      // XXX: auth
      _broker.queryError(id, from, to, value,
			     new ActorError(ActorError.TYPE_CANCEL,
				           ActorError.FORBIDDEN));
    }
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void sendPresence(String to,
			   String from,
			   Serializable []data)
  {
    log.fine(this + " sendPresence requires login first");
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
    log.fine(this + " sendPresenceUnavailable requires login first");
  }

  /**
   * Presence probe from the server to a client
   */
  public void sendPresenceProbe(String to,
			        String from,
			        Serializable []data)
  {
    log.fine(this + " sendPresenceProbe requires login first");
  }

  /**
   * A subscription request from a client
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
    log.fine(this + " sendPresenceSubscribe requires login first");
  }

  /**
   * A subscription response to a client
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
    log.fine(this + " sendPresenceSubscribed requires login first");
  }

  /**
   * An unsubscription request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
    log.fine(this + " sendPresenceUnsubscribe requires login first");
  }

  /**
   * A unsubscription response to a client
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
    log.fine(this + " sendPresenceUnsubscribed requires login first");
  }

  /**
   * An error response to a client
   */
  public void sendPresenceError(String to,
			        String from,
			        Serializable []data,
			        ActorError error)
  {
    log.fine(this + " sendPresenceError requires login first");
  }
}
