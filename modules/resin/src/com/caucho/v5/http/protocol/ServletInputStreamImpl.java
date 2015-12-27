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

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;

public class ServletInputStreamImpl extends ServletInputStream 
{
  private static final L10N L = new L10N(ServletInputStreamImpl.class);
  private static final Logger log
    = Logger.getLogger(ServletInputStreamImpl.class.getName());
  
  private RequestHttpBase _request;
  private ReadStream _is;

  public ServletInputStreamImpl(RequestHttpBase request)
  {
    _request = request;
  }

  public void init(ReadStream is)
  {
    _is = is;
  }

  @Override
  public int available() 
    throws IOException
  {
    InputStream is = _is;
    
    if (is != null) {
      return is.available();
    }
    else {
      return -1;
    }
  }

  /**
   * Reads a byte from the input stream.
   *
   * @return the next byte or -1 on end of file.
   */
  @Override
  public int read() 
    throws IOException
  {
    InputStream is = _is;
    
    if (is != null) {
      return is.read();
    }
    else {
      return -1;
    }
  }

  @Override
  public int read(byte []buf, int offset, int len) throws IOException
  {
    InputStream is = _is;
    
    if (is != null) {
      return is.read(buf, offset, len);
    }
    else {
      return -1;
    }
  }

  @Override
  public long skip(long n) 
    throws IOException
  {
    if (_is == null)
      return -1;
    else
      return _is.skip(n);
  }

  @Override
  public void close() 
    throws IOException
  {
  }

  public void free()
  {
    _is = null;
  }

  @Override
  public boolean isFinished()
  {
    InputStream is = _is;
    
    try {
      return is == null || is.available() < 0;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return true;
  }

  @Override
  public boolean isReady()
  {
    ReadStream is = _is;
    
    return is == null || is.isReady();
  }

  @Override
  public void setReadListener(ReadListener readListener)
  {
    if (! _request.isAsync()) {
      throw new IllegalStateException(L.l("setReadListener requires async"));
    }
    
    while (! isFinished()) {
      try {
        int ch = _is.read();
        
        if (ch < 0) {
          break;
        }
        
        _is.unread();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
        break;
      }

      readListener.onDataAvailable();
    }
    
    readListener.onAllDataRead();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _is + "]";
  }
}
