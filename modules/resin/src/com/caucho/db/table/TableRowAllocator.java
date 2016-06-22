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

package com.caucho.db.table;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.xa.DbTransaction;
import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.inject.Module;
import com.caucho.util.Friend;

/**
 * Table format:
 *
 * <pre>
 * Block 0: allocation table
 * Block 1: fragment table
 * Block 2: table definition
 *   0    - store data
 *   1024 - table data
 *    1024 - index pointers
 *   2048 - CREATE text
 * Block 3: first data
 * </pre>
 */
@Module
class TableRowAllocator extends AbstractTaskWorker {
  private final static Logger log
    = Logger.getLogger(TableRowAllocator.class.getName());

  private static final int FREE_ROW_BLOCK_SIZE = 256;

  public final static long ROW_CLOCK_MIN = 1024;

  private final Table _table;

  private final int _rowLength;
  private final int _rowsPerBlock;
  private final int _rowEnd;

  private final AtomicLongArray _insertFreeRowBlockArray
    = new AtomicLongArray(FREE_ROW_BLOCK_SIZE);
  private final AtomicInteger _insertFreeRowBlockHead
    = new AtomicInteger();
  private final AtomicInteger _insertFreeRowBlockTail
    = new AtomicInteger();

  private long _rowTailTop = BlockStore.BLOCK_SIZE * FREE_ROW_BLOCK_SIZE;
  private final AtomicLong _rowTailOffset = new AtomicLong();

  // clock counters for row insert allocation
  private long _rowClockTop;
  private long _rowClockOffset;

  private long _clockRowFree;
  private long _clockRowUsed;
  
  private long _clockBlockFree;
  private long _clockRowDeleteCount;

  TableRowAllocator(Table table)
  {
    _table = table;
    
    _rowsPerBlock = table.getRowsPerBlock();
    _rowLength = table.getRowLength();
    _rowEnd = table.getRowEnd();
  }

  @Friend(Table.class)
  int allocateRow(Block block, DbTransaction xa)
    throws IOException, SQLException, InterruptedException
  {
    Lock blockLock = block.getWriteLock();

    blockLock.tryLock(xa.getTimeout(), TimeUnit.MILLISECONDS);
    try {
      block.read();

      byte []buffer = block.getBuffer();

      int rowOffset = 0;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          buffer[rowOffset] = Table.ROW_ALLOC;

          block.setDirty(rowOffset, rowOffset + 1);

          return rowOffset;
        }
      }
    } finally {
      blockLock.unlock();
    }

    return -1;
  }

  //
  // row allocation
  //

  @Friend(Table.class)
  long allocateInsertRowBlock()
    throws IOException
  {
    long blockId = allocateRowBlockId();

    if (blockId != 0) {
      return blockId;
    }

    long rowTailOffset = _rowTailOffset.get();

    blockId = _table.firstRowBlock(rowTailOffset);

    if (blockId <= 0) {
      Block block = _table.allocateRow();

      blockId = block.getBlockId();
      // System.out.println("ALLOC: " + blockId + " " + _rowTailOffset.get() + " " + _rowTailTop);

      block.free();
    }

    _rowTailOffset.compareAndSet(rowTailOffset, blockId + BlockStore.BLOCK_SIZE);
    
    return blockId;
  }

  //
  // allocator worker/single threaded
  //

  private void fillFreeRows()
  {
    if (_rowTailOffset.get() < _rowTailTop || isClosed()) {
      return;
    }
    
    while (scanClock()) {
      if (! resetClock()) {
        return;
      }
    }
  }
  
  private boolean scanClock()
  {
    while (! isClosed () && isFreeRowBlockIdAvailable()) {
      long clockBlockId = _rowClockOffset;

      try {
        clockBlockId = _table.firstRowBlock(clockBlockId);
        
        if (clockBlockId < 0) {
          _rowClockOffset = _rowClockTop;
          
          return true;
        }

        if (isRowBlockFree(clockBlockId)) {
          _clockBlockFree++;
          freeRowBlockId(clockBlockId);
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        clockBlockId = _rowClockTop;
      } finally {
        _rowClockOffset = clockBlockId + BlockStore.BLOCK_SIZE;
      }
    }
    
    return false;
  }

  private boolean resetClock()
  {
    // force 50% free rows before clock starts again
    long newRowCount = (_clockRowUsed - _clockRowFree) / _rowsPerBlock;
    
    long tableRowDeleteCount = _table.getRowDeleteCount();
    long deleteRowCount = (tableRowDeleteCount - _clockRowDeleteCount) / _rowsPerBlock;

    if (_clockRowFree < ROW_CLOCK_MIN && _rowClockOffset > 0) {
      // minimum 256 blocks of free rows
      newRowCount = ROW_CLOCK_MIN;
    }
    else if (deleteRowCount < ROW_CLOCK_MIN) {
      // if none deleted, don't clock
      newRowCount = ROW_CLOCK_MIN;
    }
    
    _rowClockOffset = 0;
    _rowClockTop = _rowTailOffset.get();
    _clockRowUsed = 0;
    _clockRowFree = 0;
    _clockRowDeleteCount = tableRowDeleteCount;
    
    if (newRowCount > 0) {
      _rowTailTop = _rowTailOffset.get() + newRowCount * _rowLength;
      // _rowClockOffset = _rowTailTop;
      
      return false;
    }
    else {
      // System.out.println("RESET: used=" + _clockRowUsed + " free=" + _clockRowFree + " top=" + _rowTailTop);

      return true;
    }
  }
  
  /**
   * Test if any row in the block is free
   */
  private boolean isRowBlockFree(long blockId)
    throws IOException
  {
    if (isClosed())
      return false;
    
    Block block = _table.readBlock(blockId);

    try {
      int rowOffset = 0;

      byte []buffer = block.getBuffer();
      boolean isFree = false;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          isFree = true;
          _clockRowFree++;
        }
        else
          _clockRowUsed++;
      }

      return isFree;
    } finally {
      block.free();
    }
  }

  private boolean isFreeRowBlockIdAvailable()
  {
    int head = _insertFreeRowBlockHead.get();
    int tail = _insertFreeRowBlockTail.get();
    
    return (head + 1) % FREE_ROW_BLOCK_SIZE != tail;
  }

  private long allocateRowBlockId()
  {
    while (true) {
      int tail = _insertFreeRowBlockTail.get();
      int head = _insertFreeRowBlockHead.get();
      
      if (head == tail) {
        wake();
        return 0;
      }
    
      long blockId = _insertFreeRowBlockArray.getAndSet(tail, 0);
      
      int nextTail = (tail + 1) % FREE_ROW_BLOCK_SIZE;
      
      _insertFreeRowBlockTail.compareAndSet(tail, nextTail);
      
      if (blockId > 0) {
        int size = (head - tail + FREE_ROW_BLOCK_SIZE) % FREE_ROW_BLOCK_SIZE;
        
        if (2 * size < FREE_ROW_BLOCK_SIZE) {
          wake();
        }
        
        return blockId;
      }
    }
  }

  @Friend(Table.class)
  void freeRowBlockId(long blockId)
  {
    while (true) {
      int head = _insertFreeRowBlockHead.get();
      int tail = _insertFreeRowBlockTail.get();
      
      int nextHead = (head + 1) % FREE_ROW_BLOCK_SIZE;
      
      if (nextHead == tail)
        return;
      
      _insertFreeRowBlockHead.compareAndSet(head, nextHead);
      
      if (_insertFreeRowBlockArray.compareAndSet(head, 0, blockId))
        return;
    }
  }

  @Override
  public long runTask()
  {
    fillFreeRows();
      
    return -1;
  }
}
