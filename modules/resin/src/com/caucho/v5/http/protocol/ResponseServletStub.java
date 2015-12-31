/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import com.caucho.v5.http.cache.EntryHttpCacheBase;
import com.caucho.v5.http.cache.FilterChainHttpCacheBase;
import com.caucho.v5.vfs.FlushBuffer;
import com.caucho.v5.vfs.PrintWriterImpl;
import com.caucho.v5.vfs.WriteStream;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;
import java.util.Collection;
import java.util.Locale;
import java.util.Collection;

// Is there anything at all useful that could be put here?
public class ResponseServletStub implements ResponseCaucho {
  public ResponseServletStub()
  {
  }

  protected OutResponseBase createResponseStream()
  {
    return new OutResponseStub();
  }
  
  @Override
  public String getCharacterEncoding()
  {
    return "ISO-8859-1";
  }
  
  @Override
  public void setLocale(Locale locale)
  {
  }
  
  @Override
  public Locale getLocale()
  {
    return null;
  }

  @Override
  public void setContentLengthLong(long length)
  {
  }
  
  public boolean writeHeadersInt(WriteStream out, int length, boolean isHead)
  {
    return false;
  }

  @Override
  public void setBufferSize(int size)
  {
  }
  
  @Override
  public int getBufferSize()
  {
    return 0;
  }
  
  @Override
  public void flushBuffer()
  {
  }
  
  @Override
  public boolean isCommitted()
  {
    return false;
  }
  
  @Override
  public void reset()
  {
  }
  
  @Override
  public void resetBuffer()
  {
  }
  
  @Override
  public void setContentLength(int length)
  {
  }
  
  @Override
  public void setContentType(String type)
  {
  }

  @Override
  public void setStatus(int status)
  {
  }
  
  @Override
  public void setStatus(int status, String messages)
  {
  }
  
  @Override
  public void sendRedirect(String location)
  {
  }
  
  @Override
  public void sendError(int i)
  {
  }
  
  @Override
  public void sendError(int i, String message)
  {
  }
    
  @Override
  public String encodeUrl(String url)
  {
    return url;
  }
  
  @Override
  public String encodeURL(String url)
  {
    return url;
  }
  
  @Override
  public String encodeRedirectUrl(String url)
  {
    return url;
  }
  
  @Override
  public String encodeRedirectURL(String url)
  {
    return url;
  }

  @Override
  public void addCookie(Cookie cookie)
  {
  }
  
  @Override
  public boolean containsHeader(String header)
  {
    return false;
  }
  
  @Override
  public void setHeader(String header, String value)
  {
  }
  
  @Override
  public void setIntHeader(String header, int value)
  {
  }
  
  @Override
  public void setDateHeader(String header, long value)
  {
  }
  
  @Override
  public void addHeader(String header, String value)
  {
  }
  
  @Override
  public void addIntHeader(String header, int value)
  {
  }
  
  @Override
  public void addDateHeader(String header, long value)
  {
  }

  @Override
  public String getHeader(String key)
  {
    return null;
  }
  
  public void clearBuffer()
  {
  }
  
  @Override
  public void completeCache()
  {
  }
  
  @Override
  public void close() throws IOException
  {
  }

  public boolean disableHeaders(boolean disable)
  {
    return false;
  }

  public int getRemaining()
  {
    return 0;
  }

  @Override
  public void setForbidForward(boolean forbid)
  {
  }
  
  @Override
  public boolean getForbidForward()
  {
    return false;
  }
  
  @Override
  public void setHasError(boolean hasError)
  {
  }
  
  @Override
  public boolean hasError()
  {
    return true;
  }
  
  @Override
  public void killCache()
  {
  }
  
  @Override
  public void setPrivateCache(boolean isPrivate)
  {
  }
  
  @Override
  public void setSessionId(String id)
  {
  }

  @Override
  public int getStatus()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Collection<String> getHeaders(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Collection<String> getHeaderNames()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public RequestHttpBase getAbstractHttpResponse()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public ServletResponse getResponse()
  {
    return null;
  }
  
  @Override
  public void setNoCache(boolean killCache)
  {
  }

  public int getStatusCode()
  {
    return 200;
  }

  @Override
  public String getStatusMessage()
  {
    return null;
  }
  
  @Override
  public void setFooter(String key, String value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
    
  @Override
  public void addFooter(String key, String value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public void setFlushBuffer(FlushBuffer out)
  {
  }
  
  public FlushBuffer getFlushBuffer()
  {
    return null;
  }

  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  @Override
  public void setResponseStream(OutResponseBase stream)
  {
  }

  @Override
  public OutResponseBase getResponseStream()
  {
    return new OutResponseWrapper();
  }

  public boolean isDisabled()
  {
    return false;
  }

  public void enable()
  {
  }

  public void disable()
  {
  }

  @Override
  public PrintWriter getWriter()
    throws IOException
  {
    return new PrintWriterImpl(new NullWriter());
  }

  @Override
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    ServletOutputStreamImpl out = new ServletOutputStreamImpl();

    out.init(NullOutputStream.NULL);

    return out;
  }

  @Override
  public void setCharacterEncoding(String enc)
  {
  }

  @Override
  public String getContentType()
  {
    return null;
  }

  @Override
  public boolean isNoCacheUnlessVary()
  {
    return false;
  }
  
  @Override
  public void setCacheInvocation(FilterChainHttpCacheBase cacheFilterChain)
  {
  }
  
  @Override
  public boolean isCaching()
  {
    return false;
  }

  public void setMatchCacheEntry(EntryHttpCacheBase cacheEntry)
  {
  }

  @Override
  public void setForwardEnclosed(boolean isForwardEnclosed) {
  }

  @Override
  public boolean isForwardEnclosed()
  {
    return false;
  }
  
  @Override
  public void writeHeaders(int length)
  {
  }

  static class NullWriter extends Writer {
    private static final NullWriter NULL = new NullWriter();

    public void write(int ch) {}
    public void write(char []buffer, int offset, int length) {}
    public void flush() {}
    public void close() {}
  }

  static class NullOutputStream extends OutputStream {
    private static final NullOutputStream NULL = new NullOutputStream();

    public void write(int ch) {}
    public void write(byte []buffer, int offset, int length) {}
    public void flush() {}
    public void close() {}
  }
}
