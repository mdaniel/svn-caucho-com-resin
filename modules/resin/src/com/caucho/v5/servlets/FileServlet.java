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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.Base64Util;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.QDate;
import com.caucho.v5.util.RandomUtil;
import com.caucho.v5.vfs.CaseInsensitive;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Serves static files.  The cache headers are automatically set on these
 * files.
 */
@SuppressWarnings("serial")
public class FileServlet extends GenericServlet {
  private static final L10N L = new L10N(FileServlet.class);
  private static final Logger log
    = Logger.getLogger(FileServlet.class.getName());

  private static final EnvironmentLocal<LruCache<String,Cache>> _pathCacheLocal
    = new EnvironmentLocal<LruCache<String,Cache>>();

  private final LruCache<String,Cache> _pathCache;

  private final LruCache<String,Cache> _localCache
    = new LruCache<String,Cache>(16 * 1024);

  private PathImpl _context;
  private WebApp _app;
  private RequestDispatcher _dir;

  private boolean _isCaseInsensitive;
  private boolean _isEnableRange = true;
  private boolean _isGenerateSession;
  private String _characterEncoding;

  public FileServlet()
  {
    SystemManager resin = SystemManager.getCurrent();

    LruCache<String,Cache> pathCache;

    pathCache = _pathCacheLocal.get(resin.getClassLoader());
    if (pathCache == null) {
      pathCache = new LruCache<String,Cache>(256 * 1024);
      _pathCacheLocal.set(pathCache, resin.getClassLoader());
    }

    _pathCache = pathCache;

    _isCaseInsensitive = CaseInsensitive.isCaseInsensitive();
  }

  /**
   * Sets the character encoding.
   */
  public void setCharacterEncoding(String encoding)
  {
    _characterEncoding = encoding;
  }

  /**
   * Flag to disable the "Range" header.
   */
  public void setEnableRange(boolean isEnable)
  {
    _isEnableRange = isEnable;
  }

  /**
   * Flag to generate sessions on requests.
   */
  public void setGenerateSession(boolean isGenerateSession)
  {
    _isGenerateSession = isGenerateSession;
  }

  /**
   * Clears the cache
   */
  /*
  public void clearCache()
  {
    _pathCache.clear();
  }
  */

  /**
   * Removes an entry from the cache
   */
  public void removeCacheEntry(String uri)
  {
    _pathCache.remove(uri);
  }

  @Override
  public void init(ServletConfig conf)
    throws ServletException
  {
    super.init(conf);

    _app = (WebApp) getServletContext();
    _context = _app.getRootDirectory();

    try {
      _dir = _app.getNamedDispatcher("directory");
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }

    String enable = getInitParameter("enable-range");
    if (enable != null && enable.equals("false"))
      _isEnableRange = false;

    String encoding = getInitParameter("character-encoding");
    if (encoding != null && ! "".equals(encoding))
      _characterEncoding = encoding;
  }

  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    RequestCaucho cauchoReq = null;
    HttpServletRequest req;
    HttpServletResponse res;

    if (request instanceof RequestCaucho) {
      cauchoReq = (RequestCaucho) request;
      req = cauchoReq;
    }
    else
      req = (HttpServletRequest) request;

    res = (HttpServletResponse) response;

    boolean isInclude = false;
    String uri;

    uri = (String) req.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
    if (uri != null)
      isInclude = true;
    else
      uri = req.getRequestURI();

    Cache cache = _localCache.get(uri);

    String filename = null;

    String cacheUrl = null;

    if (cache == null) {
      cacheUrl = getCacheUrl(req, uri);

      cache = _pathCache.get(cacheUrl);

      if (cache != null) {
        _localCache.put(uri, cache);
      }
    }

    if (cache == null) {
      StringBuilder cb = new StringBuilder();
      String servletPath;

      if (cauchoReq != null) {
        servletPath = cauchoReq.getPageServletPath();
      }
      else if (isInclude) {
        servletPath = (String) req.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
      }
      else {
        servletPath = req.getServletPath();
      }

      if (servletPath != null) {
        cb.append(servletPath);
      }

      String pathInfo;
      if (cauchoReq != null) {
        pathInfo = cauchoReq.getPagePathInfo();
      }
      else if (isInclude) {
        pathInfo
          = (String) req.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
      }
      else {
        pathInfo = req.getPathInfo();
      }

      if (pathInfo != null) {
        cb.append(pathInfo);
      }

      String relPath = cb.toString();

      if (_isCaseInsensitive) {
        relPath = relPath.toLowerCase(Locale.ENGLISH);
      }
      
      filename = getServletContext().getRealPath(relPath);
      PathImpl path = _context.lookupNative(filename);

      // only top-level requests are checked
      if (cauchoReq == null || cauchoReq.getRequestDepth(0) != 0) {
      }
      else if (relPath.regionMatches(true, 0, "/web-inf", 0, 8)
               && (relPath.length() == 8
                   || ! Character.isLetterOrDigit(relPath.charAt(8)))) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      else if (relPath.regionMatches(true, 0, "/meta-inf", 0, 9)
               && (relPath.length() == 9
                   || ! Character.isLetterOrDigit(relPath.charAt(9)))) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (relPath.endsWith(".DS_store")) {
        // MacOS-X security hole with trailing '.'
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      else if (! CauchoUtil.isWindows() || relPath.length() == 0) {
      }
      else if (path.isDirectory()) {
      }
      else if (path.isWindowsInsecure()) {
        // Windows security issues with trailing '.'
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      // A null will cause problems.
      for (int i = relPath.length() - 1; i >= 0; i--) {
        char ch = relPath.charAt(i);

        if (ch == 0) {
          res.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
      }

      ServletContext webApp = getServletContext();

      String mimeType = webApp.getMimeType(relPath);

      boolean isPathReadable = path.canRead();
      PathImpl jarPath = null;

      if (! isPathReadable) {
        String resource = "META-INF/resources" + relPath;
        URL url = webApp.getClassLoader().getResource(resource);

        if (url != null)
          jarPath = VfsOld.lookup(url);
      }

      cache = new Cache(path, jarPath, relPath, mimeType);
      _localCache.put(uri, cache);

      _pathCache.put(cacheUrl, cache);
    }
    else if (cache.isModified()) {
      cache = new Cache(cache.getFilePath(),
                        cache.getJarPath(),
                        cache.getRelPath(),
                        cache.getMimeType());

      cacheUrl = getCacheUrl(req, uri);
      _pathCache.put(cacheUrl, cache);
      _localCache.put(uri, cache);
    }

    if (_isGenerateSession)
      req.getSession(true);

    if (cache.isDirectory()) {
      if (! uri.endsWith("/")) {
        String queryString = req.getQueryString();

        if (queryString != null)
          sendRedirect(res, uri + "/?" + queryString);
        else
          sendRedirect(res, uri + "/");
      }
      else if (_dir != null)
        _dir.forward(req, res);
      else
        res.sendError(HttpServletResponse.SC_NOT_FOUND);

      return;
    }

    if (! cache.canRead()) {
      if (isInclude) {
        throw new FileNotFoundException(uri);
      }
      else {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
      return;
    }

    // server/4500, #4218
    String method = req.getMethod();
    if (! method.equalsIgnoreCase("GET")
        && ! method.equalsIgnoreCase("HEAD")
        && ! method.equalsIgnoreCase("POST")) {
      res.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "Method not implemented");
      return;
    }

    String ifMatch = req.getHeader("If-None-Match");
    String etag = cache.getEtag();

    if (ifMatch != null && ifMatch.equals(etag)) {
      res.addHeader("ETag", etag);
      res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String lastModified = cache.getLastModifiedString();

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

        isModified = (ifModifiedTime == 0
                      || ifModifiedTime != cache.getLastModified());
      }

      if (! isModified) {
        if (etag != null)
          res.addHeader("ETag", etag);
        res.sendError(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }
    }

    res.addHeader("ETag", etag);
    res.addHeader("Last-Modified", lastModified);

    if (_isEnableRange && cauchoReq != null && cauchoReq.isTop())
      res.addHeader("Accept-Ranges", "bytes");

    if (_characterEncoding != null)
      res.setCharacterEncoding(_characterEncoding);

    String mime = cache.getMimeType();
    if (mime != null)
      res.setContentType(mime);

    if (method.equalsIgnoreCase("HEAD")) {
      res.setContentLengthLong(cache.getLength());
      return;
    }

    if (_isEnableRange) {
      String range = req.getHeader("Range");

      if (range != null) {
        String ifRange = req.getHeader("If-Range");

        if (ifRange != null && ! ifRange.equals(etag)) {
        }
        else if (handleRange(req, res, cache, range, mime))
          return;
      }
    }

    if (res instanceof ResponseCaucho) {
      ResponseCaucho cRes = (ResponseCaucho) res;

      cRes.setContentLengthLong(cache.getLength());

      cRes.getResponseStream().sendFile(cache.getPath(),
                                        0,
                                        cache.getLength());
    }
    else {
      long length = cache.getLength();

      res.setContentLengthLong(length);

      OutputStream os = res.getOutputStream();
      cache.getPath().writeToStream(os);
    }
  }

  private String getCacheUrl(HttpServletRequest req, String uri)
  {
    WebApp webApp = (WebApp) req.getServletContext();
    return webApp.getId() + "|" + uri;
  }

  private void sendRedirect(HttpServletResponse res, String url)
    throws IOException
  {
    String encUrl;

    ResponseServlet resImpl = null;

    if (res instanceof ResponseServlet) {
      resImpl = (ResponseServlet) res;

      encUrl = resImpl.encodeAbsoluteRedirect(url);
    }
    else
      encUrl = res.encodeRedirectURL(url);

    try {
      res.reset();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    res.setHeader("Location", encUrl);
    res.setContentType("text/html; charset=utf-8");

    PrintWriter out = res.getWriter();

    out.println("The URL has moved <a href=\"" + encUrl + "\">here</a>");

    if (resImpl != null)
      resImpl.close();
  }

  private boolean handleRange(HttpServletRequest req,
                              HttpServletResponse res,
                              Cache cache,
                              String range,
                              String mime)
    throws IOException
  {
    // This is duplicated in CacheInvocation.  Possibly, it should be
    // completely removed although it's useful even without caching.
    int length = range.length();

    boolean hasMore = range.indexOf(',') > 0;

    long cacheLength = cache.getLength();
    long bytesMax = 2 * cacheLength;
    long bytesWritten = 0;

    int head = 0;
    boolean isFirstChunk = true;
    String boundary = null;
    int off = range.indexOf("bytes=", head);
    ServletOutputStream os = null;

    if (off < 0)
      return false;

    off += 6;

    while (off > 0 && off < length) {
      boolean hasFirst = false;
      long first = 0;
      boolean hasLast = false;
      long last = 0;
      int ch = -1;

      // Skip whitespace
      for (; off < length && (ch = range.charAt(off)) == ' '; off++) {
      }

      // read range start (before '-')
      for (;
           off < length && (ch = range.charAt(off)) >= '0' && ch <= '9';
           off++) {
        first = 10 * first + ch - '0';
        hasFirst = true;
      }

      if (length <= off && ! isFirstChunk)
        break;
      else if (ch != '-')
        return false;

      // read range end (before '-')
      for (off++;
           off < length && (ch = range.charAt(off)) >= '0' && ch <= '9';
           off++) {
        last = 10 * last + ch - '0';
        hasLast = true;
      }

      // #3766 - browser errors in range
      if (off < length && ch != ' ' && ch != ',')
        return false;

      // Skip whitespace
      for (; off < length && (ch = range.charAt(off)) == ' '; off++) {
      }

      head = off;
      if (! hasLast) {
        if (first == 0)
          return false;

        last = cacheLength - 1;
      }

      // suffix
      if (! hasFirst) {
        first = cacheLength - last;
        last = cacheLength - 1;
      }

      if (last < first)
        break;

      if (cacheLength <= last) {
        // XXX: actually, an error
        break;
      }
      res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
      StringBuilder cb = new StringBuilder();
      cb.append("bytes ");
      cb.append(first);
      cb.append('-');
      cb.append(last);
      cb.append('/');
      cb.append(cacheLength);
      String chunkRange = cb.toString();

      bytesWritten += last - first + 1;
      if (bytesMax <= bytesWritten) {
        String msg;

        msg = L.l("{0} too many range bytes requested {1} for uri={2} IP={3}",
                  this,
                  bytesWritten,
                  req.getRequestURL(),
                  req.getRemoteAddr());

        log.warning(msg);

        if (msg != null)
          throw new IOException(msg);

      }

      if (hasMore) {
        if (isFirstChunk) {
          StringBuilder cb1 = new StringBuilder();

          cb1.append("--");
          Base64Util.encode(cb1, RandomUtil.getRandomLong());
          boundary = cb1.toString();

          res.setContentType("multipart/byteranges; boundary=" + boundary);
          os = res.getOutputStream();
        }
        else {
          os.write('\r');
          os.write('\n');
        }

        isFirstChunk = false;

        os.write('-');
        os.write('-');
        os.print(boundary);
        os.print("\r\nContent-Type: ");
        os.print(mime);
        os.print("\r\nContent-Range: ");
        os.print(chunkRange);
        os.write('\r');
        os.write('\n');
        os.write('\r');
        os.write('\n');
      }
      else {
        res.setContentLengthLong((last - first + 1));

        res.addHeader("Content-Range", chunkRange);
      }

      ReadStream is = null;
      try {
        is = cache.getPath().openRead();
        is.skip(first);

        os = res.getOutputStream();
        is.writeToStream(os, (int) (last - first + 1));
      } finally {
        if (is != null)
          is.close();
      }

      for (off--; off < length && range.charAt(off) != ','; off++) {
      }

      off++;
    }

    if (hasMore) {
      os = res.getOutputStream();

      os.write('\r');
      os.write('\n');
      os.write('-');
      os.write('-');
      os.print(boundary);
      os.write('-');
      os.write('-');
      os.write('\r');
      os.write('\n');
    }

    return true;
  }

  static class Cache {
    private PathImpl _path;
    private PathImpl _jarPath;
    private PathImpl _pathResolved;
    private boolean _isDirectory;
    private boolean _canRead;
    private long _length;
    private long _lastModified = 0xdeadbabe1ee7d00dL;
    private String _relPath;
    private String _etag;
    private String _lastModifiedString;
    private String _mimeType;

    Cache(PathImpl path, PathImpl jarPath, String relPath, String mimeType)
    {
      _path = path;
      _jarPath = jarPath;
      _relPath = relPath;
      _mimeType = mimeType;

      fillData();
    }

    PathImpl getPath()
    {
      return _pathResolved;
    }

    PathImpl getFilePath()
    {
      return _path;
    }

    PathImpl getJarPath()
    {
      return _jarPath;
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
      long lastModified = _pathResolved.getLastModified();
      long length = _pathResolved.length();

      // server/1t06
      if (_path != _pathResolved && _path.canRead())
        return true;

      return (lastModified != _lastModified || length != _length);
    }

    private void fillData()
    {
      _pathResolved = _path;

      if (_jarPath != null && ! _path.canRead())
        _pathResolved = _jarPath;

      long lastModified = _pathResolved.getLastModified();
      long length = _pathResolved.length();

      _lastModified = lastModified;
      _length = length;
      _canRead = _pathResolved.canRead();
      _isDirectory = _pathResolved.isDirectory();
      
      long etagHash = _pathResolved.getCrc64();
      
      if (_mimeType != null) {
        etagHash = Crc64.generate(etagHash, _mimeType);
      }

      StringBuilder sb = new StringBuilder();
      sb.append('"');
      Base64Util.encode(sb, etagHash);
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
