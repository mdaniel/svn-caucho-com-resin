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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.util.FreeList;

public class ResponseAdapterToChar extends ResponseAdapter {
  private static final FreeList<ResponseAdapterToChar> _freeList
    = new FreeList<ResponseAdapterToChar>(32);

  private ToCharResponseStreamWrapper _responseStream;

  private ResponseAdapterToChar()
  {
  }

  /**
   * Creates a new ResponseAdapter.
   */
  public static ResponseAdapterToChar create(HttpServletResponse response)
  {
    ResponseAdapterToChar resAdapt = _freeList.allocate();

    if (resAdapt == null) {
      resAdapt = new ResponseAdapterToChar();
    }
    
    resAdapt.init(response);

    return resAdapt;
  }

  @Override
  protected OutResponseBase createWrapperResponseStream()
  {
    if (_responseStream == null) {
      _responseStream = new ToCharResponseStreamWrapper();
    }

    return _responseStream;
  }

  @Override
  public void init(HttpServletResponse response)
  {
    _responseStream.start();
    
    super.init(response);
  }

  @Override
  public void resetBuffer()
  {
    _responseStream.clearBuffer();

    super.resetBuffer();

    /*
    if (_currentWriter instanceof JspPrintWriter)
      ((JspPrintWriter) _currentWriter).clear();
    */
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getResponse() + "]";
  }

  public static void free(ResponseAdapterToChar resAdapt)
  {
    resAdapt.free();

    _freeList.free(resAdapt);
  }

  class ToCharResponseStreamWrapper extends OutResponseToChar {
    ToCharResponseStreamWrapper()
    {
    }
    
    @Override
    public String getEncoding()
    {
      return getResponse().getCharacterEncoding();
    }

    /**
     * Flushes the buffer.
     */
    @Override
    public void flushChar()
      throws IOException
    {
      flushBuffer();
      
      getResponse().getWriter().flush();
    }

    /**
     * Flushes the buffer.
     */
    @Override
    public void flush()
      throws IOException
    {
      super.flush();
     
      // server/1732
      getResponse().getWriter().flush();
    }

    /**
     * Flushes the buffer.
     */
    /*
    @Override
    public void close()
      throws IOException
    {
      // jsp/1730
      flushBuffer();

      // server/172q
      // getResponse().getWriter().close();
    }
    */
    
    @Override
    protected void writeNext(char []buffer, int offset, int length)
      throws IOException
    {
      getResponse().getWriter().write(buffer, offset, length);
    }
  }
}
