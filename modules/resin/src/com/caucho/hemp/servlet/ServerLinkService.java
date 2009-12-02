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

import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.bam.QueryGet;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleActor;

import com.caucho.hmtp.AuthQuery;
import com.caucho.hmtp.AuthResult;
import com.caucho.hmtp.EncryptedObject;
import com.caucho.hmtp.GetPublicKeyQuery;
import com.caucho.hmtp.SelfEncryptedCredentials;

import com.caucho.security.SelfEncryptedCookie;
import com.caucho.security.SecurityException;
import com.caucho.server.cluster.Server;

import java.security.Key;
import java.util.logging.*;

/**
 * The LinkService is low-level link
 */

public class ServerLinkService extends SimpleActor {
  private static final Logger log
    = Logger.getLogger(ServerLinkService.class.getName());
  
  private ServerLinkManager _linkManager;
  
  private ServerFromLinkStream _manager;

  private boolean _isRequireEncryptedPassword = true;
  
  /**
   * Creates the LinkService for low-level link messages
   */
  public ServerLinkService(ServerFromLinkStream manager,
			   ActorStream agentStream,
			   ServerLinkManager linkManager)
  {
    _manager = manager;
    _linkManager = linkManager;
    
    // the agent stream serves as its own broker because there's no
    // routing involved
    setLinkStream(agentStream);
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
  public void authLogin(long id, String to, String from, LoginQuery query)
  {
    login(id, to, from, query.getAuth(), query.getAddress());
  }

  @QuerySet
  public void authLogin(long id, String to, String from, AuthQuery query)
  {
    login(id, to, from, query, null);
  }

  private boolean login(long id, String to, String from,
			AuthQuery query, String ipAddress)
  {
    Object credentials = query.getCredentials();

    if (credentials instanceof EncryptedObject) {
      EncryptedObject encPassword = (EncryptedObject) credentials;

      Key key = _linkManager.decryptKey(encPassword.getKeyAlgorithm(),
					encPassword.getEncKey());

      credentials = _linkManager.decrypt(key, encPassword.getEncData());
    }
    else if (credentials instanceof SelfEncryptedCredentials) {
      try {
	SelfEncryptedCredentials encCred
	  = (SelfEncryptedCredentials) credentials;

	byte []encData = encCred.getEncData();

	Server server = Server.getCurrent();

	String adminCookie = server.getAdminCookie();

	if (adminCookie != null) {
	  credentials = SelfEncryptedCookie.decrypt(adminCookie, encData);
	}
	else
	  credentials = null;
      } catch (SecurityException e) {
	log.log(Level.FINE, e.toString(), e);
	
	getLinkStream().queryError(id, from, to, query,
				     new ActorError(ActorError.TYPE_AUTH,
						    ActorError.FORBIDDEN,
						    e.getMessage()));
	return true;
      }
    }
    else if (_isRequireEncryptedPassword) {
      getLinkStream().queryError(id, from, to, query,
				   new ActorError(ActorError.TYPE_AUTH,
						ActorError.FORBIDDEN,
						"passwords must be encrypted"));
      return true;
    }
    
    String jid = _manager.login(query.getUid(),
				credentials,
				query.getResource(),
				ipAddress);

    if (jid != null)
      getLinkStream().queryResult(id, from, to, new AuthResult(jid));
    else
      getLinkStream().queryError(id, from, to, query,
				   new ActorError(ActorError.TYPE_AUTH,
						ActorError.FORBIDDEN));

    return true;
  }
}
