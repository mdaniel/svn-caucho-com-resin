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

package com.caucho.server.webapp;

import com.caucho.server.connection.*;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * Internal response for an include() or forward()
 */
class IncludeResponse extends CauchoResponseWrapper
{
  private static final L10N L = new L10N(IncludeResponse.class);
  
  private final IncludeResponseStream2 _originalStream
    = new IncludeResponseStream2(this);
  
  private final ServletOutputStreamImpl _responseOutputStream
    = new ServletOutputStreamImpl();
  private final ResponseWriter _responsePrintWriter
    = new ResponseWriter();
  
  private AbstractResponseStream _stream;
  
  IncludeResponse()
  {
  }
  
  IncludeResponse(HttpServletResponse response)
  {
    super(response);
  }

  /**
   * Starts the request
   */
  void startRequest()
  {
    _originalStream.startRequest();
    _stream = _originalStream;

    _responseOutputStream.init(_stream);
    _responsePrintWriter.init(_stream);
  }

  /**
   * Finish request.
   */
  void finishRequest()
    throws IOException
  {
    _stream.close();
  }

  @Override
  public void close()
  {
  }

  /**
   * included response can't set the content type.
   */
  public void setContentType(String type)
  {
  }

  /**
   * Sets the ResponseStream
   */
  public void setResponseStream(AbstractResponseStream responseStream)
  {
    _stream = responseStream;

    _responseOutputStream.init(_stream);
    _responsePrintWriter.init(_stream);
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getResponseStream()
  {
    return _stream;
  }

  /**
   * Gets the response stream.
   */
  public AbstractResponseStream getOriginalStream()
  {
    return _originalStream;
  }

  /**
   * Returns true for a Caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return _stream.isCauchoResponseStream();
  }
  
  /**
   * Returns the ServletOutputStream for the response.
   */
  public ServletOutputStream getOutputStream() throws IOException
  {
    return _responseOutputStream;
  }

  /**
   * Returns a PrintWriter for the response.
   */
  public PrintWriter getWriter() throws IOException
  {
    /*
    if (! _hasWriter) {
      _hasWriter = true;

      if (_charEncoding != null && _responseStream != null)
	_responseStream.setEncoding(_charEncoding);
    }
    */
    
    return _responsePrintWriter;
  }

  /**
   * Returns the parent writer.
   */
  public PrintWriter getNextWriter()
  {
    return null;
  }

  public void setBufferSize(int size)
  {
    _stream.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _stream.getBufferSize();
  }

  public void flushBuffer()
    throws IOException
  {
    _stream.flush();
  }

  public void flushHeader()
    throws IOException
  {
    _stream.flushBuffer();
  }

  public void setDisableAutoFlush(boolean disable)
  {
    // XXX: _responseStream.setDisableAutoFlush(disable);
  }

  public void resetBuffer()
  {
    _stream.clearBuffer();

    // jsp/15ma
    // killCaching();
  }

  /**
   * Clears the response for a forward()
   *
   * @param force if not true and the response stream has committed,
   *   throw the IllegalStateException.
   */
  void reset(boolean force)
  {
    if (! force && _originalStream.isCommitted())
      throw new IllegalStateException(L.l("response cannot be reset() after committed"));
    
    _stream.clearBuffer();
  }

  public void clearBuffer()
  {
    _stream.clearBuffer();
  }
}
