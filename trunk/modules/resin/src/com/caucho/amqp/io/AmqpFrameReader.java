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

package com.caucho.amqp.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

public class AmqpFrameReader extends InputStream {
  private static final Logger log
    = Logger.getLogger(AmqpFrameReader.class.getName());
  
  private static final L10N L = new L10N(AmqpFrameReader.class);
  
  private ReadStream _is;
  
  private int _size;
  private int _doff;
  private int _type;
  private int _extra;
  
  private int _offset;
  
  public AmqpFrameReader()
  {
    
  }
  
  public AmqpFrameReader(ReadStream is)
  {
    init(is);
  }
  
  public void init(ReadStream is)
  {
    _is = is;
  }
  
  public int getSize()
  {
    return _size;
  }
  
  public int getOffset()
  {
    return _offset;
  }
  
  public boolean startFrame()
    throws IOException
  {
    ReadStream is = _is;
    
    int ch;
    
    try {
      ch = is.read();
    
      if (ch < 0) {
        return false;
      }
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    }
    
    _size = (((ch & 0xff) << 24)
        + ((is.read() & 0xff) << 16)
        + ((is.read() & 0xff) << 8)
        + ((is.read() & 0xff)));
    
    _doff = is.read() & 0xff;
    
    _type = is.read() & 0xff;
    
    int e1 = is.read() & 0xff;
    ch = is.read();
    
    if (ch < 0)
      throw new EOFException(L.l("Unexpected end of file"));
    
    _extra = (((e1 & 0xff) << 8) + (ch & 0xff));
    
    _offset = 8;
    
    return true;
  }
  
  public void finishFrame()
    throws IOException
  {
    int delta = _size - _offset;
    _offset = _size;

    if (delta > 0) {
      _is.skip(delta);
    }
  }
  
  @Override
  public int read()
    throws IOException
  {
    if (_size <= _offset) {
      return -1;
    }
    
    _offset++;
    
    int value = _is.read();
    
    return value;
  }
}
