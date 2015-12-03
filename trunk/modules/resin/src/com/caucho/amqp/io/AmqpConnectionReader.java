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

import java.io.IOException;

import com.caucho.vfs.ReadStream;

/**
 * reads frames from a connection
 */
public class AmqpConnectionReader
{
  private final AmqpFrameHandler _handler;
  
  private ReadStream _is;
  private AmqpFrameReader _fin;
  private AmqpReader _ain;
  
  public AmqpConnectionReader(ReadStream is, AmqpFrameHandler handler)
  {
    _is = is;
    _handler = handler;

    _fin = new AmqpFrameReader();
    _fin.init(is);
    _ain = new AmqpReader();
    _ain.init(_fin);
  }
  
  public boolean readVersion()
    throws IOException
  {
    ReadStream is = _is;
    
    int ch = is.read();
    
    if (ch != 'A'
        || is.read() != 'M'
        || is.read() != 'Q'
        || is.read() != 'P') {
      System.out.println("ILLEGAL_HEADER: " + (char) ch);
      throw new IOException();
    }
    
    int code = is.read();
    boolean isSasl = false;
    
    switch (code) {
    case 0x00:
      isSasl = false;
      break;
    case 0x03:
      isSasl = true;
      break;
    default:
      System.out.println("BAD_CODE: " + code);
      throw new IOException("Unknown code");
    }
    
    int major = is.read() & 0xff;
    int minor = is.read() & 0xff;
    int version = is.read() & 0xff;
    
    if (major != 0x01 || minor != 0x00 || version != 0x00) {
      System.out.println("UNKNOWN_VERSION");
      throw new IOException();
    }
    
    return true;
  }

  public boolean readFrame()
    throws IOException
  {
    if (! _fin.startFrame()) {
      return false;
    }
    
    AmqpAbstractFrame frame = _ain.readObject(AmqpAbstractFrame.class);
    
    frame.invoke(_ain, _handler);
    
    _fin.finishFrame();
    
    return true;
  }
  
  public boolean readOpen()
    throws IOException
  {
    if (! _fin.startFrame()) {
      return false;
    }
    
    FrameOpen open = _ain.readObject(FrameOpen.class);
    
    _fin.finishFrame();
    
    return true;
  }
}
