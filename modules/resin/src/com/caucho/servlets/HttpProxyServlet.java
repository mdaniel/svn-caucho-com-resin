/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.servlets;

import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP proxy
 *
 * <pre>
 * &lt;servlet>
 *   &lt;servlet-name>http-proxy&lt;/servlet-name>
 *   &lt;servlet-class>com.caucho.servlets.HttpProxyServlet&lt;/servlet-class>
 *   &lt;init host='localhost:8081'/>
 * &lt;/servlet>
 * </pre>
 */
public class HttpProxyServlet extends GenericServlet {
  static protected final Logger log =
    Logger.getLogger(HttpProxyServlet.class.getName());
  static final L10N L = new L10N(HttpProxyServlet.class);
  
  private ArrayList<String> _hosts = new ArrayList<String>();
  private Path []_urlPaths;
  private int _roundRobin;

  /**
   * Adds a host
   */
  public void addHost(String host)
  {
    _hosts.add(host);
  }

  /**
   * Initialize the servlet with the server's sruns.
   */
  public void init()
    throws ServletException
  {
    if (_hosts.size() == 0)
      throw new ServletException(L.l("HttpProxyServlet needs at least one host."));

    _urlPaths = new Path[_hosts.size()];

    for (int i = 0; i < _hosts.size(); i++) {
      String host = _hosts.get(i);

      if (host.startsWith("http"))
	_urlPaths[i] = Vfs.lookup(host);
      else
	_urlPaths[i] = Vfs.lookup("http://" + host);
    }
  }

  /**
   * Handle the request.
   */
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    PrintWriter out = res.getWriter();

    int startIndex = _roundRobin;
    _roundRobin = (_roundRobin + 1) % _urlPaths.length;

    for (int i = 0; i < _urlPaths.length; i++) {
      int index = (startIndex + i) % _urlPaths.length;
	
      if (handleRequest(req, res, _urlPaths[index]))
	return;
    }
      
    res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
  }

  private boolean handleRequest(HttpServletRequest req,
				HttpServletResponse res,
				Path path)
    throws ServletException, IOException
  {
    String hostURL = path.getURL();
    
    String uri;
    if (req.isRequestedSessionIdFromUrl()) {
      uri =  (req.getRequestURI() + ";jsessionid=" +
	      req.getRequestedSessionId());
    }
    else
      uri = req.getRequestURI();

    if (req.getQueryString() != null)
      uri += '?' + req.getQueryString();

    path = path.lookup(uri);

    ReadWritePair pair = path.openReadWrite();

    ReadStream rs = pair.getReadStream();
    WriteStream ws = pair.getWriteStream();

    ws.setAttribute("method", req.getMethod());

    Enumeration e = req.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      String value = req.getHeader(name);

      ws.setAttribute(name, value);
    }

     try {
      InputStream is = req.getInputStream();
      ws.writeStream(is);

      String status = (String) rs.getAttribute("status");
      int statusCode = 200;

      if (status != null) {
	try {
	  statusCode = Integer.parseInt(status);
	} catch (Throwable e1) {
	}
      }

      String location = null;
      Iterator iter = rs.getAttributeNames();
      while (iter.hasNext()) {
	String name = (String) iter.next();

	if (name.equalsIgnoreCase("status")) {
	}
	else if (name.equalsIgnoreCase("transfer-encoding")) {
	}
	else if (name.equalsIgnoreCase("content-length")) {
	}
	else if (name.equalsIgnoreCase("location"))
	  location = (String) rs.getAttribute("location");
	else
	  res.addHeader(name, (String) rs.getAttribute(name));
      }

      if (location == null) {
      }
      else if (location.startsWith(hostURL)) {
	location = location.substring(hostURL.length());

	String prefix;
	if (req.isSecure()) {
	  if (req.getServerPort() != 443)
	    prefix = ("https://" + req.getServerName() +
		      ":" + req.getServerPort());
	  else
	    prefix = ("https://" + req.getServerName());
	}
	else {
	  if (req.getServerPort() != 80)
	    prefix = ("http://" + req.getServerName() +
		      ":" + req.getServerPort());
	  else
	    prefix = ("http://" + req.getServerName());
	}
	
	if (! location.startsWith("/"))
	  location = prefix + "/" + location;
	else
	  location = prefix + location;
      }

      if (location != null)
	res.setHeader("Location", location);

      if (statusCode == 302 && location != null)
	res.sendRedirect(location);
      else if (statusCode != 200)
	res.setStatus(statusCode);
	
      OutputStream os = res.getOutputStream();
      rs.writeToStream(os);
    } catch (IOException e1) {
      log.log(Level.FINE, e1.toString(), e1);

      return false;
    } finally {
      ws.close();
      rs.close();
    }

    return true;
  }
}
