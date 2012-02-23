/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.httpcache;

import com.caucho.server.http.CauchoRequest;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;

/**
 * Cached response.
 */
public class ProxyCacheEntry extends AbstractCacheEntry {
  private static final Logger log
    = Logger.getLogger(ProxyCacheEntry.class.getName());
  
  private static final long INTERNAL_MAX_AGE = 5000; 

  ProxyCache _cache;

  // Next link for the vary
  ProxyCacheEntry _next;

  // path to the stored data
  ProxyCacheInode _inode;
  
  // saved header keys
  ArrayList<String> _headerKeys;
  // saved header values
  ArrayList<String> _headerValues;
  // saved content type
  String _contentType;
  // saved content encoding
  String _charEncoding;
  // saved etag
  String _etag;
  // saved last-modified
  String _lastModified;
  // saved mime-type
  String _mimeType;
  // cache length
  long _contentLength;
  // true if it allows range
  boolean _allowRange = true;

  // vary headers
  ArrayList<String> _vary;
  // vary crc
  long _varyCrc;

  // true if the cache depends on cookies
  boolean _varyCookies;

  long _resinMaxAge;
  long _maxAge = Long.MIN_VALUE;
  long _sMaxAge = Long.MIN_VALUE;
  
  long _expireDate;
  String _expireString;
  // true if the user set the expires
  boolean _hasExpires;
  boolean _isChar;
    
  // true if the entry is complete and valid
  boolean _isValid;
  boolean _isForwardEnclosed;

  ProxyCacheEntry(ProxyCache cache)
  {
    _cache = cache;

    _allowRange = cache.isEnableRange();
  }
  
  /**
   * If the response discovers that the page hasn't changed,
   * update the cache time.
   */
  @Override
  public void updateExpiresDate()
  {
    _expireDate = CurrentTime.getCurrentTime() + 5000;
  }

  /**
   * Clears the expires time
   */
  public void clearExpires()
  {
    _expireDate = 0;
  }

  /**
   * Returns the current inode.
   */
  ProxyCacheInode getInode()
  {
    return _inode;
  }

  /**
   * Opens the inode for byte writing.
   */
  @Override
  public OutputStream openOutputStream()
  {
    _isChar = false;

    return _inode.openOutputStream();
  }

  /**
   * Opens the inode for writing.
   */
  @Override
  public Writer openWriter()
  {
    _isChar = true;

    return _inode.openWriter();
  }

  /**
   * Sets the current inode.
   */
  void setInode(ProxyCacheInode inode)
  {
    if (_inode != null)
      throw new IllegalStateException();
    
    _inode = inode;
  }

  void remove()
  {
    _isValid = false;

    ProxyCacheInode inode = _inode;
    _inode = null;

    if (inode != null) {
      try {
        inode.free();
      } catch (Exception e) {
        _cache.setEnable(false);
        
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  @Override
  public void setForwardEnclosed(boolean isForwardEnclosed)
  {
    _isForwardEnclosed = isForwardEnclosed;
  }

  @Override
  public boolean isForwardEnclosed()
  {
    return _isForwardEnclosed;
  }

  @Override
  public void destroy()
  {
    remove();
  }

  public boolean isNotModifiedAllowed(CauchoRequest cReq)
  {
    // If the user set the expires, we need to go back so it can be
    // reset
    if (_hasExpires) {
      return false;
    }

    // If it's a range request, then must go through
    String range = cReq.getHeader("Range");
    
    if (range != null && ! _allowRange) {
      return false;
    }

    // If the servlet set the caching tags on its cached response,
    // set them for our request.

    // DispatchRequest must ignore the If-None-Match header
    // in the original request
    if (_etag == null && _lastModified == null)
      return false;
    
    if (cReq.getHeader("If-None-Match") != null
        || cReq.getHeader("If-Modified-Since") != null) {
      return false;
    }
    
    ProxyCacheInode inode = getInode();
    if (inode == null || inode.isClosed())
      return false;

    
    return true;
  }

  /**
   * Updates based on a not-modified time
   */
  public void updateNotModified()
  {
    long now = CurrentTime.getCurrentTime();

    if (_sMaxAge > 0)
      _expireDate = _sMaxAge + now;
    else if (_maxAge > 0)
      _expireDate = _maxAge + now;
    else
      _expireDate = now + INTERNAL_MAX_AGE;
  }
}
