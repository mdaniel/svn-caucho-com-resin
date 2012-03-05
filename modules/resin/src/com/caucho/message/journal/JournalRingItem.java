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

package com.caucho.message.journal;

import com.caucho.util.RingItem;
import com.caucho.vfs.TempBuffer;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public class JournalRingItem extends RingItem
{
  private boolean _isData;
  
  private boolean _isInit = true;
  private boolean _isFin = true;
  private long _code;
  private long _xid;
  private long _qid;
  private long _mid;
  
  private byte []_buffer;
  private int _offset;
  private int _length;
  
  private long _blockAddr;
  

  private final JournalResult _result = new JournalResult();
  
  private TempBuffer _tBuf;
  
  public JournalRingItem(int index)
  {
    super(index);
  }
  
  public final void init(long code, 
                         long xid, long qid, long mid,
                         byte []buffer, int offset, int length,
                         TempBuffer tBuf)
  {
    _isData = true;
    
    _code = code;
    _xid = xid;
    _qid = qid;
    _mid = mid;
    
    _buffer = buffer;
    _offset = offset;
    _length = length;
    
    _tBuf = tBuf;
  }
  
  public final void initCheckpoint(long blockAddr, int offset, int length)
  {
    _isData = false;
    
    _code = JournalFile.OP_CHECKPOINT;
    _blockAddr = blockAddr;
    _offset = offset;
    _length = length;
  }
  
  public final void setCode(long code)
  {
    _code = code;
  }
  
  public final void init(long code, long qid)
  {
    _code = code;
    _qid = qid;
  }
 
  public final boolean isData()
  {
    return _isData;
  }
  
  public final boolean isInit()
  {
    return _isInit;
  }
  
  public final boolean isFin()
  {
    return _isFin;
  }
  
  public final long getCode()
  {
    return _code;
  }
  
  public final long getXid()
  {
    return _xid;
  }
  
  public final long getQid()
  {
    return _qid;
  }
  
  public final long getMid()
  {
    return _mid;
  }
  
  public final byte []getBuffer()
  {
    return _buffer;
  }
  
  public final int getOffset()
  {
    return _offset;
  }
  
  public final int getLength()
  {
    return _length;
  }
  
  public final void freeTempBuffer()
  {
    TempBuffer tBuf = _tBuf;
    _tBuf = null;
    
    _buffer = null;
    
    if (tBuf != null) {
      tBuf.freeSelf();
    }
  }
  
  public final JournalResult getResult()
  {
    return _result;
  }
  
  public final long getBlockAddr()
  {
    return _blockAddr;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _code + "," + _qid + "," + _mid + "]";
  }
}
