/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.http.protocol.RequestFacade;
import com.caucho.v5.util.CurrentTime;

/**
 * Cached response.
 */
public class EntryHttpCache extends EntryHttpCacheBase {
  private static final Logger log
    = Logger.getLogger(EntryHttpCache.class.getName());
  
  private static final long INTERNAL_MAX_AGE = 5000; 

  private HttpCache _cache;

  // Next link for the vary
  private EntryHttpCache _next;

  // path to the stored data
  private InodeHttpCache _inode;
  
  // saved header keys
  private ArrayList<String> _headerKeys;
  // saved header values
  private ArrayList<String> _headerValues;
  // saved content type
  private String _contentType;
  // saved content encoding
  private String _charEncoding;
  // saved etag
  private String _etag;
  // saved last-modified
  private String _lastModified;
  // saved mime-type
  private String _mimeType;
  // cache length
  private long _contentLength;
  // true if it allows range
  private boolean _allowRange = true;

  // vary headers
  private ArrayList<String> _vary;
  // vary crc
  private long _varyCrc;

  // true if the cache depends on cookies
  private boolean _varyCookies;

  private long _resinMaxAge;
  private long _maxAge = Long.MIN_VALUE;
  private long _sMaxAge = Long.MIN_VALUE;
  
  private long _expireDate;
  private String _expireString;
  // true if the user set the expires
  private boolean _hasExpires;
    
  // true if the entry is complete and valid
  private boolean _isValid;
  private boolean _isForwardEnclosed;

  EntryHttpCache(HttpCache cache)
  {
    _cache = cache;

    _allowRange = cache.isEnableRange();
  }

  public EntryHttpCache getNext()
  {
    return _next;
  }

  public void setNext(EntryHttpCache next)
  {
    _next = next;
  }
  
  boolean isVaryCookies()
  {
    return _varyCookies;
  }

  void setVaryCookies(boolean value)
  {
    _varyCookies = value;
  }

  boolean isValid()
  {
    InodeHttpCache inode = _inode;
    
    return _isValid && inode != null && inode.isValid();
  }
  
  void setValid(boolean isValid)
  {
    _isValid = isValid;
  }
  
  boolean isAllowRange()
  {
    return _allowRange;
  }
  
  void clearAllowRange()
  {
    _allowRange = false;
  }
  
  void clear()
  {
    _isValid = false;
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
  
  public long getExpireDate()
  {
    return _expireDate;
  }
  
  public void setExpireDate(long date)
  {
    _expireDate = date;
  }
  
  public void setExpires(String expireString)
  {
    _hasExpires = true;
    _expireString = expireString;
  }

  public String getExpires()
  {
    if (_hasExpires) {
      return _expireString;
    }
    else {
      return null;
    }
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
  InodeHttpCache getInode()
  {
    return _inode;
  }

  public long getContentLength()
  {
    return _contentLength;
  }

  public void setContentLength(long contentLength)
  {
    _contentLength = contentLength;
  }

  public String getContentType()
  {
    return _contentType;
  }

  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }

  public String getCharEncoding()
  {
    return _charEncoding;
  }

  public void setCharEncoding(String charEncoding)
  {
    _charEncoding = charEncoding;
  }

  public long getMaxAge()
  {
    return _maxAge;
  }

  public void setMaxAge(long maxAge)
  {
    _maxAge = maxAge;
  }

  public long getResinMaxAge()
  {
    return _resinMaxAge;
  }

  public void setResinMaxAge(long maxAge)
  {
    _resinMaxAge = maxAge;
  }

  public long getServerMaxAge()
  {
    return _sMaxAge;
  }

  public void setServerMaxAge(long maxAge)
  {
    _sMaxAge = maxAge;
  }

  public String getEtag()
  {
    return _etag;
  }
  
  public void setEtag(String etag)
  {
    _etag = etag;
  }

  public String getMimeType()
  {
    return _mimeType;
  }

  public void setMimeType(String type)
  {
    _mimeType = type;
  }
  
  public String getLastModified()
  {
    return _lastModified;
  }

  public void setLastModified(String lastModified)
  {
    _lastModified = lastModified;
  }

  public ArrayList<String> getHeaderKeys()
  {
    return _headerKeys;
  }

  public void setHeaderKeys(ArrayList<String> headerKeys)
  {
    _headerKeys = headerKeys;
  }

  public ArrayList<String> getHeaderValues()
  {
    return _headerValues;
  }

  public void setHeaderValues(ArrayList<String> headerValues)
  {
    _headerValues = headerValues;
  }

  public void setVary(ArrayList<String> vary)
  {
    _vary = vary;
  }

  public ArrayList<String> getVary()
  {
    return _vary;
  }

  public long getVaryCrc()
  {
    return _varyCrc;
  }

  public void setVaryCrc(long crc)
  {
    _varyCrc = crc;
  }

  /**
   * Opens the inode for byte writing.
   */
  @Override
  public OutputStream openOutputStream()
  {
    return _inode.openOutputStream();
  }

  /**
   * Sets the current inode.
   */
  void setInode(InodeHttpCache inode)
  {
    Objects.requireNonNull(inode);
    
    _inode = inode;
  }

  void remove()
  {
    _isValid = false;

    InodeHttpCache inode = _inode;
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

  public boolean isNotModifiedAllowed(RequestFacade cReq)
  {
    if (! isValid()) {
      return false;
    }
    
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
    if (_etag == null && _lastModified == null) {
      return false;
    }
    
    if (cReq.getHeader("If-None-Match") != null
        || cReq.getHeader("If-Modified-Since") != null) {
      return false;
    }
    
    InodeHttpCache inode = getInode();
    if (inode == null || ! inode.isValid()) {
      return false;
    }
    
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
