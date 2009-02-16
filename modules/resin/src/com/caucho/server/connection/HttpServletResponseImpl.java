/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.server.connection;

import com.caucho.util.L10N;
import com.caucho.vfs.FlushBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;

/**
 * User facade for http responses.
 */
public class HttpServletResponseImpl implements CauchoResponse
{
  private static final L10N L = new L10N(HttpServletResponseImpl.class);
  
  private AbstractHttpRequest _request;
  private AbstractHttpResponse _response;

  public HttpServletResponseImpl(AbstractHttpResponse response)
  {
    _response = response;
  }

  //
  // servlet response
  //
  
  /**
   * Sets the response content type.  The content type includes
   * the character encoding.  The content type must be set before
   * calling <code>getWriter()</code> so the writer can use the
   * proper character encoding.
   *
   * <p>To set the output character encoding to ISO-8859-2, use the
   * following:
   *
   * <code><pre>
   * response.setContentType("text/html; charset=ISO-8859-2");
   * </pre></code>
   *
   * @param type the mime type of the output
   */
  public void setContentType(String type)
  {
    getResponse().setContentType(type);
  }
  
  /**
   * Returns the content type for the response.
   *
   * @since 2.4
   */
  public String getContentType()
  {
    return getResponse().getContentType();
  }

  /**
   * Returns the character encoding the response is using for output.
   * If no character encoding is specified, ISO-8859-1 will be used.
   */
  public String getCharacterEncoding()
  {
    return getResponse().getCharacterEncoding();
  }

  /**
   * Sets the character encoding the response is using for output.
   * If no character encoding is specified, ISO-8859-1 will be used.
   *
   * @since 2.4
   */
  public void setCharacterEncoding(String charset)
  {
    getResponse().setCharacterEncoding(charset);
  }
  
  /**
   * Sets the output locale.  The response will set the character encoding
   * based on the locale.  For example, setting the "kr" locale will set
   * the character encoding to "EUC_KR".
   */
  public void setLocale(Locale locale)
  {
    getResponse().setLocale(locale);
  }
  
  /**
   * Returns the output locale.
   */
  public Locale getLocale()
  {
    return getResponse().getLocale();
  }
  
  /**
   * Returns an output stream for writing to the client.  You can use
   * the output stream to write binary data.
   */
  public ServletOutputStream getOutputStream()
    throws IOException
  {
    return getResponse().getOutputStream();
  }
  
  /**
   * Returns a PrintWriter with the proper character encoding for writing
   * text data to the client.
   */
  public PrintWriter getWriter()
    throws IOException
  {
    return getResponse().getWriter();
  }
  
  /**
   * Sets the output buffer size to <code>size</code>.  The servlet engine
   * may round the size up.
   *
   * @param size the new output buffer size.
   */
  public void setBufferSize(int size)
  {
    getResponse().setBufferSize(size);
  }
  
  /**
   * Returns the size of the output buffer.
   */
  public int getBufferSize()
  {
    return getResponse().getBufferSize();
  }
  
  /**
   * Flushes the buffer to the client.
   */
  public void flushBuffer()
    throws IOException
  {
    getResponse().flushBuffer();
  }
  
  /**
   * Returns true if some data has actually been send to the client.  The
   * data will be sent if the buffer overflows or if it's explicitly flushed.
   */
  public boolean isCommitted()
  {
    return getResponse().isCommitted();
  }
  
  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void reset()
  {
    getResponse().reset();
  }
  
  /**
   * Resets the output stream, clearing headers and the output buffer.
   * Calling <code>reset()</code> after data has been committed is illegal.
   *
   * @throws IllegalStateException if <code>isCommitted()</code> is true.
   */
  public void resetBuffer()
  {
    getResponse().resetBuffer();
  }
  
  /**
   * Explicitly sets the length of the result value.  Normally, the servlet
   * engine will handle this.
   */
  public void setContentLength(int len)
  {
    getResponse().setContentLength(len);
  }

  /**
   * Disables the response
   *
   * @since Servlet 3.0
   */
  public void disable()
  {
    getResponse().disable();
  }

  /**
   * Enables the response
   *
   * @since Servlet 3.0
   */
  public void enable()
  {
    getResponse().enable();
  }

  /**
   * Returns true if the response is disabled
   *
   * @since Servlet 3.0
   */
  public boolean isDisabled()
  {
    return getResponse().isDisabled();
  }

  //
  // HttpServletResponse methods
  //

  /**
   * Sets the HTTP status
   *
   * @param sc the HTTP status code
   */
  public void setStatus(int sc)
  {
    getResponse().setStatus(sc);
  }
  
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc, String msg)
    throws IOException
  {
    getResponse().sendError(sc, msg);
  }
  
  /**
   * Sends an HTTP error page based on the status code
   *
   * @param sc the HTTP status code
   */
  public void sendError(int sc)
    throws IOException
  {
    getResponse().sendError(sc);
  }
  
  /**
   * Redirects the client to another page.
   *
   * @param location the location to redirect to.
   */
  public void sendRedirect(String location)
    throws IOException
  {
    getResponse().sendRedirect(location);
  }
  
  /**
   * Sets a header.  This will override a previous header
   * with the same name.
   *
   * @param name the header name
   * @param value the header value
   */
  public void setHeader(String name, String value)
  {
    getResponse().setHeader(name, value);
  }
  
  /**
   * Adds a header.  If another header with the same name exists, both
   * will be sent to the client.
   *
   * @param name the header name
   * @param value the header value
   */
  public void addHeader(String name, String value)
  {
    getResponse().addHeader(name, value);
  }
  
  /**
   * Returns true if the output headers include <code>name</code>
   *
   * @param name the header name to test
   */
  public boolean containsHeader(String name)
  {
    return getResponse().containsHeader(name);
  }
  
  /**
   * Sets a header by converting a date to a string.
   *
   * <p>To set the page to expire in 15 seconds use the following:
   * <pre><code>
   * long now = System.currentTime();
   * response.setDateHeader("Expires", now + 15000);
   * </code></pre>
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void setDateHeader(String name, long date)
  {
    getResponse().setDateHeader(name, date);
  }
  
  /**
   * Adds a header by converting a date to a string.
   *
   * @param name name of the header
   * @param date the date in milliseconds since the epoch.
   */
  public void addDateHeader(String name, long date)
  {
    getResponse().addDateHeader(name, date);
  }
    
  /**
   * Sets a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void setIntHeader(String name, int value)
  {
    getResponse().setIntHeader(name, value);
  }
  
  /**
   * Adds a header by converting an integer value to a string.
   *
   * @param name name of the header
   * @param value the value as an integer
   */
  public void addIntHeader(String name, int value)
  {
    getResponse().addIntHeader(name, value);
  }
  
  /**
   * Sends a new cookie to the client.
   */
  public void addCookie(Cookie cookie)
  {
    getResponse().addCookie(cookie);
  }
  
  /**
   * Encodes session information in a URL. Calling this will enable
   * sessions for users who have disabled cookies.
   *
   * @param url the url to encode
   * @return a url with session information encoded
   */
  public String encodeURL(String url)
  {
    return getResponse().encodeURL(url);
  }
  
  /**
   * Encodes session information in a URL suitable for
   * <code>sendRedirect()</code> 
   *
   * @param url the url to encode
   * @return a url with session information encoded
   */
  public String encodeRedirectURL(String name)
  {
    return getResponse().encodeRedirectURL(name);
  }
  
  /**
   * @deprecated
   */
  public void setStatus(int sc, String msg)
  {
    getResponse().setStatus(sc, msg);
  }
  
  /**
   * @deprecated
   */
  public String encodeUrl(String url)
  {
    return getResponse().encodeUrl(url);
  }
  
  /**
   * @deprecated
   */
  public String encodeRedirectUrl(String url)
  {
    return getResponse().encodeRedirectUrl(url);
  }

  //
  // CauchoResponse methods
  //

  public AbstractResponseStream getResponseStream()
  {
    return getResponse().getResponseStream();
  }
  
  public void setResponseStream(AbstractResponseStream os)
  {
    getResponse().setResponseStream(os);
  }

  public boolean isCauchoResponseStream()
  {
    return getResponse().isCauchoResponseStream();
  }
  
  public void setFlushBuffer(FlushBuffer out)
  {
    getResponse().setFlushBuffer(out);
  }
  
  public FlushBuffer getFlushBuffer()
  {
    return getResponse().getFlushBuffer();
  }
  
  public String getHeader(String key)
  {
    return getResponse().getHeader(key);
  }
  
  public void setFooter(String key, String value)
  {
    getResponse().setFooter(key, value);
  }
  
  public void addFooter(String key, String value)
  {
    getResponse().addFooter(key, value);
  }

  // XXX: really close invocation
  
  public void close()
    throws IOException
  {
  }

  public boolean disableHeaders(boolean disable)
  {
    return getResponse().disableHeaders(disable);
  }

  public boolean getForbidForward()
  {
    return getResponse().getForbidForward();
  }
  
  public void setForbidForward(boolean forbid)
  {
    getResponse().setForbidForward(forbid);
  }

  public int getStatusCode()
  {
    return getResponse().getStatusCode();
  }
  
  public String getStatusMessage()
  {
    return getResponse().getStatusMessage();
  }

  public boolean hasError()
  {
    return getResponse().hasError();
  }
  
  public void setHasError(boolean error)
  {
    getResponse().setHasError(error);
  }

  public void setSessionId(String id)
  {
    getResponse().setSessionId(id);
  }

  public void killCache()
  {
    getResponse().killCache();
  }
  
  public void setNoCache(boolean killCache)
  {
    getResponse().setNoCache(killCache);
  }
  
  public void setPrivateCache(boolean isPrivate)
  {
    getResponse().setPrivateCache(isPrivate);
  }

  //
  // HttpServletRequestImpl methods
  //

  private AbstractHttpResponse getResponse()
  {
    AbstractHttpResponse response = _response;

    if (response == null)
      throw new IllegalStateException(L.l("{0} is not longer valid because it has already been closed.",
					  this));
    
    return response;
  }

  public AbstractHttpResponse getAbstractHttpResponse()
  {
    return _response;
  }


  public TcpDuplexController upgradeProtocol(TcpDuplexHandler handler)
  {
    return getResponse().upgradeProtocol(handler);
  }
  
  public void closeImpl()
    throws IOException
  {
    AbstractHttpResponse response = _response;

    _response = null;

    if (response != null)
      response.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
