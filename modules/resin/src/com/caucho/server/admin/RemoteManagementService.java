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
 * @author Sam
 */

package com.caucho.server.admin;

import com.caucho.config.ConfigException;
import com.caucho.config.program.*;
import com.caucho.config.types.RawString;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.hmux.HmuxRequest;
import com.caucho.server.host.HostConfig;
import com.caucho.server.security.AbstractConstraint;
import com.caucho.server.security.SecurityConstraint;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.logging.Logger;

public class RemoteManagementService
{
  private static final L10N L = new L10N(RemoteManagementService.class);
  protected static final Logger log
    = Logger.getLogger(RemoteManagementService.class.getName());

  private final Management _management;

  private InetNetwork[]_allowedNetworks;

  protected RemoteManagementService(Management management)
  {
    _management = management;
  }

  public void start()
  {
    if (_management.getRemoteCookie() == null) {
      log.warning(L.l("Remote administration disabled.  Remote administration requires at least one enabled management <user>"));
      return;
    }
    
    HostConfig hostConfig = _management.getHostConfig();

    WebAppConfig webAppConfig = new WebAppConfig();
    webAppConfig.setId("remote-service");
    webAppConfig.setRootDirectory(new RawString("/admin-dummy-root"));

    ConfigProgram program;
    
    program = new PropertyValueProgram("web-app", webAppConfig);
    hostConfig.addBuilderProgram(program);

    ServletMapping servlet = new ServletMapping();

    servlet.setServletName("remote-management");
    servlet.addURLPattern("/*");
    servlet.addURLRegexp(".*");
    servlet.setServletClass(ManagementServlet.class.getName());

    ContainerProgram servletInit = new ContainerProgram();
    servletInit.addProgram(new PropertyValueProgram("service", this));
    servlet.setInit(servletInit);
    
    program = new PropertyValueProgram("servlet-mapping", servlet);
    webAppConfig.addBuilderProgram(program);

    SecurityConstraint constraint = new SecurityConstraint();
    constraint.setURLPattern("/*");
    constraint.addConstraint(new HmuxConstraint(this));
    constraint.init();

    program = new PropertyValueProgram("security-constraint", constraint);
    webAppConfig.addBuilderProgram(program);

    try {
      _allowedNetworks = new InetNetwork[] {
        new InetNetwork(InetAddress.getByName("127.0.0.1"), 24),
        new InetNetwork(InetAddress.getByName("10.0.0.0"), 24),
        new InetNetwork(InetAddress.getByName("172.16.0.0"), 20),
        new InetNetwork(InetAddress.getByName("192.168.0.0"), 16),
      };
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * security constraint authorization
   */
  public boolean isAuthorized(HttpServletRequest request,
			      HttpServletResponse response,
                              ServletContext application)
    throws ServletException, IOException
  {
    // The order of tests is important for the QA, see server/2e2-

    HttpServletResponse res = (HttpServletResponse) response;

    if (! (request instanceof HmuxRequest)) {
      log.warning(L.l("remote management attempt with non-hmux-request '{0}' from ip {1}",
                      request, request.getRemoteAddr()));

      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      
      return false;
    }

    HmuxRequest hmuxRequest = (HmuxRequest) request;

    if (! request.getServletPath().equals("")) {
      log.warning(L.l("remote management attempt with invalid servlet-path '{0}' from address '{1}'",
		      request.getServletPath(), request.getRemoteAddr()));

      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      
      return false;
    }

    if (request.getPathInfo() != null) {
      log.warning(L.l("remote management attempt with invalid path-info '{0}' from address '{1}'",
                      request.getPathInfo(), request.getRemoteAddr()));

      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      
      return false;
    }

    // The remote test needs to be last to allow the other QA to work properly
    // XXX: this should be configurable, i.e. setReadConstraints and setWriteConstraints

    String remoteAddr = hmuxRequest.getRemoteAddr();

    boolean isValidAddr = false;
    for (int i = 0; i < _allowedNetworks.length; i++) {
      if (_allowedNetworks[i].isMatch(remoteAddr))
        isValidAddr = true;
    }

    if (! isValidAddr) {
      log.warning(L.l("remote management attempt from invalid address '{0}'",
                      remoteAddr));

      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return false;
    }

    return true;
  }

  /**
   * read/write permission
   */
  private boolean isRequestAllowed(ServletRequest request,
				   ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletResponse res = (HttpServletResponse) response;

    // check attributes for evidence of forward, includes, error page handling
    Enumeration<String> attributeNames = request.getAttributeNames();

    while (attributeNames.hasMoreElements()) {
      String  attributeName = attributeNames.nextElement();

      if (attributeName.equals("javax.servlet.request.X509Certificate"))
        continue;
      
      log.warning(L.l("management service request attribute '{0}' invalidates request",
                      attributeName));

      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return false;
    }

    return true;
  }

  protected boolean isReadAllowed(ServletRequest request,
				  ServletResponse response)
    throws IOException, ServletException
  {
    if (!isRequestAllowed(request, response))
      return false;

    return true;
  }

  protected boolean isWriteAllowed(ServletRequest request,
				   ServletResponse response)
    throws IOException, ServletException
  {
    if (! isRequestAllowed(request, response))
      return false;

    return true;
  }

  public void service(ServletRequest request,
		      ServletResponse response)
    throws IOException, ServletException
  {
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _management.getServerId() + "]");
  }

  private static class HmuxConstraint
    extends AbstractConstraint
  {
    private final RemoteManagementService _service;

    public HmuxConstraint(RemoteManagementService service)
    {
      _service = service;
    }

    public boolean isAuthorized(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application)
      throws ServletException, IOException
    {
      return _service.isAuthorized(request, response, application);
    }
  }
}
