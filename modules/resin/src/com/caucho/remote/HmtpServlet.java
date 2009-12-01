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

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.hemp.*;
import com.caucho.hemp.broker.*;
import com.caucho.hemp.servlet.*;
import com.caucho.bam.Broker;
import com.caucho.security.Authenticator;
import com.caucho.security.AdminAuthenticator;
import com.caucho.server.connection.*;
import com.caucho.server.cluster.Server;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.servlet.DuplexContext;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.*;

import javax.inject.Inject;
import javax.enterprise.inject.Instance;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class HmtpServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());
  private static final L10N L = new L10N(HmtpServlet.class);

  private boolean _isAdmin;
  private boolean _isAuthenticationRequired = true;

  private @Inject Instance<Authenticator> _authInstance;
  private @Inject Instance<AdminAuthenticator> _adminInstance;

  private Authenticator _auth;
  private ServerLinkManager _linkManager;

  public void setAdmin(boolean isAdmin)
  {
    _isAdmin = isAdmin;
  }

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
    String authRequired = getInitParameter("authentication-required");

    if ("false".equals(authRequired))
      _isAuthenticationRequired = false;

    String admin = getInitParameter("admin");

    if ("true".equals(admin))
      _isAdmin = true;

    try {
      if (_isAdmin)
        _auth = _adminInstance.get();
      else
        _auth = _authInstance.get();
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

    Broker broker;

    if (_isAdmin)
      broker = Server.getCurrent().getAdminBroker();
    else
      broker = HempBroker.getCurrent();

    String address = req.getRemoteAddr();

    ServerFromLinkStream fromLinkStream
      = new ServerFromLinkStream(broker, _linkManager, is, os, address,
                                 _auth, _isAuthenticationRequired);

    DuplexContext duplex = req.startDuplex(fromLinkStream);

    duplex.setTimeout(30 * 60 * 1000L);
  }
}
