/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic.context;

import com.caucho.portal.generic.PortletConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * A ResponseHandler that writes out to the connection.
 */
class TopLevelResponseHandler implements ResponseHandler
{
  private PortletConnection _connection;
  private ConnectionContext _context;

  public TopLevelResponseHandler( PortletConnection connection, 
                                  ConnectionContext context )
  {
    _connection = connection;
    _context = context;
  }

  public void finish()
    throws IOException
  {
  }

  public ResponseHandler getSuccessor()
  {
    return null;
  }

  /**
   * Set a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * Properties that begin with "Cache-" or not sent to the 
   * connection.
   *
   * @see javax.portlet.PortletResponse#setProperty
   */ 
  public void setProperty(String name, String value)
  {
    if (!name.startsWith("Cache-"))
      _connection.setProperty(name, value);
  }

  /**
   * Add a value to a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * Properties that begin with "Cache-" or not sent to the 
   * connection.
   *
   * @see javax.portlet.PortletResponse#addProperty
   */ 
  public void addProperty(String name, String value)
  {
    if (!name.startsWith("Cache-"))
      _connection.addProperty(name, value);
  }

  public void setLocale(Locale locale)
  {
    _connection.setLocale(locale);
  }

  public Locale getLocale()
  {
    return _connection.getLocale();
  }

  public void setContentType(String contentType)
  {
    _connection.setContentType(contentType);
  }

  public String getContentType()
  {
    return _connection.getContentType();
  }

  public void setBufferSize(int size)
  {
    _connection.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _connection.getBufferSize();
  }

  public void flushBuffer()
    throws IOException
  {
    _connection.flushBuffer();
  }

  public void resetBuffer()
  {
    _connection.resetBuffer();
  }

  public void reset()
  {
    _connection.reset();
  }

  public boolean isCommitted()
  {
    return _connection.isCommitted();
  }

  public void setCharacterEncoding(String enc)
    throws UnsupportedEncodingException
  {
    _connection.setCharacterEncoding(enc);
  }

  /**
   * Get the character encoding of the writer.
   */
  public String getCharacterEncoding()
  {
    return _connection.getCharacterEncoding();
  }

  /**
   * Get a writer that sends output to the client of the connection.
   */
  public PrintWriter getWriter()
    throws IOException
  {
    return _connection.getWriter();
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    return _connection.getOutputStream();
  }
}
