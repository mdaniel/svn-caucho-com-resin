/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.httpcache;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.server.http.AbstractResponseStream;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.webapp.CauchoFilterChain;
import com.caucho.server.webapp.IncludeResponse;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.Alarm;
import com.caucho.util.Base64;
import com.caucho.util.CacheListener;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.util.Crc64;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.QDate;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.ClientDisconnectException;

/**
 * Represents the proxy cache in a filter chain.
 */
public class ProxyCacheFilterChain extends AbstractCacheFilterChain
  implements CacheListener, CauchoFilterChain
{
  private static final L10N L = new L10N(ProxyCacheFilterChain.class);
  
  private static final Logger log
    = Logger.getLogger(ProxyCacheFilterChain.class.getName());

  private static long MIN_EXPIRES = 5000;

  private static int MAX_VARY = 8;

  private ProxyCache _cache;

  private FilterChain _next;

  // is this page cacheable
  private boolean _isCacheable = true;
  // time the cache was last checked
  private long _lastCacheCheck;

  // the cached entry
  private final AtomicReference<ProxyCacheEntry> _entryRef
    = new AtomicReference<ProxyCacheEntry>();

  // thread filling the cache
  private final AtomicReference<CauchoResponse> _cacheFillRef
    = new AtomicReference<CauchoResponse>();
  
  private final AtomicReference<ProxyCacheEntry> _cacheFillEntry
    = new AtomicReference<ProxyCacheEntry>();

  private WebApp _webApp;

  // uri/query string for logging
  private String _uri;
  private String _queryString;

  private final AtomicLong _hitCount = new AtomicLong();

  /**
   * Create the filter chain servlet.
   */
  public ProxyCacheFilterChain(FilterChain next,
                          WebApp webApp)
  {
    this(ProxyCache.getLocalCache(), next, webApp);
  }

  /**
   * Create the filter chain servlet.
   */
  public ProxyCacheFilterChain(ProxyCache cache,
                          FilterChain next,
                          WebApp webApp)
  {
    _cache = cache;

    _next = next;
    _webApp = webApp;
  }

  public long getHitCount()
  {
    return _hitCount.get();
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  public String getUri()
  {
    return _uri;
  }

  @Override
  public FilterChain getNext()
  {
    return _next;
  }

  /**
   * Clears the expires timers
   */
  public void clearExpires()
  {
    for (ProxyCacheEntry entry = _entryRef.get();
         entry != null;
         entry = entry._next) {
      entry.clearExpires();
    }
  }

  /**
   * Handles caching for the filter.
   *
   * @param req the servlet request
   * @param res the servlet response
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    if (! isCacheable()) {
      _next.doFilter(request, response);
      return;
    }
    else if (! (request instanceof CauchoRequest)
             || ! (response instanceof CauchoResponse)) {
      _next.doFilter(request, response);
      return;
    }

    CauchoRequest req = (CauchoRequest) request;
    CauchoResponse res = (CauchoResponse) response;

    if (! req.getMethod().equals("GET")
        && ! req.getMethod().equals("HEAD")) {
      _next.doFilter(request, response);
      return;
    }
    
    ProxyCacheEntry entry = getCacheEntry(req);

    long now = CurrentTime.getCurrentTime();
    
    if (entry == null) {
      _cacheFillRef.compareAndSet(null, res);
    }
    else if (now <= entry._expireDate
             || ! _cacheFillRef.compareAndSet(null, res)) {
      if (useCache(req, res, entry)) {
        // If we've got a cached value, try to use it
        _cache.hit();
        return;
      }
    }
    
    _cache.miss();
    
    try {
      if (doRequestCacheable(req, res, entry, now)) {
        res.completeCache();
      } else {
        _next.doFilter(request, response);
      }  
    } catch (ClientDisconnectException e) {
      throw e;
    } finally {
      if (_cacheFillRef.get() == res) {
        ProxyCacheEntry fillEntry = _cacheFillEntry.getAndSet(null);
        
        _cacheFillRef.compareAndSet(res, null);
        
        if (fillEntry != null)
          fillEntry.remove();
      }
    }
  }
  
  private boolean doRequestCacheable(CauchoRequest req,
                                     CauchoResponse res,
                                     ProxyCacheEntry entry,
                                     long now)
    throws ServletException, IOException
  {
    if (_cacheFillRef.get() != res)
      return false;
    else if (req.getWebApp() == null || ! req.getWebApp().isActive() ) {
      // web-app starting: #2474
      return false;
    }
    else if (! _isCacheable && now < _lastCacheCheck + 60000) {
      return false;
    }
    
    _lastCacheCheck = now;

    res.setCacheInvocation(this);

    if (entry != null && entry.isNotModifiedAllowed(req)) {
      ProxyCacheRequest cacheRequest
        = new ProxyCacheRequest(req, entry);
      ProxyCacheResponse cacheResponse
        = new ProxyCacheResponse(cacheRequest, res, this);

      AbstractResponseStream rs = res.getResponseStream();

      try {
        rs.setCauchoResponse(cacheResponse);

        _next.doFilter(cacheRequest, cacheResponse);
      } finally {
        rs.setCauchoResponse(res);
        
        cacheResponse.close();
      }
    }
    else {
      // If the request caches its results, it will call startCaching.
      _next.doFilter(req, res);
    }

    // If we're caching, force a flush
    // res.close();

    return true;
  }

  /**
   * Returns true if the entry is cacheable.
   */
  final boolean isCacheable()
  {
    if (! _cache.isEnable())
      return false;
    else if (_isCacheable)
      return true;
    else if (_lastCacheCheck + 60000 < CurrentTime.getCurrentTime())
      return true;
    else {
      return false;
    }
  }
  
  final boolean isCached()
  {
    return _entryRef.get() != null;
  }

  private ProxyCacheEntry getCacheEntry(CauchoRequest req)
  {
    ProxyCacheEntry entry = _entryRef.get();
    // If the entry isn't valid, don't use it
    if (entry == null) {
      return null;
    }

    // Mark this entry as used for the LRU
    if (! _cache.useEntry(this)) {
      return null;
    }

    entry = getVaryEntry(entry, req);

    // If the entry isn't valid, don't use it
    if (entry == null || ! entry._isValid) {
      return null;
    }

    // Check if anonymous caching is allowed
    if (entry._varyCookies
        && (req.getHeader("Cookie") != null
            || req.isRequestedSessionIdFromURL())) {
      return null;
    }

    // If the webApp is reloading, can't use the cache
    if (_webApp.isModified()) {
      return null;
    }
    
    return entry;
  }
  /**
   * Tries to use the cache.  If the response hasn't expired,
   * send it directly to the user.  Otherwise, set If-Modified-Since and
   * If-None-Match so the servlet can bypass processing.
   *
   * @param req the servlet request
   * @param res the servlet response
   *
   * @return true if the request has completed because the cache has filled
   *   the request
   */
  private boolean useCache(CauchoRequest req,
                           CauchoResponse res,
                           ProxyCacheEntry entry)
    throws IOException, ServletException
  {
    if (fillNotModified(req, res, entry)) {
      return true;
    }
    
    if (fillFromCache(req, res, entry)) {
      return true;
    }
 
    return false;
  }

  /**
   * fillNotModified tries to return a 304 not modified from the result.
   *
   * @param req the servlet request trying to get data from the cache
   * @param response the servlet response which will receive data
   * @param entry the cache entry to use
   * @param isSyntheticHeader if false, the not-modified should be sent to the browser
   */
  private boolean fillNotModified(CauchoRequest req,
                                  CauchoResponse response,
                                  ProxyCacheEntry entry)
    throws IOException
  {
    if (! entry._isValid)
      return false;
    
    ResetResponse reset = resetResponse(entry, response);

    if (reset == ResetResponse.OK) {
    }
    else if (reset == ResetResponse.FAIL) { 
      log.warning(this + " unable to reset " + req + " " + response);
      
      return false;
    }
    else {
      // #4472
      log.finer(this + " unable to reset " + reset + " " + req + " " + response);
      
      return false;
    }

    if (! isRequestNotModified(entry, req, response)) {
      return false;
    }

    _hitCount.incrementAndGet();

    if (log.isLoggable(Level.FINE)) {
      String debugURI = _uri;

      if (_queryString != null)
        debugURI = debugURI + "?" + _queryString;

      log.fine("not-modified: " + debugURI);
    }

    long now = CurrentTime.getCurrentTime();

    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    
    if (entry._etag != null)
      response.setHeader("ETag", entry._etag);
    
    if (entry._lastModified != null)
      response.setHeader("Last-Modified", entry._lastModified);

    if (entry._maxAge > 0) {
      response.setDateHeader("Expires", now + entry._maxAge);
      response.addHeader("Cache-Control", "max-age=" + entry._maxAge / 1000);
      
      entry._expireDate = now + entry._maxAge;
    }
    
    if (entry._sMaxAge > 0) {
      response.addHeader("Cache-Control", "s-maxage=" + entry._sMaxAge/ 1000);
    }

    return true;
  }

  /**
   * fillFromCache is called when the client needs the entire result, and
   * the result is already in the cache.
   *
   * @param req the servlet request trying to get data from the cache
   * @param response the servlet response which will receive data
   * @param entry the cache entry to use
   * @param isSyntheticHeader if false, the not-modified should be sent to the browser
   */
  @Override
  public boolean fillFromCache(CauchoRequest req,
                               CauchoResponse response,
                               AbstractCacheEntry abstractEntry)
    throws IOException
  {
    ProxyCacheEntry entry = (ProxyCacheEntry) abstractEntry;

    if (! entry._isValid)
      return false;

    if (resetResponse(entry, response) == ResetResponse.FAIL) {
      log.warning(this + " unable to reset " + req + " " + response);
      
      return false;
    }

    _hitCount.incrementAndGet();

    if (log.isLoggable(Level.FINE)) {
      String debugURI = _uri;

      if (_queryString != null)
        debugURI = debugURI + "?" + _queryString;

      log.fine("using cache: " + debugURI + " from " + req);
    }

    long now = CurrentTime.getCurrentTime();

    String range = req.getHeader("Range");
    
    if (range != null && ! entry._allowRange)
      return false;
    
    if (entry._contentType != null)
      response.setContentType(entry._contentType);

    if (entry._charEncoding != null)
      response.setCharacterEncoding(entry._charEncoding);

    ArrayList<String> headerKeys = entry._headerKeys;
    ArrayList<String> headerValues = entry._headerValues;
    for (int i = 0; headerKeys != null && i < headerKeys.size(); i++) {
      response.addHeader(headerKeys.get(i), headerValues.get(i));
    }

    if (entry._hasExpires) {
      response.addHeader("Expires", entry._expireString);
    }
    else if (entry._maxAge > 0) {
      // HTTP/1.0 browsers should be gone
      response.addDateHeader("Expires", now + entry._maxAge);
    }

    String method = req.getMethod();
    if (method != null && method.equals("HEAD")) {
      response.setContentLength((int) entry._contentLength);

      return true;
    }

    ProxyCacheInode inode = entry.getInode();
    if (inode == null || ! inode.allocate()) {
      return false;
    }

    try {
      if (range != null) {
        String ifRange = req.getHeader("If-Range");
        
        if (ifRange != null && ! ifRange.equals(entry._etag)) {
        }
        else if (handleRange(req, response,
                             entry, inode, range,
                             entry._mimeType)) {

          return true;
        }
      }

      if (entry._isChar) {
        inode.writeToWriter(response.getWriter());
      }
      else {
        // XXX: need qa for this
        response.setCacheInvocation(null);
        response.killCache();
        response.setContentLength((int) entry._contentLength);

        // inode may be cleared.
        if (! inode.writeToStream(response.getResponseStream())) {
          return false;
        }
      }

      if (entry.isForwardEnclosed()) {
        flushBuffer(response);
      }
    } catch (ClientDisconnectException e) {
      throw e;
    } catch (InterruptedIOException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      /*
      log.warning("cache disabled due to " + e.toString());
      _cache.setEnable(false);
      */

      entry._isValid = false;

      try {
        req.killKeepalive("proxy cache exception: " + e);
        // response.reset();
      } catch (Throwable e1) {
        log.log(Level.FINE, e.toString(), e);
      }

      return true;
    } finally {
      inode.free();
    }

    return true;
  }

  private ResetResponse resetResponse(ProxyCacheEntry entry,
                                      CauchoResponse response)
  {
    try {
      // server/2h71, 2h79
     response.reset();

      ServletResponse resp = response;

      while (resp != null) {
        if (resp instanceof ProxyCacheResponse) {
          return ResetResponse.OK;
        }
        
        if (entry.isForwardEnclosed()) {
          // jsp/15ma
          resp.resetBuffer();
        }

        else if (resp instanceof IncludeResponse) {
          // server/2h71, 183d
          // return false;
          return ResetResponse.INCLUDE;
        }

        if (resp instanceof CauchoResponse)
          resp = ((CauchoResponse)resp).getResponse();
        else
          resp = null;
      }
      
      return ResetResponse.OK;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return ResetResponse.FAIL;
    }
  }
  
  /**
   * See if the request is asking for a NOT_MODIFIED response. 
   */
  private boolean isRequestNotModified(ProxyCacheEntry entry,
                                       CauchoRequest req,
                                       CauchoResponse response)
  {
    if (entry._etag != null) {
      String value = req.getHeader("if-none-match");

      if (value != null) {
        int i = value.indexOf(';');
        
        if (i >= 0)
          value = value.substring(0, i);
        
        if (value != null && value.equals(entry._etag)) {
          return true;
        }
      }
    }

    if (entry._lastModified != null) {
      String value = req.getHeader("if-modified-since");

      if (value != null) {
        int semicolon = value.indexOf(';');
        int plus = value.indexOf('+');

        if (plus > 0)
          value = value.substring(0, plus);
        else if (semicolon > 0)
          value = value.substring(0, semicolon);

        if (entry._lastModified.startsWith(value)) {
          return true;
        }
      }
    }
    
    return false;
  }

  private void flushBuffer(CauchoResponse response)
    throws IOException
  {
    response.flushBuffer();
    response.close();

    ServletResponse resp = response.getResponse();

    while (resp != null) {
      if (resp instanceof CauchoResponse) {
        CauchoResponse cr = (CauchoResponse)resp;
        cr.flushBuffer();
        cr.close();
        resp = cr.getResponse();
      }
      else {
        resp.flushBuffer();
        resp.getOutputStream().close();

        resp = null;
      }
    }
  }

  /**
   * This is a duplicate of FileServlet, unfortunately.
   */
  private boolean handleRange(HttpServletRequest req,
                              CauchoResponse res,
                              ProxyCacheEntry cache,
                              ProxyCacheInode inode,
                              String range,
                              String mime)
    throws IOException
  {
    // This is duplicated in CacheInvocation.  Possibly, it should be
    // completely removed although it's useful even without caching.
    int length = range.length();
    
    long cacheLength = cache._contentLength;
    long bytesMax = 2 * cacheLength;
    long bytesWritten = 0;

    boolean hasMore = range.indexOf(',') > 0;

    int head = 0;
    ServletOutputStream os = res.getOutputStream();
    boolean isFirstChunk = true;
    String boundary = null;
    int off = range.indexOf("bytes=", head);

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
        return false;

      if (cacheLength <= last) {
        // XXX: actually, an error
        return false;
      }

      res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

      String chunkRange = ("bytes " + first + '-' + last + '/' + cacheLength);

      if (hasMore) {
        if (isFirstChunk) {
          CharBuffer cb1 = new CharBuffer();

          cb1.append("--");
          Base64.encode(cb1, RandomUtil.getRandomLong());
          boundary = cb1.toString();

          res.setContentType("multipart/byteranges; boundary=" + boundary);
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
        res.setContentLength((int) (last - first + 1));

        res.addHeader("Content-Range", chunkRange);
      }
      
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

      inode.writeToStream(res.getResponseStream(), first, last - first + 1);

      for (off--; off < length && range.charAt(off) != ','; off++) {
      }

      off++;
    }

    if (hasMore) {
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

  /**
   * Starts the caching after the headers have been sent.
   *
   * @param req the servlet request
   * @param req the servlet response
   * @param keys the saved header keys
   * @param values the saved header values
   * @param contentType the response content type
   * @param charEncoding the response character encoding
   *
   * @return the inode to store the cache value or null if
   *         uncacheable.
   */
  @Override
  public AbstractCacheEntry startCaching(CauchoRequest req,
                                         CauchoResponse res,
                                         ArrayList<String> keys,
                                         ArrayList<String> values,
                                         String contentType,
                                         String charEncoding,
                                         long contentLength)
  {
    CauchoResponse fillResponse = _cacheFillRef.get();

    if (fillResponse != res) {
      return null;
    }

    if (! isRequestCacheable(req, contentLength)) {
      _cacheFillRef.set(null);
      
      return null;
    }

    _cacheFillEntry.set(fillEntry(req, res, keys, values, 
                                  contentType, charEncoding, contentLength));

    /*
    if (false && entry == null) {
      // server/2713, server/2h81
      // killCache();

      ProxyCacheEntry oldEntry = _entryRef.getAndSet(null);
      _cacheFillRef.set(null);

      if (oldEntry != null)
        oldEntry.remove();
    }
    */

    return _cacheFillEntry.get();
  }

  private boolean isRequestCacheable(CauchoRequest req,
                                     long contentLength)
  {
    ProxyCacheEntry oldEntry = _entryRef.get();

    if (! "GET".equals(req.getMethod())) {
      if (! "HEAD".equals(req.getMethod()))
        killCache();

      return false;
    }

    else if (_cache.getMaxEntrySize() < contentLength) {
      killCache();

      return false;
    }
    else if (oldEntry != null
             && oldEntry._varyCookies
             && req.getHeader("Cookie") != null) {
      // server/13wi
      return false;
    }
    else
      return true;
  }
  
  
  public ProxyCacheEntry fillEntry(CauchoRequest req,
                                   CauchoResponse res,
                                   ArrayList<String> keys,
                                   ArrayList<String> values,
                                   String contentType,
                                   String charEncoding,
                                   long contentLength)
  {
    ProxyCacheEntry entry = new ProxyCacheEntry(_cache);
    
    _uri = req.getPageURI();
    _queryString = req.getPageQueryString();

    long now = CurrentTime.getCurrentTime();
    String expires = null;
    String lastModified = null;
    String etag = null;
    boolean isPrivate = false;

    ArrayList<String> vary = null;

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      String value = values.get(i);

      if (key.equalsIgnoreCase("cache-control")) {
        if (! processCacheControl(value, entry)) {
          killCache();
          
          return null;
        }
      }
      else if (key.equalsIgnoreCase("Vary")) {
        vary = processVary(value, vary, entry, req);
        
        if (vary == null) {
          return null;
        }

        if (_cache.isRewriteVaryAsPrivate() && ! entry._varyCookies) {
          keys.set(i, "Cache-Control");
          values.set(i, "private");
          isPrivate = true;
        }
      }
      else if (key.equalsIgnoreCase("Content-Encoding")) {
        entry._allowRange = false;
      }
      else if (value.equalsIgnoreCase("Set-Cookie")
               || value.equalsIgnoreCase("Set-Cookie2")) {
        keys.remove(i);
        values.remove(i);
        i--;
      }
      else if (key.equalsIgnoreCase("expires")) {
        expires = value;
      }
      else if (key.equalsIgnoreCase("last-modified")) {
        lastModified = value;
      }
      else if (key.equalsIgnoreCase("etag")) {
        etag = value;
      }
    }

    // server/1k67
    boolean isNoCacheUnlessVary = res.isNoCacheUnlessVary();

    if (isNoCacheUnlessVary && vary == null) {
      killCache();
      
      return null;
    }

    // server/2h0n, #3065
    if (req.getHeader("Authorization") != null && ! isPrivate) {
      res.addHeader("Cache-Control", "private");
    }

    entry._vary = vary;

    long crc = getVaryCRC(vary, req);

    entry._varyCrc = crc;

    long expireDate = -1;
    if (entry._sMaxAge > 0) {
      entry._maxAge = entry._sMaxAge;
      entry._resinMaxAge = entry._sMaxAge;

      entry._expireDate = now + entry._sMaxAge;
      // XXX: if adding Expires, need separate date
    }
    else if (entry._maxAge > 0) {
      // entry._maxAge = maxAge;
      entry._resinMaxAge = entry._maxAge;
      entry._expireDate = now + entry._maxAge;
    }

    if (expires != null) {
      if (! fillExpireDate(entry, expires, now)) {
        return null;
      }
    }
    else if (now < entry._expireDate) {
      res.setDateHeader("Expires", entry._expireDate);
    }

    entry._lastModified = lastModified;

    entry._etag = etag;

    entry._mimeType = res.getHeader("Content-Type");

    // If we're caching, cache without checking for 5 seconds
    if (expireDate == -1 && (etag != null || lastModified != null)) {
      if (entry._maxAge <= 0) {
        fillMaxAge(entry, _uri, req, res);
      }

      // server/1319
      if (entry._maxAge > 0) { // && ! req.getVaryCookies())
        res.setDateHeader("Expires", now + entry._maxAge);
      }

      // Only cache internally for 5 seconds so we see updates
      if (entry._resinMaxAge > 0)
        entry._expireDate = now + entry._resinMaxAge;
      else if (entry._sMaxAge > 0)
        entry._expireDate = now + entry._sMaxAge;
      else if (entry._maxAge > 0)
        entry._expireDate = now + entry._maxAge;
    }

    if (entry._expireDate - 1000 < now
        && entry._lastModified == null && entry._etag == null) {
      killCache();
      
      return null;
    }

    /*server/131z
    if (entry._varyCookies)
      res.setNoCache(true);
    */

    // Store the headers so cached entries will be correct
    entry._headerKeys = new ArrayList<String>();
    entry._headerValues = new ArrayList<String>();

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);

      if (key.equalsIgnoreCase("expires")
          || key.equalsIgnoreCase("set-cookie")
          || key.equalsIgnoreCase("set-cookie2")) {
        // hide cookies and expires
      }
      else {
        entry._headerKeys.add(key);
        entry._headerValues.add(values.get(i));
      }
    }

    if (contentType != null)
      entry._contentType = contentType;
    /* server/137t
    else if (charEncoding != null)
      entry._contentType = "text/html; charset=" + charEncoding;
    */

    if (charEncoding != null)
      entry._charEncoding = charEncoding;

    entry._inode = _cache.createInode();
    _isCacheable = true;

    return entry;
  }
  
  private boolean processCacheControl(String value,
                                      ProxyCacheEntry entry)
  {
    int length = value.length();
    int head = 0;

    while (head < length) {
      char ch;

      for (;
           head < length
           && ((ch = value.charAt(head)) == ' ' || ch == ',' || ch == ';');
           head++) {
      }

      if (value.startsWith("public", head)) {
      }

      else if (value.startsWith("must-revalidate", head)
              || value.startsWith("proxy-revalidate", head)) {
        // server/137h
        // internalCacheTimeout = -1;
        return false;
      }

      else if (value.startsWith("max-age", head)) {
        entry._maxAge = parseMaxAge(value, head, entry._maxAge);
        
        if (entry._maxAge < 0)
          return false;
      }
      else if (value.startsWith("s-maxage", head)) {
        entry._sMaxAge = parseMaxAge(value, head, entry._sMaxAge);
        
        if (entry._sMaxAge < 0)
          return false;
      }

      else {
        return false;
      }

      for (;
           head < length
           && (ch = value.charAt(head)) != ' ' && ch != ',' && ch != ';';
           head++) {
      }
    }
    
    return true;
  }
  
  private ArrayList<String> processVary(String value,
                                        ArrayList<String> vary,
                                        ProxyCacheEntry entry,
                                        CauchoRequest req)
  {
    if (vary == null)
      vary = new ArrayList<String>();

    int length = value.length();
    int head = 0;
    char ch = ' ';

    while (head < length) {
      for (; head < length
           && ((ch = value.charAt(head)) == ' ' || ch == ',' || ch == ';');
           head++) {
      }

      if (length <= head)
        break;

      int tail = head;
      for (; tail < length
           && ((ch = value.charAt(tail)) != ' ' && ch != ',' && ch != ';'
           && ch != '=');
           tail++) {
      }

      if (head < tail) {
        String varyValue = value.substring(head, tail);

        if (varyValue.equalsIgnoreCase("cookie")) {
          entry._varyCookies = true;

          if (req.getHeader("Cookie") != null) {
            // server/13wj
            _cacheFillRef.set(null);

            return null;
          }
        }
        else
          vary.add(varyValue);
      }

      if (ch == '=') {
        for (; tail < length
               && ((ch = value.charAt(tail)) != ' ' && ch != ',' && ch != ';');
             tail++) {
        }
      }

      head = tail;
    }
    
    return vary;
  }

  private long parseMaxAge(String value,
                           int head, 
                           long maxAge)
  {
    int p = value.indexOf('=', head);

    if (p > 0) {
      int sign = 1;
      int age = 0;
      for (p++; p < value.length(); p++) {
        int ch = value.charAt(p);

        if (ch == '-') {
          sign = -1;
        }
        else if ('0' <= ch && ch <= '9') {
          age = age * 10 + ch - '0';
        }
        else
          break;
      }

      return 1000L * sign * age;
    }
    
    return maxAge;
  }

  private boolean fillExpireDate(ProxyCacheEntry entry, 
                                 String expires,
                                 long now)
  {
    QDate calendar = QDate.allocateLocalDate();

    try {
      long expireDate = calendar.parseDate(expires);
      entry._hasExpires = true;
      entry._expireString = expires;
      entry._expireDate = expireDate;

      if (expireDate < now) {
        killCache();

        ProxyCacheEntry oldEntry = _entryRef.getAndSet(null);
        _cacheFillRef.set(null);

        if (oldEntry != null)
          oldEntry.remove();

        return false;
      }

      entry._expireDate = expireDate;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  
    QDate.freeLocalDate(calendar);
    
    return true;
  }

  private void fillMaxAge(ProxyCacheEntry entry, String uri,
                          CauchoRequest req,
                          CauchoResponse res)
  {
    AbstractHttpResponse absRes = res.getAbstractHttpResponse();

    WebApp webApp = req.getWebApp();

    if (webApp != null && uri != null) {
      entry._maxAge = webApp.getMaxAge(uri);
      entry._sMaxAge = webApp.getSMaxAge(uri);
    }

    if (entry._maxAge > 0 && entry._maxAge < MIN_EXPIRES)
      entry._maxAge = MIN_EXPIRES;

    long internalCacheTimeout = 5000L;

    /*
    if (entry._expireString == null &&
        entry._maxAge == Long.MIN_VALUE && entry._sMaxAge == Long.MIN_VALUE)
      entry._maxAge = internalCacheTimeout;
    */

    if (absRes == null) {
      // server/2hh2
    }
    else if (internalCacheTimeout <= 0) {
    }
    else if (entry._maxAge > 0 && entry._sMaxAge > 0) {
      absRes.addHeaderImpl("Cache-Control",
                        "max-age=" + entry._maxAge / 1000L +
                        ", s-maxage=" + entry._sMaxAge / 1000L);
    }
    else if (entry._maxAge > 0) {
      absRes.addHeaderImpl("Cache-Control", "max-age=" + entry._maxAge / 1000L);
    }
    else if (entry._sMaxAge > 0) {
      absRes.addHeaderImpl("Cache-Control", "s-maxage=" + entry._sMaxAge / 1000L);
    }

    if (entry._sMaxAge > 0)
      entry._resinMaxAge = entry._sMaxAge;
    else if (entry._maxAge > 0)
      entry._resinMaxAge = entry._maxAge;
    else
      entry._resinMaxAge = internalCacheTimeout;
  }

  void killCache()
  {
    _isCacheable = false;
  }

  void killCacheDisconnect()
  {
    _lastCacheCheck = 0;
    _isCacheable = false;
  }

  void disableCache()
  {
    ProxyCache cache = _cache;

    if (cache != null) {
      cache.setEnable(false);
      cache.clear();
    }
  }

  /**
   * Returns the vary entry matching the crc.
   */
  private ProxyCacheEntry getVaryEntry(ProxyCacheEntry entry, 
                                       HttpServletRequest req)
  {
    ArrayList<String> vary = entry._vary;

    if (vary == null)
      return entry;

    long crc = getVaryCRC(vary, req);

    for (; entry != null; entry = entry._next) {
      if (entry._varyCrc == crc)
        return entry;
    }

    return null;
  }

  /**
   * Returns the vary crc.
   */
  private long getVaryCRC(ArrayList<String> vary, HttpServletRequest req)
  {
    if (vary == null)
      return 0;

    long crc = 0;

    if (req instanceof HttpServletRequestImpl) {
      AbstractHttpRequest cauchoReq
        = ((HttpServletRequestImpl) req).getAbstractHttpRequest();

      for (int i = 0; i < vary.size(); i++) {
        String key = vary.get(i);
        CharSegment value = cauchoReq.getHeaderBuffer(key);

        if (value != null)
          crc = Crc64.generate(crc,
                               value.getBuffer(),
                               value.getOffset(),
                               value.getLength());
      }
    }
    else {
      for (int i = 0; i < vary.size(); i++) {
        String key = vary.get(i);
        String value = req.getHeader(key);

        if (value != null)
          crc = Crc64.generate(crc, value);
      }
    }

    return crc;
  }

  /**
   * Update the headers when the caching has finished.
   *
   * @param okay if true, the cache if valid
   */
  @Override
  public void finishCaching(CauchoResponse res)
  {
    if (_cacheFillRef.get() != res) {
      // throw new IllegalStateException(L.l("{0} multiple finish caching calls",
      //                                     this));
      return;
    }
    
    ProxyCacheEntry loadingEntry = _cacheFillEntry.get();
    
    if (loadingEntry == null)
      return;
    
    ProxyCacheInode inode;

    if ((inode = loadingEntry._inode) != null) {
      long contentLength = inode.getLength();

      loadingEntry._contentLength = contentLength;
      loadingEntry._isValid = true;

      if (contentLength <= 0) {
        log.fine(this + " tried to cache blank page");

        loadingEntry._isValid = false;
        killCache();
      }
      else if (contentLength < _cache.getMaxEntrySize()) {
        ProxyCacheEntry oldEntry = _entryRef.get();
        ProxyCacheEntry newEntry = replaceEntry(oldEntry, loadingEntry); 
        _entryRef.compareAndSet(oldEntry, newEntry);
        
        loadingEntry = null;
        _cacheFillEntry.set(null);

        // Mark this entry as used for the LRU
        _cache.activateEntry(this);
      }
      else
        killCache();
    }

    if (loadingEntry != null) {
      try {
        loadingEntry.remove();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    if (log.isLoggable(Level.FINE) && _entryRef.get() != null)
      logCache(_entryRef.get());
  }

  /**
   * Update the headers when the caching has finished.
   *
   * @param okay if true, the cache if valid
   */
  @Override
  public void killCaching(CauchoResponse res)
  {
    if (_cacheFillRef.get() != res)
      return;
    
    ProxyCacheEntry loadingEntry = _cacheFillEntry.getAndSet(null);
    
    if (loadingEntry != null) {
      killCache();

      try {
        loadingEntry.destroy();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Replaces the entry with the matching CRC.
   */
  private ProxyCacheEntry replaceEntry(ProxyCacheEntry head,
                                       ProxyCacheEntry next)
  {
    int count = 0;

    ProxyCacheEntry prev = null;
    for (ProxyCacheEntry ptr = head; ptr != null; ptr = ptr._next) {
      if (ptr._varyCrc == next._varyCrc) {
        next._next = ptr._next;

        ptr.remove();

        if (prev != null) {
          prev._next = next;
          return head;
        }
        else
          return next;
      }

      count++;
      prev = ptr;
    }

    next._next = head;

    if (MAX_VARY < count) {
      ProxyCacheEntry ptr = head;
      for (; ptr != null && count > 0; ptr = ptr._next) {
        count--;
      }

      if (ptr != null)
        ptr._next = null;
    }

    return next;
  }

  /**
   * Callback when the item has been removed from the cache, usually
   * because of an LRU event.
   */
  @Override
  public void removeEvent()
  {
    ProxyCacheEntry entry;

    entry = _entryRef.getAndSet(null);

    if (log.isLoggable(Level.FINE) && entry != null) {
      String debugURI = _uri;
      if (_queryString != null)
        debugURI += "?" + _queryString;

      log.fine("lru removing cache: " + debugURI);
    }

    for (; entry != null; entry = entry._next) {
      try {
        entry.remove();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Logs the cache activity.
   */
  private void logCache(ProxyCacheEntry entry)
  {
    CharBuffer extra = new CharBuffer();

    extra.append(_uri);
    if (_queryString != null) {
      extra.append("?");
      extra.append(_queryString);
    }

    if (entry._varyCrc != 0) {
      extra.append(" crc=");
      extra.append(entry._varyCrc);
    }

    if (entry._varyCookies) {
      extra.append(" anonymous");
    }

    if (entry._etag != null) {
      extra.append(" etag=");
      extra.append(entry._etag);
    }
    else if (entry._lastModified != null) {
      extra.append(" last-modified=");
      extra.append(entry._lastModified);
    }
    else if (CurrentTime.getCurrentTime() < entry._expireDate) {
      extra.append(" expires=");
      extra.append(QDate.formatGMT(entry._expireDate));
    }

    if (entry._isValid && entry._inode != null) {
      long length = entry._inode.getLength();
      extra.append(" length=" + length);
    }

    if (entry != null)
      log.fine("caching: " + extra.close());
    else {
      log.fine("removing cache: " + extra.close());
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _uri + "?" + _queryString
            + ", next=" + _next + "]");
  }

  enum ResetResponse {
    FAIL,
    OK,
    INCLUDE;
  }
}
