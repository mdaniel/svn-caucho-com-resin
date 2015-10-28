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

package com.caucho.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.cache.FilterChainHttpCacheBase;
import com.caucho.v5.http.protocol.OutResponseBase;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.protocol.ResponseWrapper;
import com.caucho.v5.http.protocol.ResponseWriter;
import com.caucho.v5.http.protocol.ServletOutputStreamImpl;
import com.caucho.v5.vfs.FlushBuffer;

/**
 * Response wrapper that can take advantage of Resin's streams.
 */
public class CauchoResponseWrapper extends ResponseWrapper
  implements ResponseCaucho {
  private static final Logger log
    = Logger.getLogger(CauchoResponseWrapper.class.getName());
  
  private FlushBuffer _flushBuffer;
  
  private final FilterWrapperResponseStream _originalStream;
  protected OutResponseBase _stream;

  private ResponseWriter _writer;
  private ServletOutputStreamImpl _os;
  
  private boolean _hasError;

  public CauchoResponseWrapper()
  {
    _os = new ServletOutputStreamImpl();
    _writer = new ResponseWriter();
    
    _originalStream = new FilterWrapperResponseStream();
  }

  public CauchoResponseWrapper(HttpServletResponse response)
  {
    this();
    
    init(response);
  }

  /**
   * Initialize the response.
   */
  public void init(HttpServletResponse response)
  {
    setResponse(response);

    _stream = _originalStream;
    _os.init(_originalStream);
    _writer.init(_originalStream);

    _hasError = false;

    _originalStream.init(this);

    _originalStream.start();
  } 

  /**
   * complete the response.
   */
  @Override
  public void close()
    throws IOException
  {
    if (_stream != null) {
      _stream.close();
    }
    else {
      _originalStream.close();
    }
  }

  @Override
  public ServletResponse getResponse()
  {
    return _response;
  }

  /**
   * Sets the response content type.
   */
  @Override
  public void setContentType(String value)
  {
    _response.setContentType(value);
    
    try {
      _stream.setEncoding(getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
    }
  }

  /**
   * Sets the ResponseStream
   */
  @Override
  public void setResponseStream(OutResponseBase stream)
  {
    _stream = stream;

    _os.init(stream);
    _writer.init(stream);
  }

  /**
   * Gets the response stream.
   */
  @Override
  public OutResponseBase getResponseStream()
  {
    return _stream;
  }

  /**
   * Returns true for a caucho response stream.
   */
  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Returns the servlet output stream.
   */
  @Override
  public ServletOutputStream getOutputStream() throws IOException
  {
    return _os;
  }

  /**
   * Returns the print writer.
   */
  @Override
  public PrintWriter getWriter() throws IOException
  {
    return _writer;
  }

  /**
   * Returns the output stream for this wrapper.
   */
  protected OutputStream getStream() throws IOException
  {
    return _response.getOutputStream();
  }

  /**
   * Sets the flush buffer
   */
  public void setFlushBuffer(FlushBuffer flushBuffer)
  {
    _flushBuffer = flushBuffer;
  }

  /**
   * Gets the flush buffer
   */
  public FlushBuffer getFlushBuffer()
  {
    return _flushBuffer;
  }

  @Override
  public void flushBuffer()
    throws IOException
  {
    if (_flushBuffer != null)
      _flushBuffer.flushBuffer();
    
    _stream.flushBuffer();
    
    //_response.flushBuffer();
  }

  @Override
  public void reset()
  {
    super.reset();

    resetBuffer();
  }
  
  @Override
  public void resetBuffer()
  {
    if (_stream != null)
      _stream.clearBuffer();
    
    _response.resetBuffer();
  }

  public void clearBuffer()
  {
    resetBuffer();
  }

  @Override
  public void setLocale(Locale locale)
  {
    _response.setLocale(locale);
    
    try {
      _stream.setLocale(_response.getLocale());
    } catch (UnsupportedEncodingException e) {
    }
  }

  /*
   * caucho
   */

  @Override
  public String getHeader(String key)
  {
    try {
      return _response.getHeader(key);
    } catch (AbstractMethodError e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    }
  }
  
  public boolean disableHeaders(boolean disable)
  {
    return false;
  }
  
  @Override
  public void setContentLengthLong(long length)
  {
    _response.setContentLengthLong(length);
  }

  @Override
  public void setFooter(String key, String value)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setFooter(key, value);
  }

  @Override
  public void addFooter(String key, String value)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).addFooter(key, value);
  }

  public int getRemaining()
  {
    /*
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getRemaining();
    else
      return 0;
    */
    return _stream.getRemaining();
  }

  /**
   * When set to true, RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public void setForbidForward(boolean forbid)
  {
  }

  /**
   * Returns true if RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public boolean getForbidForward()
  {
    return false;
  }

  @Override
  public String getStatusMessage()
  {
    if (_response instanceof ResponseCaucho)
      return ((ResponseCaucho) _response).getStatusMessage();

    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  /**
   * Set to true while processing an error.
   */
  @Override
  public void setHasError(boolean hasError)
  {
    _hasError = hasError;
  }

  /**
   * Returns true if we're processing an error.
   */
  @Override
  public boolean hasError()
  {
    return _hasError;
  }

  /**
   * Kills the cache for an error.
   */
  @Override
  public void killCache()
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).killCache();
  }
  
  @Override
  public void completeCache()
  {
  }
  
  @Override
  public void setSessionId(String id)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setSessionId(id);
  }
  
  @Override
  public void setPrivateCache(boolean isPrivate)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setPrivateCache(isPrivate);
  }
  
  @Override
  public void setNoCache(boolean isPrivate)
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).setNoCache(isPrivate);
  }
  
  @Override
  public void writeHeaders(int length)
    throws IOException
  {
    if (_response instanceof ResponseCaucho)
      ((ResponseCaucho) _response).writeHeaders(length);
  }

  @Override
  public int getStatus()
  {
    return _response.getStatus();
  }

  @Override
  public Collection<String> getHeaders(String name)
  {
    return _response.getHeaders(name);
  }

  @Override
  public Collection<String> getHeaderNames()
  {
    return _response.getHeaderNames();
  }

  @Override
  public void setForwardEnclosed(boolean isForwardEnclosed)
  {
  }

  @Override
  public boolean isForwardEnclosed()
  {
    return false;
  }

  @Override
  public boolean isNoCacheUnlessVary()
  {
    return false;
  }
}
