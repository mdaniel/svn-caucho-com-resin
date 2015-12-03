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

package com.caucho.message.nautilus;

import java.io.IOException;
import java.io.InputStream;

/**
 * A chunk of journal data, part of a queue message.
 */
class MessageDataInputStream extends InputStream
{
  private MessageDataNode _head;
  private int _offset;
  private byte []_buffer = new byte[1]; 
  
  MessageDataInputStream(MessageDataNode head)
  {
    _head = head;
  }
  
  @Override
  public int read()
    throws IOException
  {
    int len = read(_buffer, 0, 1);
    
    if (len > 0)
      return _buffer[0] & 0xff;
    else
      return -1;
  }
  
  @Override
  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    MessageDataNode head = _head;
    
    if (head == null)
      return -1;
    
    int nodeLength = head.getLength();
    int nodeOffset = _offset;
    
    if (nodeLength <= nodeOffset) {
      _offset = 0;
      head = head.getNext();
      _head = null;
      
      if (head == null)
        return -1;
      
      nodeLength = head.getLength();
      nodeOffset = 0;
    }
    
    int sublen = nodeLength - nodeOffset;
    
    if (length < sublen)
      sublen = length;
    
    head.read(nodeOffset, buffer, offset, sublen);
    
    _offset += sublen;
    
    return sublen;
  }
}
