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

import java.security.Key;
import java.util.logging.Logger;

import com.caucho.bam.ActorClient;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.Broker;
import com.caucho.bam.NotAuthorizedException;
import com.caucho.bam.QueryGet;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;
import com.caucho.bam.SimpleActorClient;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.hmtp.AuthQuery;
import com.caucho.hmtp.AuthResult;
import com.caucho.hmtp.EncryptedObject;
import com.caucho.hmtp.GetPublicKeyQuery;
import com.caucho.hmtp.SelfEncryptedCredentials;
import com.caucho.security.SelfEncryptedCookie;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.DynamicServerQuery;
import com.caucho.server.cluster.DynamicServerResult;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * The HmuxLinkService is low-level link
 */

public class HmuxLinkService extends SimpleActor {
  private static final Logger log
    = Logger.getLogger(HmuxLinkService.class.getName());
  private static final L10N L = new L10N(HmuxLinkService.class);
  
  private ServerAuthManager _linkManager;
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
    setLinkStream(new HmuxBamStream(request));
  }

  //
  // message handling
  //

  @QueryGet
  public void getPublicKey(long id, String to, String from,
			      GetPublicKeyQuery query)
  {
    GetPublicKeyQuery result = _linkManager.getPublicKey();

    getLinkStream().queryResult(id, from, to, result);
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

      if (Math.abs(creationDate - now) > 3 * 3600 * 1000) {
	log.warning(this + " expired credentials date");

	getLinkStream().queryError(id, from, to, query,
				     new ActorError(ActorError.TYPE_AUTH,
						    ActorError.FORBIDDEN,
						    "expired credentials"));
	return;
      }
    }
    else {
      getLinkStream().queryError(id, from, to, query,
				   new ActorError(ActorError.TYPE_AUTH,
						ActorError.FORBIDDEN,
						"passwords must be encrypted"));
      return;
    }

    if (! (credentials instanceof String)) {
      getLinkStream().queryError(id, from, to, query,
				   new ActorError(ActorError.TYPE_AUTH,
						  ActorError.FORBIDDEN,
						  "unknown credentials: " + credentials));

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
    
    _adminConn = _server.createAdminClient("admin.resin");

    _request.setHmtpAdminConnection(_adminConn);

    getLinkStream().queryResult(id, from, to,
				  new AuthResult(_adminConn.getJid()));
  }

  @QueryGet
  public void getDynamicService(long id, String to, String from,
				DynamicServerQuery query)
  {
    ClusterServer clusterServer
      = _server.getResin().findClusterServer(query.getId());

    if (clusterServer != null) {
      DynamicServerResult result
	= new DynamicServerResult(clusterServer.getId(),
				  clusterServer.getIndex(),
				  clusterServer.getAddress(),
				  clusterServer.getPort());

      getLinkStream().queryResult(id, from, to, result);
    }
    else 
      getLinkStream().queryResult(id, from, to, null);
  }

  public ActorStream getBrokerStream(boolean isAdmin)
  {
    if (_adminConn == null) {
      String cookie = _server.getAdminCookie();

      if (cookie != null)
	throw new NotAuthorizedException(L.l("'{0}' anonymous login is not allowed in this server",
						cookie));
    
      _adminConn = _server.createAdminClient("admin.resin");
    }

    return _adminConn.getActorStream();
  }
}
