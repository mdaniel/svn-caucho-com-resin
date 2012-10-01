/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

import com.caucho.vfs.TempBuffer;

/**
 * Creates hashes for the identifiers.
 */
public class ResinDeflaterOutputStream extends OutputStream {
  private static FreeList<Deflater> _freeDeflaterList
    = new FreeList<Deflater>(16);

  private OutputStream _os;
  private Deflater _deflater;
  
  private byte [] _byteBuf = new byte[1];
  
  private TempBuffer _tempBuf;
  private byte [] _buf;
  
  /**
   * Creates the output
   */
  public ResinDeflaterOutputStream(OutputStream os)
  {
    init(os);
  }
  
  public void init(OutputStream os)
  {
    _os = os;
    
    _tempBuf = TempBuffer.allocate();
    _buf = _tempBuf.getBuffer();
    
    try {
      _deflater = _freeDeflaterList.allocate();

      if (_deflater == null)
        _deflater = new Deflater();
      else
        _deflater.reset();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(int value)
    throws IOException
  {
    _byteBuf[0] = (byte) value;
    
    write(_byteBuf, 0, 1);
  }

  @Override
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;
    
    Deflater def = _deflater;
    
    while (length > 0) {
      int sublen = length;
        
      if (_buf.length < sublen)
        sublen = _buf.length;
        
      def.setInput(buffer, offset, sublen);
        
      while (! def.needsInput()) {
        deflate();
      }
        
      offset += sublen;
      length -= sublen;
    }
  }

  /**
   * Close the stream
   */
  @Override
  public void close()
    throws IOException
  {
    Deflater deflater = _deflater;
    
    if (deflater == null)
      return;
    
    deflater.finish();
    while (! deflater.finished()) {
      deflate();
    }
    
    _deflater = null;
    
    // deflater.end();
    if (! _freeDeflaterList.free(deflater))
      deflater.end();
    
    _buf = null;
    TempBuffer tempBuf = _tempBuf;
    _tempBuf = null;
    
    TempBuffer.free(tempBuf);
  }
  
  private void deflate()
    throws IOException
  {
    int len = _deflater.deflate(_buf, 0, _buf.length);
    
    if (len > 0)
      _os.write(_buf, 0, len);
  }
}
