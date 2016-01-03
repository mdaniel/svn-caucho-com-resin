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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.filters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.http.protocol.OutResponseToByte;
import com.caucho.v5.io.TempBuffer;

public class FilterWrapperResponseStream extends OutResponseToByte
{
  private static final Logger log
    = Logger.getLogger(FilterWrapperResponseStream.class.getName());
  
  private CauchoResponseWrapper _response;
  
  private OutputStream _os;
  
  public FilterWrapperResponseStream()
  {
  }

  public void init(CauchoResponseWrapper response)
  {
    _response = response;
    _os = null;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  /*
  @Override
  protected void flushDataBuffer(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    OutputStream os = getStream();
    
    if (os != null) {
      os.write(buf, offset, length);
    }
  }
  */

  @Override
  protected TempBuffer flushData(TempBuffer head, TempBuffer tail, boolean isEnd)
  {
    try {
      OutputStream os = getStream();

      if (os != null) {
        // os.write(buf, offset, length);
      }

      TempBuffer next = head.getNext();

      if (next != null) {
        head.setNext(null);
        TempBuffer.freeAll(next);;
      }

      return head;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
  }

  /**
   * flushing
   */
  @Override
  public void flush()
    throws IOException
  {
    super.flush();
    
    // flushBuffer();

    OutputStream os = getStream();

    if (os != null)
      os.flush();
  }

  /**
   * Gets the stream.
   */
  private OutputStream getStream()
    throws IOException
  {
    if (_os != null)
      return _os;
    else if (_response != null)
      _os = _response.getStream();

    return _os;
  }

  /**
   * Close.
   */
  public void closeImpl()
    throws IOException
  {
    super.closeImpl();

    _response = null;
    
    OutputStream os = _os;
    _os = null;
    // server/1839
    
    if (os != null)
      os.close();
  }
}
