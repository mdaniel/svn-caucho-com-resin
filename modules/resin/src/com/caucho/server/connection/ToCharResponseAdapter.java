/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletResponse;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.FreeList;

import com.caucho.log.Log;

import com.caucho.vfs.FlushBuffer;

public class ToCharResponseAdapter extends ResponseAdapter {
  private static final Logger log = Log.open(ToCharResponseAdapter.class);
  
  private static final FreeList<ToCharResponseAdapter> _freeList =
    new FreeList<ToCharResponseAdapter>(32);

  private ToCharResponseStreamWrapper _responseStream;

  private ToCharResponseAdapter(HttpServletResponse response)
  {
    super(response);
  }

  /**
   * Creates a new ResponseAdapter.
   */
  public static ToCharResponseAdapter create(HttpServletResponse response)
  {
    ToCharResponseAdapter resAdapt = _freeList.allocate();

    if (resAdapt == null)
      resAdapt = new ToCharResponseAdapter(response);
    else
      resAdapt.setResponse(response);

    resAdapt.init(response);

    return resAdapt;
  }

  protected AbstractResponseStream createWrapperResponseStream()
  {
    if (_responseStream == null)
      _responseStream = new ToCharResponseStreamWrapper();

    return _responseStream;
  }

  public void init(HttpServletResponse response)
  {
    _responseStream.start();
    
    super.init(response);
  }

  public void resetBuffer()
  {
    _responseStream.clearBuffer();

    super.resetBuffer();

    /*
    if (_currentWriter instanceof JspPrintWriter)
      ((JspPrintWriter) _currentWriter).clear();
    */
  }

  public static void free(ToCharResponseAdapter resAdapt)
  {
    resAdapt.free();

    _freeList.free(resAdapt);
  }

  class ToCharResponseStreamWrapper extends ToCharResponseStream {
    protected String getEncoding()
    {
      return getResponse().getCharacterEncoding();
    }

    /**
     * Flushes the buffer.
     */
    public void flushChar()
      throws IOException
    {
      flushBuffer();
      
      getResponse().getWriter().flush();
    }

    /**
     * Flushes the buffer.
     */
    public void close()
      throws IOException
    {
      // jsp/1730
      flushBuffer();
      
      getResponse().getWriter().close();
    }
    
    protected void writeNext(char []buffer, int offset, int length)
      throws IOException
    {
      getResponse().getWriter().write(buffer, offset, length);
    }
  }
}
