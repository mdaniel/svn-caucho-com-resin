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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public interface ResponseHandler
{
  /**
   * Get the handler that this handler wraps, if any
   */
  public ResponseHandler getSuccessor();

  /**
   * Set a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * @see javax.portlet.PortletResponse#setProperty
   */ 
  public void setProperty(String name, String value);

  /**
   * Add a value to a property to be returned to the client.
   *
   * "properties" correspond to HTTP headers in the response for HTTP
   * connections.
   *
   * @see javax.portlet.PortletResponse#addProperty
   */ 
  public void addProperty(String name, String value);


  public void setContentType(String contentType);

  /**
   * Get the type previously set with setContentType
   * or null if the type has not been set.
   */ 
  public String getContentType();

  public void setLocale(Locale locale);

  public Locale getLocale();

  public void setBufferSize(int size);

  public int getBufferSize();

  /** 
   * Implementations should NOT call flushBuffer() on any wrapped
   * streams, flushing through the chain of ResponseHandlers is done by
   * ConnectionContext.
   */
  public void flushBuffer() 
    throws IOException;

  /** 
   * Implementations should NOT call resetBuffer() on any wrapped
   * streams, resetBuffer() through the chain of ResponseHandlers is done by
   * ConnectionContext.
   */
  public void resetBuffer();

  /** 
   * Implementations should call NOT call reset() on any wrapped streams.
   * streams, reset() through the chain of ResponseHandlers is done by
   * ConnectionContext.
   */
  public void reset();

  public boolean isCommitted();
  
  /**
   * Set the character encoding of the writer.  
   */
  public void setCharacterEncoding(String enc) 
    throws UnsupportedEncodingException;

  /**
   * Get the character encoding of the writer.
   */
  public String getCharacterEncoding();

  /**
   * Get a writer that sends output to the client of the connection.
   */
  public PrintWriter getWriter() 
    throws IOException;

  public OutputStream getOutputStream() 
    throws IOException;

  public void finish()
    throws IOException;

}
