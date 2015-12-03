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

import com.caucho.db.block.BlockStore;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public final class JournalResult
{
  private BlockStore _blockStore;
  
  private long _blockAddr1;
  private int _offset1;
  private int _len1;
  
  private long _blockAddr2;
  private int _offset2;
  private int _len2;
  
  public final void init1(BlockStore blockStore, 
                          long blockAddr, int offset, int len)
  {
    _blockStore = blockStore;
    
    _blockAddr1 = blockAddr;
    _offset1 = offset;
    _len1 = len;
  }
  
  public final void init2(long blockAddr, int offset, int len)
  {
    _blockAddr2 = blockAddr;
    _offset2 = offset;
    _len2 = len;
  }
  
  public final BlockStore getBlockStore()
  {
    return _blockStore;
  }
  
  public final long getBlockAddr1()
  {
    return _blockAddr1;
  }
  
  public final int getOffset1()
  {
    return _offset1;
  }
  
  public final int getLength1()
  {
    return _len1;
  }
  
  public final long getBlockAddr2()
  {
    return _blockAddr2;
  }
  
  public final int getOffset2()
  {
    return _offset2;
  }
  
  public final int getLength2()
  {
    return _len2;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + Long.toHexString(_blockAddr1)
            + ":" + _offset1 + "," + _len1 + "]");
  }
}
