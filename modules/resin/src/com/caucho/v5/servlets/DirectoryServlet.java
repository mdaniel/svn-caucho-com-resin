/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.servlets;

import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.webapp.WebAppResinBase;
import com.caucho.v5.i18n.CharacterEncoding;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.URLUtil;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

public class DirectoryServlet extends HttpServlet {
  WebAppResinBase _app;
  PathImpl _context;
  private boolean _enable = true;

  public DirectoryServlet(PathImpl context)
  {
    _context = context;
  }

  public DirectoryServlet()
  {
    this(VfsOld.lookup());
  }

  public void setEnable(boolean enable)
  {
    _enable = enable;
  }

  public void init()
  {
    _app = (WebAppResinBase) getServletContext();
    _context = _app.getRootDirectory();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)

    throws IOException
  {
    if (! _enable) {
      res.sendError(404);
      return;
    }
    
    RequestCaucho cauchoReq = null;

    if (req instanceof RequestCaucho)
      cauchoReq = (RequestCaucho) req;

    String uri = req.getRequestURI();
 
    String encoding = CharacterEncoding.getLocalEncoding();
    if (encoding == null)
      res.setContentType("text/html");
    else
      res.setContentType("text/html; charset=" + encoding);

    boolean isInclude = false;

    if (cauchoReq != null) {
      uri = cauchoReq.getPageURI();
      isInclude = ! uri.equals(cauchoReq.getRequestURI());
    }
    else {
      uri = (String) req.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
      if (uri != null)
        isInclude = true;
      else
        uri = req.getRequestURI();
    }

    StringBuilder cb = new StringBuilder();
    String servletPath;

    if (cauchoReq != null)
      servletPath = cauchoReq.getPageServletPath();
    else if (isInclude)
      servletPath = (String) req.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
    else
      servletPath = req.getServletPath();
        
    if (servletPath != null)
      cb.append(servletPath);
      
    String pathInfo;
    if (cauchoReq != null)
      pathInfo = cauchoReq.getPagePathInfo();
    else if (isInclude)
      pathInfo = (String) req.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
    else
      pathInfo = req.getPathInfo();
        
    if (pathInfo != null)
      cb.append(pathInfo);
    
    String relPath = cb.toString();
    String filename = getServletContext().getRealPath(relPath);
    PathImpl path = _context.lookupNative(filename);

    if (CauchoUtil.isWindows() && path.isWindowsInsecure()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    
    if (uri.length() > 0 && uri.charAt(uri.length() - 1) != '/') {
      res.sendRedirect(uri + "/");
      return;
    }

    String rawpath = java.net.URLDecoder.decode(uri);

    PrintWriter pw = res.getWriter();

    if (rawpath.length() == 0 || rawpath.charAt(0) != '/')
      rawpath = "/" + rawpath;

    boolean endsSlash = rawpath.charAt(rawpath.length() - 1) == '/';
    String tail = "";
    if (! endsSlash) {
      int p = rawpath.lastIndexOf('/');
      tail = rawpath.substring(p + 1) + "/";
      rawpath = rawpath + "/";
    }

    pw.println("<html>");
    pw.println("<head>");
    pw.println("<title>Directory of " + rawpath + "</title>");
    pw.println("</head>");
    pw.println("<body>");

    pw.println("<h1>Directory of " + rawpath + "</h1>");

    pw.println("<ul>");

    Iterator i = path.iterator();
    while (i.hasNext()) {
      String name = (String) i.next();

      if (name.equalsIgnoreCase("web-inf") ||
          name.equalsIgnoreCase("meta-inf") ||
          name.equalsIgnoreCase(".ds_store")) {
        continue;
      }

      String enc = URLUtil.encodeURL(tail + name);

      pw.println("<li><a href='" + enc + "'>" + name + "</a>");
    }
    pw.println("</ul>");
    pw.println("</body>");
    pw.println("</html>");
    pw.close();
  }
}
