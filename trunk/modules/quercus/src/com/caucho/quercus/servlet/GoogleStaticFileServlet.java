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
import com.caucho.quercus.servlet.api.QuercusHttpServletRequest;
import com.caucho.quercus.servlet.api.QuercusHttpServletRequestImpl;
import com.caucho.util.Base64;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.QDate;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.GoogleMergePath;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GoogleStaticFileServlet extends GenericServlet {
  private static final L10N L = new L10N(GoogleStaticFileServlet.class);
  private static final Logger log
    = Logger.getLogger(GoogleStaticFileServlet.class.getName());

  private String _gsBucket;
  private Path _pwd;

  private ServletContext _context;

  private LruCache<String,CacheEntry> _cache;

  public GoogleStaticFileServlet()
  {
  }

  @Override
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);

    _context = config.getServletContext();
    _cache = new LruCache<String,CacheEntry>(1024);

    _pwd = new FilePath(_context.getRealPath("/"));

    String mode
      = System.getProperty("com.google.appengine.tools.development.ApplicationPreparationMode");

    boolean isGsDisabled = "true".equals(mode);

    if (! isGsDisabled) {
      String iniPath = getInitParameter("ini-file");

      if (iniPath != null) {
        String realPath = _context.getRealPath(iniPath);

        // don't call Quercus.init() as that will load all the modules for calling
        // from PHP code, which we don't use nor want
        GoogleQuercus quercus = new GoogleQuercus();
        quercus.setIniFile(_pwd.lookup(realPath));

        _gsBucket = quercus.getIniString("google.cloud_storage_bucket");

        if (_gsBucket != null) {
          _pwd = new GoogleMergePath(_pwd, _gsBucket, true);
        }
      }
    }

    log.log(Level.INFO, L.l("{0} initialized with bucket={1},pwd={2}",
                            getClass().getSimpleName(), _gsBucket, _pwd.getUserPath()));
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    QuercusHttpServletRequest req
      = new QuercusHttpServletRequestImpl((HttpServletRequest) request);
    HttpServletResponse res = (HttpServletResponse) response;

    String uri = QuercusRequestAdapter.getPageURI(req);

    CacheEntry entry = _cache.get(uri);

    if (entry == null) {
      Path path = getPath(req);
      String relPath = path.getUserPath();

      String mimeType = _context.getMimeType(uri);

      entry = new CacheEntry(path, relPath, mimeType);
      _cache.put(uri, entry);
    }
    else if (entry.isModified()) {
      entry = new CacheEntry(entry.getPath(),
                             entry.getRelPath(),
                             entry.getMimeType());

      _cache.put(uri, entry);
    }

    String ifMatch = req.getHeader("If-None-Match");
    String etag = entry.getEtag();

    if (ifMatch != null && ifMatch.equals(etag)) {
      res.addHeader("ETag", etag);
      res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String lastModified = entry.getLastModifiedString();

    if (ifMatch == null) {
      String ifModified = req.getHeader("If-Modified-Since");

      boolean isModified = true;

      if (ifModified == null) {
      }
      else if (ifModified.equals(lastModified)) {
        isModified = false;
      }
      else {
        long ifModifiedTime;

        QDate date = QDate.allocateLocalDate();

        try {
          ifModifiedTime = date.parseDate(ifModified);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);

          ifModifiedTime = 0;
        }

        QDate.freeLocalDate(date);

        isModified = ifModifiedTime == 0
                       || ifModifiedTime != entry.getLastModified();
      }

      if (! isModified) {
        if (etag != null) {
          res.addHeader("ETag", etag);
        }

        res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }
    }

    res.addHeader("ETag", etag);
    res.addHeader("Last-Modified", lastModified);

    String mime = entry.getMimeType();
    if (mime != null) {
      res.setContentType(mime);
    }

    res.setContentLength((int) entry.getLength());

    String method = req.getMethod();
    if (method.equalsIgnoreCase("HEAD")) {
      return;
    }

    Path path = entry.getPath();

    if (path.isDirectory()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);

      return;
    }
    else if (! path.canRead()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);

      return;
    }

    OutputStream os = response.getOutputStream();
    path.writeToStream(os);
  }

  protected Path getPath(QuercusHttpServletRequest req)
  {
    // copy to clear status cache
    // XXX: improve caching so we don't need to do this anymore
    Path pwd = _pwd.copy();

    StringBuilder sb = new StringBuilder();
    String servletPath = QuercusRequestAdapter.getPageServletPath(req);

    if (servletPath.startsWith("/")) {
      sb.append(servletPath, 1, servletPath.length());
    }
    else {
      sb.append(servletPath);
    }

    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    if (pathInfo != null) {
      sb.append(pathInfo);
    }

    String scriptPath = sb.toString();

    Path path = pwd.lookupChild(scriptPath);

    return path;
  }

  static class CacheEntry {
    private Path _path;
    private boolean _isDirectory;
    private boolean _canRead;
    private long _length;
    private long _lastModified = 0xdeadbabe1ee7d00dL;
    private String _relPath;
    private String _etag;
    private String _lastModifiedString;
    private String _mimeType;

    CacheEntry(Path path, String relPath, String mimeType)
    {
      _path = path;
      _relPath = relPath;
      _mimeType = mimeType;

      init();
    }

    Path getPath()
    {
      return _path;
    }

    boolean canRead()
    {
      return _canRead;
    }

    boolean isDirectory()
    {
      return _isDirectory;
    }

    long getLength()
    {
      return _length;
    }

    String getRelPath()
    {
      return _relPath;
    }

    String getEtag()
    {
      return _etag;
    }

    long getLastModified()
    {
      return _lastModified;
    }

    String getLastModifiedString()
    {
      return _lastModifiedString;
    }

    String getMimeType()
    {
      return _mimeType;
    }

    boolean isModified()
    {
      // don't want to use caching because current caching is too crude
      // XXX: improve caching
      _path.clearStatusCache();

      long lastModified = _path.getLastModified();
      long length = _path.getLength();

      return (lastModified != _lastModified || length != _length);
    }

    private void init()
    {
      long lastModified = _path.getLastModified();
      long length = _path.getLength();

      _lastModified = lastModified;
      _length = length;
      _canRead = _path.canRead();
      _isDirectory = _path.isDirectory();

      StringBuilder sb = new StringBuilder();
      sb.append('"');
      Base64.encode(sb, _path.getCrc64());
      sb.append('"');
      _etag = sb.toString();

      QDate cal = QDate.allocateGmtDate();

      cal.setGMTTime(lastModified);
      _lastModifiedString = cal.printDate();

      QDate.freeGmtDate(cal);

      if (lastModified == 0) {
        _canRead = false;
        _isDirectory = false;
      }
    }
  }
}
