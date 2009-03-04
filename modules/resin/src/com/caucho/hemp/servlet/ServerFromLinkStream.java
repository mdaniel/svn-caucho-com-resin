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

import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.ActorException;
import com.caucho.bam.NotAuthorizedException;
import com.caucho.bam.hmtp.HmtpPacketType;
import com.caucho.bam.hmtp.FromLinkStream;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.*;
import com.caucho.hessian.io.*;
import com.caucho.security.Authenticator;
import com.caucho.security.PasswordCredentials;
import com.caucho.security.BasicPrincipal;
import com.caucho.security.SelfEncryptedCookie;
import com.caucho.server.connection.*;
import com.caucho.server.cluster.Server;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import java.io.*;
import java.security.*;
import java.util.logging.*;
import javax.servlet.*;


/**
 * Main protocol handler for the HTTP version of HMTP
 */
public class ServerFromLinkStream extends FromLinkStream
  implements TcpDuplexHandler
{
  private static final L10N L = new L10N(ServerFromLinkStream.class);
  private static final Logger log
    = Logger.getLogger(ServerFromLinkStream.class.getName());
  
  private HempBroker _broker;
  private ActorStream _toBroker;
  private ActorClient _conn;

  private Authenticator _auth;
  private boolean _isAuthenticationRequired;

  private Hessian2StreamingInput _in;
  private Hessian2Output _out;

  private ActorStream _linkStream;
  private ServerLinkService _linkService;
  private ActorStream _linkServiceStream;
  private AuthBrokerStream _authHandler;

  private String _jid;

  public ServerFromLinkStream(HempBroker broker,
			      ServerLinkManager linkManager,
			      InputStream is,
			      OutputStream os,
			      String ipAddress,
			      Authenticator auth,
			      boolean isAuthRequired)
  {
    super(is);

    _auth = auth;
    _isAuthenticationRequired = isAuthRequired;
    _broker = broker;

    if (log.isLoggable(Level.FINEST)) {
      is = new HessianDebugInputStream(is, log, Level.FINEST);
    }

    _in = new Hessian2StreamingInput(is);

    _linkStream = new ServerToLinkStream(getJid(), os);
    // _authHandler = new AuthBrokerStream(getJid(), _agentStream);
    _linkService = new ServerLinkService(this, _linkStream, linkManager);
    
    _linkServiceStream = new ServerLinkFilter(_linkService.getActorStream(),
					      ipAddress);
  }

  public String getJid()
  {
    return _jid;
  }

  @Override
  protected ActorStream getStream(String to)
  {
    ActorStream stream;

    if (to == null)
      return _linkServiceStream;
    else if (_conn != null)
      return _toBroker;
    else
      return _authHandler;
  }

  @Override
  protected ActorStream getToLinkStream()
  {
    return _linkStream;
  }

  @Override
  protected String getFrom(String from)
  {
    return getJid();
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpDuplexController controller)
    throws IOException
  {
    try {
      if (readPacket()) {
	return true;
      }
      else {
	controller.close();
	return false;
      }
    } catch (Exception e) {
      return false;
    }
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpDuplexController controller)
    throws IOException
  {
    return false;
  }

  String login(String uid, Object credentials, String resource,
	       String ipAddress)
  {
    Server server = Server.getCurrent();

    if (_auth == null && ! _isAuthenticationRequired) {
    }
    else if (_auth == null) {
      throw new NotAuthorizedException(L.l("{0} does not have a configured authenticator",
					     this));
    }
    else if (credentials instanceof String) {
      String password = (String) credentials;
    
      Principal user = new BasicPrincipal(uid);
      PasswordCredentials pwdCred = new PasswordCredentials(password);
    
      if (_auth.authenticate(user, pwdCred, null) == null) {
	throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
					     uid));
      }
    }
    else if (credentials instanceof SelfEncryptedCookie) {
      SelfEncryptedCookie cookie = (SelfEncryptedCookie) credentials;

      // XXX: cred timeout
      
      if (! cookie.getCookie().equals(server.getAdminCookie())) {
	throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
					     uid));
      }
    }
    else if (server.getAdminCookie() == null && credentials == null) {
      if (! "127.0.0.1".equals(ipAddress)) {
	throw new NotAuthorizedException(L.l("'{0}' is an invalid local address for '{1}', because no password credentials are available",
					     ipAddress, uid));
      }
    }
    else {
      throw new NotAuthorizedException(L.l("'{0}' is an unknown credential",
					   credentials));
    }
    
    _conn = _broker.getConnection(_linkStream, uid, null);

    _jid = _conn.getJid();
    
    _toBroker = _conn.getBrokerStream();

    return _jid;
  }
  
  /**
   * Handles a message
   */
  public void message(String to,
		      String from,
		      Serializable value)
  {
    _toBroker.message(to, _jid, value);
  }
  
  /**
   * Handles a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       ActorError error)
  {
    _toBroker.messageError(to, _jid, value, error);
  }
  
  /**
   * Handles a get query.
   *
   * The get handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void queryGet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    _toBroker.queryGet(id, to, _jid, payload);
  }
  
  /**
   * Handles a set query.
   *
   * The set handler must respond with either
   * a QueryResult or a QueryError 
   */
  public void querySet(long id,
		       String to,
		       String from,
		       Serializable payload)
  {
    _toBroker.querySet(id, to, _jid, payload);
  }
  
  /**
   * Handles a query result.
   *
   * The result id will match a pending get or set.
   */
  public void queryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    _toBroker.queryResult(id, to, _jid, value);
  }
  
  /**
   * Handles a query error.
   *
   * The result id will match a pending get or set.
   */
  public void queryError(long id,
			     String to,
			     String from,
			     Serializable value,
			     ActorError error)
  {
    _toBroker.queryError(id, to, _jid, value, error);
  }
  
  /**
   * Handles a presence availability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void presence(String to,
			   String from,
			   Serializable data)

  {
    _toBroker.presence(to, _jid, data);
  }
  
  /**
   * Handles a presence unavailability packet.
   *
   * If the handler deals with clients, the "from" value should be ignored
   * and replaced by the client's jid.
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    _toBroker.presenceUnavailable(to, _jid, data);
  }
  
  /**
   * Handles a presence probe from another server
   */
  public void presenceProbe(String to,
			      String from,
			      Serializable data)
  {
    _toBroker.presenceProbe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribe request from a client
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    _toBroker.presenceSubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence subscribed result to a client
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    _toBroker.presenceSubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribe request from a client
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    _toBroker.presenceUnsubscribe(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    _toBroker.presenceUnsubscribed(to, _jid, data);
  }
  
  /**
   * Handles a presence unsubscribed result to a client
   */
  public void presenceError(String to,
			      String from,
			      Serializable data,
			      ActorError error)
  {
    _toBroker.presenceError(to, _jid, data, error);
  }

  public void close()
  {
    Hessian2StreamingInput in = _in;
    _in = null;
    
    Hessian2Output out = _out;
    _out = null;

    if (in != null) {
      try { in.close(); } catch (IOException e) {}
    }

    if (out != null) {
      try { out.close(); } catch (IOException e) {}
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}
