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

package com.caucho.server.hmux;

import com.caucho.bam.Broker;
import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorError;
import com.caucho.bam.NotAuthorizedException;
import com.caucho.bam.ActorStream;
import com.caucho.bam.QueryGet;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;

import com.caucho.bam.hmtp.AuthQuery;
import com.caucho.bam.hmtp.AuthResult;
import com.caucho.bam.hmtp.GetPublicKeyQuery;
import com.caucho.bam.hmtp.EncryptedObject;
import com.caucho.bam.hmtp.SelfEncryptedCredentials;

import com.caucho.security.SelfEncryptedCookie;
import com.caucho.server.cluster.Server;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.servlet.ServerLinkManager;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import java.util.logging.*;

import java.security.Key;

/**
 * The HmuxLinkService is low-level link
 */

public class HmuxLinkService extends SimpleActor {
  private static final Logger log
    = Logger.getLogger(HmuxLinkService.class.getName());
  private static final L10N L = new L10N(HmuxLinkService.class);
  
  private ServerLinkManager _linkManager;
  private Server _server;

  private HmuxRequest _request;
  private ActorClient _adminConn;
  
  /**
   * Creates the LinkService for low-level link messages
   */
  public HmuxLinkService(Server server, HmuxRequest request)
  {
    _server = server;
    _linkManager = _server.getServerLinkManager();
    _request = request;
    
    // the agent stream serves as its own broker because there's no
    // routing involved
    setBrokerStream(new HmuxBamStream(request));
  }

  //
  // message handling
  //

  @QueryGet
  public void getPublicKey(long id, String to, String from,
			      GetPublicKeyQuery query)
  {
    GetPublicKeyQuery result = _linkManager.getPublicKey();

    getBrokerStream().queryResult(id, from, to, result);
  }

  @QuerySet
  public void authLogin(long id, String to, String from, AuthQuery query)
  {
    Object credentials = query.getCredentials();
    String adminCookie = _server.getAdminCookie();

    if (adminCookie == null) {
    }
    else if (credentials instanceof EncryptedObject) {
      EncryptedObject encPassword = (EncryptedObject) credentials;

      Key key = _linkManager.decryptKey(encPassword.getKeyAlgorithm(),
					encPassword.getEncKey());

      credentials = _linkManager.decrypt(key, encPassword.getEncData());
    }
    else if (credentials instanceof SelfEncryptedCredentials) {
      SelfEncryptedCredentials encCredentials
	= (SelfEncryptedCredentials) credentials;

      byte []encData = encCredentials.getEncData();

      SelfEncryptedCookie selfCookie
	= SelfEncryptedCookie.decrypt(adminCookie, encData);

      credentials = selfCookie.getCookie();
      long creationDate = selfCookie.getCreateTime();
      long now = Alarm.getCurrentTime();

      if (Math.abs(creationDate - now) > 3 * 3600) {
	log.warning(this + " expired credentials date");

	getBrokerStream().queryError(id, from, to, query,
				     new ActorError(ActorError.TYPE_AUTH,
						    ActorError.FORBIDDEN,
						    "expired credentials"));
	return;
      }
    }
    else {
      getBrokerStream().queryError(id, from, to, query,
				   new ActorError(ActorError.TYPE_AUTH,
						ActorError.FORBIDDEN,
						"passwords must be encrypted"));
      return;
    }

    String cookie = (String) credentials;

    if (cookie == null && adminCookie == null) {
    }
    else if (cookie == null
	     || ! "admin.resin".equals(query.getUid())
	     || ! cookie.equals(adminCookie)) {
      throw new NotAuthorizedException(L.l("admin.resin login forbidden because the authentication cookies do not match"));
    }
    
    Broker broker = _server.getAdminBroker();
    
    _adminConn = broker.getConnection("admin.resin", cookie);

    _request.setHmtpAdminConnection(_adminConn);

    getBrokerStream().queryResult(id, from, to,
				  new AuthResult(_adminConn.getJid()));
  }

  public ActorStream getBrokerStream(boolean isAdmin)
  {
    if (_adminConn == null) {
      Broker broker = _server.getAdminBroker();

      String cookie = _server.getAdminCookie();

      if (cookie != null)
	throw new NotAuthorizedException(L.l("'{0}' anonymous login is not allowed in this server",
						cookie));
    
      _adminConn = broker.getConnection("admin.resin", null);
    }

    return _adminConn.getBrokerStream();
  }
}
