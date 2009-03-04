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

package com.caucho.remote;

import java.io.*;
import javax.servlet.*;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.hemp.*;
import com.caucho.hemp.broker.*;
import com.caucho.hemp.servlet.*;
import com.caucho.bam.Broker;
import com.caucho.security.Authenticator;
import com.caucho.security.AdminAuthenticator;
import com.caucho.server.connection.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.util.logging.*;
import javax.servlet.http.HttpServletResponse;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class HmtpServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());
  private static final L10N L = new L10N(HmtpServlet.class);

  private boolean _isAdmin;
  private boolean _isAuthenticationRequired = true;
  
  private Authenticator _auth;
  private ServerLinkManager _linkManager;

  public void setAuthenticationRequired(boolean isAuthRequired)
  {
    _isAuthenticationRequired = isAuthRequired;
  }

  public boolean isAuthenticationRequired()
  {
    return _isAuthenticationRequired;
  }

  public void init()
  {
    try {
      InjectManager webBeans = InjectManager.getCurrent();

      if (_isAdmin)
	_auth = webBeans.getInstanceByType(AdminAuthenticator.class);
      else
	_auth = webBeans.getInstanceByType(Authenticator.class);
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
	log.log(Level.FINER, L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.",
				 this), e);
      }
      else {
	log.info(L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.  In the resin.xml, add an <sec:AdminAuthenticator>",
		   this));
      }
    }

    String authRequired = getInitParameter("authentication-required");

    if ("false".equals(authRequired))
      _isAuthenticationRequired = false;

    _linkManager = new ServerLinkManager(_auth);
  }
  
  /**
   * Service handling
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequestImpl req = (HttpServletRequestImpl) request;
    HttpServletResponseImpl res = (HttpServletResponseImpl) response;

    String upgrade = req.getHeader("Upgrade");

    if (! "HMTP/0.9".equals(upgrade)) {
      // eventually can use alt method
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    ReadStream is = req.getConnection().getReadStream();
    WriteStream os = req.getConnection().getWriteStream();

    HempBroker broker = HempBroker.getCurrent();
    String address = req.getRemoteAddr();

    ServerFromLinkStream fromLinkStream
      = new ServerFromLinkStream(broker, _linkManager, is, os, address,
				 _auth, _isAuthenticationRequired);

    TcpDuplexController controller = res.upgradeProtocol(fromLinkStream);
    
    controller.setIdleTimeMax(30 * 60 * 1000L);
  }
}
