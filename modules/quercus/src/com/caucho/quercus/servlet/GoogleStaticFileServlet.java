/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.GoogleQuercus;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.GoogleMergePath;
import com.caucho.vfs.GoogleStorePath;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GoogleStaticFileServlet extends GenericServlet {
  private String _gsBucket;
  private Path _path;

  private ServletContext _context;

  public GoogleStaticFileServlet()
  {
  }

  @Override
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    _context = config.getServletContext();

    String iniPath = getInitParameter("ini-file");

    String realPath = _context.getRealPath(iniPath);
    Path pwd = new FilePath(_context.getRealPath("/"));

    // don't call Quercus.init() as that will load all the modules
    GoogleQuercus quercus = new GoogleQuercus();
    quercus.setIniFile(pwd.lookup(realPath));

    String gsBucket = quercus.getIniString("quercus.gs_bucket");
    _gsBucket = gsBucket;

    _path = new GoogleMergePath(pwd, gsBucket, true);
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    Path path = getPath(req);

    if (! path.exists()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);

      return;
    }

    if (path.isDirectory()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);

      return;
    }

    String uri = req.getRequestURI();
    String mimeType = _context.getMimeType(uri);

    if (mimeType != null) {
      response.setContentType(mimeType);
    }
    else {
      response.setContentType("text/plain");
    }

    long len = path.getLength();
    response.setContentLength((int) len);

    OutputStream os = response.getOutputStream();
    path.writeToStream(os);
  }

  protected Path getPath(HttpServletRequest req)
  {
    String scriptPath = QuercusRequestAdapter.getPageServletPath(req);
    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    // don't want to use cached values
    Path pwd = _path.copy();

    Path path = pwd.lookup(req.getRealPath(scriptPath));

    if (path.isFile())
      return path;

    String fullPath;
    if (pathInfo != null)
      fullPath = scriptPath + pathInfo;
    else
      fullPath = scriptPath;

    return pwd.lookup(req.getRealPath(fullPath));
  }
}
