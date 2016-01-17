/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.protocol.OutResponseBase2;
import com.caucho.v5.http.protocol.RequestCache;
import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.protocol.RequestFacade;
import com.caucho.v5.http.protocol.RequestHttpBase;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseCache;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.protocol.ResponseFacade;
import com.caucho.v5.http.webapp.FilterChainCaucho;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.io.ClientDisconnectException;
import com.caucho.v5.util.Base64Util;
import com.caucho.v5.util.CacheListener;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.CharSegment;
import com.caucho.v5.util.Crc64;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;
import com.caucho.v5.util.RandomUtil;

/**
 * Represents the proxy cache in a filter chain.
 */
public class FilterChainHttpCache extends FilterChainHttpCacheBase
  implements CacheListener, FilterChainCaucho
{
  private static final L10N L = new L10N(FilterChainHttpCache.class);
  
  private static final Logger log
    = Logger.getLogger(FilterChainHttpCache.class.getName());

  private static long MIN_EXPIRES = 5000;

  private static int MAX_VARY = 8;

  private HttpCache _cache;

  private FilterChain _next;

  // is this page cacheable
  private boolean _isCacheable = true;
  // time the cache was last checked
  private long _lastCacheCheck;

  // the cached entry
  private final AtomicReference<EntryHttpCache> _entryRef
    = new AtomicReference<>();

  // thread filling the cache
  private final AtomicReference<ResponseFacade> _cacheFillResponseRef
    = new AtomicReference<>();
  
  private final AtomicReference<EntryHttpCache> _cacheFillEntry
    = new AtomicReference<EntryHttpCache>();

  private WebApp _webApp;

  // uri/query string for logging/equals
  private String _uri;
  private String _queryString;
  private int _hashCode;

  private final AtomicLong _hitCount = new AtomicLong();

  private long _uriHash;

  /**
   * Create the filter chain servlet.
   */
  public FilterChainHttpCache(FilterChain next,
                               WebApp webApp)
  {
    this(HttpCache.getLocalCache(), next, webApp);
  }

  /**
   * Create the filter chain servlet.
   */
  public FilterChainHttpCache(HttpCache cache,
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
    for (EntryHttpCache entry = _entryRef.get();
         entry != null;
         entry = entry.getNext()) {
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
    else if (! (request instanceof RequestCaucho)
             || ! (response instanceof ResponseCaucho)) {
      _next.doFilter(request, response);
      return;
    }

    RequestCache req = (RequestCache) request;
    ResponseCache res = (ResponseCache) response;

    if (! req.getMethod().equals("GET")
        && ! req.getMethod().equals("HEAD")) {
      _next.doFilter(request, response);
      return;
    }
    
    EntryHttpCache entry = getCacheEntry(req);

    long now = CurrentTime.getCurrentTime();
    
    if (entry == null) {
      _cacheFillResponseRef.compareAndSet(null, res);
    }
    else if (now <= entry.getExpireDate()
             || ! _cacheFillResponseRef.compareAndSet(null, res)) {
      if (useCache(req, res, entry)) {
        // If we've got a cached value, try to use it
        _cache.hit();
        return;
      }
    }
    
    _cache.miss();
    
    try {
      if (doRequestCacheable(req, res, entry, now)) {
        if (! req.isAsyncStarted()) { // && ! req.isDuplex()) {
          req.completeCache();
          res.completeCache();
        }
      } else {
        _next.doFilter(request, response);
      }  
    } catch (ClientDisconnectException e) {
      throw e;
    } finally {
      if (_cacheFillResponseRef.get() == res) {
        EntryHttpCache fillEntry = _cacheFillEntry.getAndSet(null);

        _cacheFillResponseRef.compareAndSet(res, null);
        
        if (fillEntry != null)
          fillEntry.remove();
      }
    }
  }
  
  private boolean doRequestCacheable(RequestCache req,
                                     ResponseCache res,
                                     EntryHttpCache entry,
                                     long now)
    throws ServletException, IOException
  {
    if (_cacheFillResponseRef.get() != res) {
      return false;
    }
    /*
    else if (req.getWebApp() == null || ! req.getWebApp().isActive() ) {
      // web-app starting: #2474
      return false;
    }
    */
    else if (! _isCacheable && now < _lastCacheCheck + 60000) {
      return false;
    }
    
    _lastCacheCheck = now;

    res.setCacheInvocation(this);
    
    if (entry != null && entry.isNotModifiedAllowed(req)) {
      RequestHttpCache cacheRequest = new RequestHttpCache(req, entry);
      ResponseHttpCache cacheResponse
        = new ResponseHttpCache(cacheRequest, res, this);

      OutResponseBase2 rs = res.getResponseStream();

      try {
        //rs.setCauchoResponse(res);

        _next.doFilter(cacheRequest, cacheResponse);
      } finally {
        //rs.setCauchoResponse(res);
        
        cacheResponse.close();
      }
    }
    else {
      // If the request caches its results, it will call startCaching.
      _next.doFilter((ServletRequest) req, (ServletResponse) res);
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

  private EntryHttpCache getCacheEntry(RequestFacade req)
  {
    EntryHttpCache entry = _entryRef.get();
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
    if (entry == null || ! entry.isValid()) {
      return null;
    }

    // Check if anonymous caching is allowed
    if (entry.isVaryCookies()
        && (req.getHeader("Cookie") != null
            || ((RequestServlet) req).isRequestedSessionIdFromURL())) {
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
  private boolean useCache(RequestCache req,
                           ResponseCache res,
                           EntryHttpCache entry)
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
  private boolean fillNotModified(RequestCache req,
                                  ResponseCache response,
                                  EntryHttpCache entry)
    throws IOException
  {
    if (! entry.isValid()) {
      return false;
    }
    
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
    
    String etag = entry.getEtag();
    if (etag != null) {
      response.setHeader("ETag", etag);
    }
    
    String lastModified = entry.getLastModified();
    if (lastModified != null) {
      response.setHeader("Last-Modified", lastModified);
    }

    if (entry.getMaxAge() > 0) {
      // response.setDateHeader("Expires", now + entry._maxAge);
      response.addHeader("Cache-Control", "max-age=" + entry.getMaxAge() / 1000);
      
      entry.setExpireDate(now + entry.getMaxAge());
    }
    
    if (entry.getServerMaxAge() > 0) {
      response.addHeader("Cache-Control", "s-maxage=" + entry.getServerMaxAge() / 1000);
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
  public boolean fillFromCache(RequestCache req,
                               ResponseCache response,
                               EntryHttpCacheBase abstractEntry)
    throws IOException
  {
    EntryHttpCache entry = (EntryHttpCache) abstractEntry;

    if (! entry.isValid()) {
      return false;
    }

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

    String range = req.getHeader("Range");
    
    if (range != null && ! entry.isAllowRange()) {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " cannot fill with range");
      
      return false;
    }
    
    String contentType = entry.getContentType();
    if (contentType != null) {
      response.setContentType(contentType);
    }

    String charEncoding = entry.getCharEncoding();
    if (charEncoding != null) {
      response.setCharacterEncoding(charEncoding);
    }

    ArrayList<String> headerKeys = entry.getHeaderKeys();
    ArrayList<String> headerValues = entry.getHeaderValues();
    for (int i = 0; headerKeys != null && i < headerKeys.size(); i++) {
      response.addHeader(headerKeys.get(i), headerValues.get(i));
    }

    String expires = entry.getExpires();
    if (expires != null) {
      response.addHeader("Expires", expires);
    }
    else if (entry.getMaxAge() > 0) {
      // HTTP/1.0 browsers should be gone
      // response.addDateHeader("Expires", now + entry._maxAge);
    }

    String method = req.getMethod();
    if (method != null && method.equals("HEAD")) {
      response.setContentLength(entry.getContentLength());

      return true;
    }

    InodeHttpCache inode = entry.getInode();
    if (inode == null || ! inode.allocate()) {
      entry.clear();
      log.fine(this + " invalid inode: " + inode);
      System.out.println(this + " BAD-INODE: " + inode + " for fillFromCache");
      
      return false;
    }

    try {
      if (range != null) {
        String ifRange = req.getHeader("If-Range");
        
        if (ifRange != null && ! ifRange.equals(entry.getEtag())) {
        }
        else if (handleRange(req, response,
                             entry, inode, range,
                             entry.getMimeType())) {

          return true;
        }
      }

      // XXX: need qa for this
      response.setCacheInvocation(null);
      response.killCache();
      response.setContentLength(entry.getContentLength());

      // inode may be cleared.
      if (! inode.writeToStream(response.getResponseStream())) {
        entry.clear();

        if (log.isLoggable(Level.FINE))
          log.fine(this + " cannot write inode: " + inode);

        System.out.println(this + " BAD_WRITE for fillFromCache()");
        return false;
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

      entry.clear();

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

  private ResetResponse resetResponse(EntryHttpCache entry,
                                      ResponseCache response)
  {
    try {
      // server/2h71, 2h79
      response.reset();

      /*
      ResponseFacade resp = response;

      while (resp != null) {
        if (resp instanceof ResponseHttpCache) {
          return ResetResponse.OK;
        }
        
        if (entry.isForwardEnclosed()) {
          // jsp/15ma
          resp.resetBuffer();
        }

        else if (resp instanceof ResponseInclude) {
          // server/2h71, 183d
          // return false;
          return ResetResponse.INCLUDE;
        }

        if (resp instanceof ResponseCaucho)
          resp = (ResponseFacade) ((ResponseCaucho)resp).getResponse();
        else
          resp = null;
      }
      */
      
      return ResetResponse.OK;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return ResetResponse.FAIL;
    }
  }
  
  /**
   * See if the request is asking for a NOT_MODIFIED response. 
   */
  private boolean isRequestNotModified(EntryHttpCache entry,
                                       RequestFacade req,
                                       ResponseFacade response)
  {
    String etag = entry.getEtag();
    
    if (etag != null) {
      String value = req.getHeader("if-none-match");

      if (value != null) {
        int i = value.indexOf(';');
        
        if (i >= 0)
          value = value.substring(0, i);
        
        if (value != null && value.equals(etag)) {
          return true;
        }
      }
    }

    String lastModified = entry.getLastModified();
    if (lastModified != null) {
      String value = req.getHeader("if-modified-since");

      if (value != null) {
        int semicolon = value.indexOf(';');
        int plus = value.indexOf('+');

        if (plus > 0)
          value = value.substring(0, plus);
        else if (semicolon > 0)
          value = value.substring(0, semicolon);

        if (lastModified.startsWith(value)) {
          return true;
        }
      }
    }
    
    return false;
  }

  private void flushBuffer(ResponseCache response)
    throws IOException
  {
    response.flushBuffer();
    response.close();

    /*
    ServletResponse resp = response.getResponse();

    while (resp != null) {
      if (resp instanceof ResponseCaucho) {
        ResponseCaucho cr = (ResponseCaucho)resp;
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
    */
  }

  /**
   * This is a duplicate of FileServlet, unfortunately.
   */
  private boolean handleRange(RequestCache req,
                              ResponseCache res,
                              EntryHttpCache cache,
                              InodeHttpCache inode,
                              String range,
                              String mime)
    throws IOException
  {
    // This is duplicated in CacheInvocation.  Possibly, it should be
    // completely removed although it's useful even without caching.
    int length = range.length();
    
    long cacheLength = cache.getContentLength();
    long bytesMax = 2 * cacheLength;
    long bytesWritten = 0;

    boolean hasMore = range.indexOf(',') > 0;

    int head = 0;
    OutResponseBase2 os = res.getResponseStream();
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
          StringBuilder cb1 = new StringBuilder();

          cb1.append("--");
          Base64Util.encode(cb1, RandomUtil.getRandomLong());
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
                  req.getRequestURI(),
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
  public EntryHttpCacheBase startCaching(RequestCache req,
                                         ResponseCache res,
                                         ArrayList<String> keys,
                                         ArrayList<String> values,
                                         String contentType,
                                         String charEncoding,
                                         long contentLength)
  {
    ResponseFacade fillResponse = _cacheFillResponseRef.get();

    if (fillResponse != res) {
      return null;
    }

    if (! isRequestCacheable(req, contentLength)) {
      _cacheFillResponseRef.set(null);
      
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

  private boolean isRequestCacheable(RequestFacade req,
                                     long contentLength)
  {
    EntryHttpCache oldEntry = _entryRef.get();

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
             && oldEntry.isVaryCookies()
             && req.getHeader("Cookie") != null) {
      // server/13wi
      return false;
    }
    else
      return true;
  }
  
  
  public EntryHttpCache fillEntry(RequestCache req,
                                  ResponseCache res,
                                  ArrayList<String> keys,
                                  ArrayList<String> values,
                                  String contentType,
                                  String charEncoding,
                                  long contentLength)
  {
    EntryHttpCache entry = new EntryHttpCache(_cache);
    
    _uri = req.getRequestURI();
    _queryString = req.getQueryString();

    long hashCode = 0;
    
    if (_uri != null) {
      hashCode = Crc64.generate(hashCode, _uri);
    }
    
    if (_queryString != null) {
      hashCode = Crc64.generate(hashCode, _queryString);
    }
    
    _hashCode = (int) (hashCode ^ Long.reverse(hashCode));

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

        if (_cache.isRewriteVaryAsPrivate() && ! entry.isVaryCookies()) {
          keys.set(i, "Cache-Control");
          values.set(i, "private");
          isPrivate = true;
        }
      }
      else if (key.equalsIgnoreCase("Content-Encoding")) {
        entry.clearAllowRange();
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
      res.setPrivateCache(true);
      // res.addHeader("Cache-Control", "private");
    }

    entry.setVary(vary);

    long crc = getVaryCRC(vary, req);

    entry.setVaryCrc(crc);

    long expireDate = -1;
    long sMaxAge = entry.getServerMaxAge();
    long maxAge = entry.getMaxAge();
    
    if (sMaxAge > 0) {
      maxAge = sMaxAge;
      entry.setMaxAge(sMaxAge);
      entry.setResinMaxAge(sMaxAge);

      entry.setExpireDate(now + sMaxAge);
      // XXX: if adding Expires, need separate date
    }
    else if (maxAge > 0) {
      // entry._maxAge = maxAge;
      entry.setResinMaxAge(maxAge);
      entry.setExpireDate(now + entry.getMaxAge());
    }

    if (expires != null) {
      if (! fillExpireDate(entry, expires, now)) {
        return null;
      }
    }
    else if (now < entry.getExpireDate()) {
      // res.setDateHeader("Expires", entry._expireDate);
    }

    entry.setLastModified(lastModified);

    entry.setEtag(etag);

    entry.setMimeType(res.getHeader("Content-Type"));

    // If we're caching, cache without checking for 5 seconds
    if (expireDate == -1 && (etag != null || lastModified != null)) {
      if (entry.getMaxAge() <= 0) {
        fillMaxAge(entry, _uri, req, res);
      }

      // server/1319
      if (entry.getMaxAge() > 0) { // && ! req.getVaryCookies())
        // res.setDateHeader("Expires", now + entry._maxAge);
      }

      // Only cache internally for 5 seconds so we see updates
      if (entry.getResinMaxAge() > 0) {
        entry.setExpireDate(now + entry.getResinMaxAge());
      }
      else if (entry.getServerMaxAge() > 0) {
        entry.setExpireDate(now + entry.getServerMaxAge());
      }
      else if (entry.getMaxAge() > 0) {
        entry.setExpireDate(now + entry.getMaxAge());
      }
    }

    if (entry.getExpireDate() - 1000 < now
        && entry.getLastModified() == null && entry.getEtag() == null) {
      killCache();
      
      return null;
    }

    /*server/131z
    if (entry._varyCookies)
      res.setNoCache(true);
    */

    // Store the headers so cached entries will be correct
    ArrayList<String> headerKeys = new ArrayList<String>();
    ArrayList<String> headerValues = new ArrayList<String>();
    
    entry.setHeaderKeys(headerKeys);
    entry.setHeaderValues(headerValues);

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);

      if (key.equalsIgnoreCase("expires")
          || key.equalsIgnoreCase("set-cookie")
          || key.equalsIgnoreCase("set-cookie2")) {
        // hide cookies and expires
      }
      else {
        headerKeys.add(key);
        headerValues.add(values.get(i));
      }
    }

    if (contentType != null)
      entry.setContentType(contentType);
    /* server/137t
    else if (charEncoding != null)
      entry._contentType = "text/html; charset=" + charEncoding;
    */

    if (charEncoding != null) {
      entry.setCharEncoding(charEncoding);
    }

    entry.setInode(_cache.createInode());
    _isCacheable = true;

    return entry;
  }
  
  private boolean processCacheControl(String value,
                                      EntryHttpCache entry)
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
        entry.setMaxAge(parseMaxAge(value, head, entry.getMaxAge()));
        
        if (entry.getMaxAge() < 0) {
          return false;
        }
      }
      else if (value.startsWith("s-maxage", head)) {
        entry.setServerMaxAge(parseMaxAge(value, head, entry.getServerMaxAge()));
        
        if (entry.getServerMaxAge() < 0)
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
                                        EntryHttpCache entry,
                                        RequestFacade req)
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
          entry.setVaryCookies(true);

          if (req.getHeader("Cookie") != null) {
            // server/13wj
            _cacheFillResponseRef.set(null);
            
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

  private boolean fillExpireDate(EntryHttpCache entry, 
                                 String expires,
                                 long now)
  {
    QDate calendar = QDate.allocateLocalDate();

    try {
      long expireDate = calendar.parseDate(expires);
      // entry.setExpires(true);
      entry.setExpires(expires);
      entry.setExpireDate(expireDate);

      if (expireDate < now) {
        killCache();

        EntryHttpCache oldEntry = _entryRef.getAndSet(null);
        _cacheFillResponseRef.set(null);

        if (oldEntry != null)
          oldEntry.remove();

        return false;
      }

      entry.setExpireDate(expireDate);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  
    QDate.freeLocalDate(calendar);
    
    return true;
  }

  private void fillMaxAge(EntryHttpCache entry, String uri,
                          RequestCache req,
                          ResponseCache res)
  {
    RequestHttpBase absRes = null;//res.getAbstractHttpResponse();
    
    /*
    WebApp webApp = req.getWebApp();

    if (webApp != null && uri != null) {
      entry.setMaxAge(webApp.getMaxAge(uri));
      entry.setServerMaxAge(webApp.getSMaxAge(uri));
    }
    */

    long maxAge = entry.getMaxAge();
    long sMaxAge = entry.getServerMaxAge();
    
    if (maxAge > 0 && maxAge < MIN_EXPIRES) {
      maxAge = MIN_EXPIRES;
      entry.setMaxAge(MIN_EXPIRES);
    }

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
    else if (maxAge > 0 && sMaxAge > 0) {
      absRes.addHeaderOutImpl("Cache-Control",
                        "max-age=" + maxAge / 1000L +
                        ", s-maxage=" + sMaxAge / 1000L);
    }
    else if (maxAge > 0) {
      absRes.addHeaderOutImpl("Cache-Control", "max-age=" + maxAge / 1000L);
    }
    else if (sMaxAge > 0) {
      absRes.addHeaderOutImpl("Cache-Control", "s-maxage=" + sMaxAge / 1000L);
    }

    if (sMaxAge > 0) {
      entry.setResinMaxAge(sMaxAge);
    }
    else if (maxAge > 0) {
      entry.setResinMaxAge(maxAge);
    }
    else {
      entry.setResinMaxAge(internalCacheTimeout);
    }
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
    HttpCache cache = _cache;

    if (cache != null) {
      cache.setEnable(false);
      cache.clear();
    }
  }

  public void clearCache()
  {
    HttpCache cache = _cache;

    if (cache != null) {
      cache.clear();
    }
  }

  /**
   * Returns the vary entry matching the crc.
   */
  private EntryHttpCache getVaryEntry(EntryHttpCache entry, 
                                      RequestFacade req)
  {
    ArrayList<String> vary = entry.getVary();

    if (vary == null) {
      return entry;
    }

    long crc = getVaryCRC(vary, req);

    for (; entry != null; entry = entry.getNext()) {
      if (entry.getVaryCrc() == crc)
        return entry;
    }

    return null;
  }

  /**
   * Returns the vary crc.
   */
  private long getVaryCRC(ArrayList<String> vary, RequestFacade req)
  {
    if (vary == null)
      return 0;

    long crc = 0;

    if (req instanceof RequestServlet) {
      RequestHttpBase cauchoReq
        = ((RequestServlet) req).getAbstractHttpRequest();

      for (int i = 0; i < vary.size(); i++) {
        String key = vary.get(i);
        CharSegment value = cauchoReq.getHeaderBuffer(key);

        if (value != null)
          crc = Crc64.generate(crc,
                               value.buffer(),
                               value.offset(),
                               value.length());
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
  public void finishCaching(ResponseCache res)
  {
    if (_cacheFillResponseRef.get() != res) {
      // throw new IllegalStateException(L.l("{0} multiple finish caching calls",
      //                                     this));
      return;
    }
    
    EntryHttpCache loadingEntry = _cacheFillEntry.get();
    
    if (loadingEntry == null) {
      return;
    }
    
    InodeHttpCache inode;

    if ((inode = loadingEntry.getInode()) != null) {
      long contentLength = inode.getLength();

      loadingEntry.setContentLength(contentLength);
      loadingEntry.setValid(true);

      if (contentLength <= 0) {
        log.fine(this + " tried to cache blank page");

        loadingEntry.setValid(false);
        killCache();
      }
      else if (contentLength < _cache.getMaxEntrySize()) {
        EntryHttpCache oldEntry = _entryRef.get();
        EntryHttpCache newEntry = replaceEntry(oldEntry, loadingEntry); 
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

    if (log.isLoggable(Level.FINE) && _entryRef.get() != null) {
      logCache(_entryRef.get());
    }
  }

  /**
   * Update the headers when the caching has finished.
   *
   * @param okay if true, the cache if valid
   */
  @Override
  public void killCaching(ResponseCache res)
  {
    if (_cacheFillResponseRef.get() != res) {
      return;
    }
    
    EntryHttpCache loadingEntry = _cacheFillEntry.getAndSet(null);
    
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
  private EntryHttpCache replaceEntry(EntryHttpCache head,
                                       EntryHttpCache next)
  {
    int count = 0;

    EntryHttpCache prev = null;
    for (EntryHttpCache ptr = head; ptr != null; ptr = ptr.getNext()) {
      if (ptr.getVaryCrc() == next.getVaryCrc()) {
        next.setNext(ptr.getNext());

        ptr.remove();

        if (prev != null) {
          prev.setNext(next);
          return head;
        }
        else
          return next;
      }

      count++;
      prev = ptr;
    }

    next.setNext(head);

    if (MAX_VARY < count) {
      EntryHttpCache ptr = head;
      for (; ptr != null && count > 0; ptr = ptr.getNext()) {
        count--;
      }

      if (ptr != null)
        ptr.setNext(null);
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
    EntryHttpCache entry;

    entry = _entryRef.getAndSet(null);

    if (log.isLoggable(Level.FINE) && entry != null) {
      String debugURI = _uri;
      if (_queryString != null)
        debugURI += "?" + _queryString;

      log.fine("lru removing cache: " + debugURI);
    }

    for (; entry != null; entry = entry.getNext()) {
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
  private void logCache(EntryHttpCache entry)
  {
    CharBuffer extra = new CharBuffer();

    extra.append(_uri);
    if (_queryString != null) {
      extra.append("?");
      extra.append(_queryString);
    }

    if (entry.getVaryCrc() != 0) {
      extra.append(" crc=");
      extra.append(entry.getVaryCrc());
    }

    if (entry.isVaryCookies()) {
      extra.append(" anonymous");
    }

    if (entry.getEtag() != null) {
      extra.append(" etag=");
      extra.append(entry.getEtag());
    }
    else if (entry.getLastModified() != null) {
      extra.append(" last-modified=");
      extra.append(entry.getLastModified());
    }
    else if (CurrentTime.getCurrentTime() < entry.getExpireDate()) {
      extra.append(" expires=");
      extra.append(QDate.formatGMT(entry.getExpireDate()));
    }

    if (entry.isValid() && entry.getInode() != null) {
      long length = entry.getInode().getLength();
      extra.append(" length=" + length);
    }

    if (entry != null)
      log.fine("caching: " + extra.close());
    else {
      log.fine("removing cache: " + extra.close());
    }
  }
  
  @Override
  public int hashCode()
  {
    return (int) _hashCode;
  }
  
  @Override
  public boolean equals(Object o)
  {
    return this == o;
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
