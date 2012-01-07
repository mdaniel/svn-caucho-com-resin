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

package com.caucho.mqueue;

import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 */
public final class MQueueItem
{
  private int _code;
  
  // XXX: to, from, queryId
  private String _to;
  private String _from;
  private long _queryId;
  
  private long _sequence;
  private long _xid;
  private TempBuffer _buffer;
  private int _offset;
  private int _length;
  
  public void init(int code, long sequence, long xa, 
                   TempBuffer buffer, int offset, int length)
  {
    _code = code;
    _sequence = sequence;
    _xid = xa;
    _buffer = buffer;
    _offset = offset;
    _length = length;
  }
  
  public int getCode()
  {
    return _code;
  }
  
  public long getSequence()
  {
    return _sequence;
  }
  
  public long getXid()
  {
    return _xid;
  }
  
  public TempBuffer getBuffer()
  {
    return _buffer;
  }
  
  public int getOffset()
  {
    return _offset;
  }
  
  public int getLength()
  {
    return _length;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
