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

package com.caucho.db.block;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;

/**
 * Queue for blocks to be written.
 */
public class BlockWriteQueue {
  private final static Logger log
    = Logger.getLogger(BlockWriteQueue.class.getName());
  
  private final BlockWriter _writer;
  
  private final int _queueSize = 1024;
  
  // private final Block []_writeQueue;
  private final Block []_writeQueue = new Block[_queueSize];
  
  private int _head;
  private int _tail;
  
  private boolean _isWait;
  
  BlockWriteQueue(BlockWriter writer)
  {
    _writer = writer;
  }

  /**
   * Adds a block that needs to be flushed.
   */
  void addDirtyBlock(Block block)
  {
    int head;
    int nextHead;

    Block foundBlock = findBlock(block.getBlockId());
    
    if (foundBlock == block)
      return;

    while (true) {
      head = _head;
      nextHead = (head + 1) % _queueSize;
      
      if (nextHead != _tail) {
        _writeQueue[head] = block;
        _head = nextHead;
        return;
      }
      
      _writer.wake();
        
      try {
        _isWait = true;
        wait(100); 
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  Block peekFirstBlock()
  {
    Block block = _writeQueue[_tail];
    
    return block;
  }
  
  void removeFirstBlock()
  {
    int tail = _tail;
    int head = _head;
    
    if (head == tail)
      throw new IllegalStateException();

    _writeQueue[tail] = null;
    _tail = (tail + 1) % _queueSize;
    
    wake();
  }
  

  Block findBlock(long blockId)
  {
    int m1 = _queueSize - 1;
    
    int ptr = (_head + m1) % _queueSize;;
    int prevTail = (_tail + m1) % _queueSize;
    
    for (; ptr != prevTail; ptr = (ptr + m1) % _queueSize) {
      Block testBlock = _writeQueue[ptr];
      
      if (testBlock != null && testBlock.getBlockId() == blockId) {
        return testBlock;
      }
    }
    
    return null;
  }
 
  boolean waitForComplete(long timeout)
  {
    long expireTime = CurrentTime.getCurrentTimeActual() + timeout;
    
    while (CurrentTime.getCurrentTimeActual() < expireTime) {
      if (isEmpty())
        return true;
      
      _isWait = true;
        
      try {
        wait(100);
      } catch (Exception e) {
      }
    }
    
    return false;
  }
  
  boolean isEmpty()
  {
    return _head == _tail;
  }
  
  boolean isFilled()
  {
    int length = (_head - _tail + _queueSize) % _queueSize;
    
    return _queueSize <= 4 * length;
  }

  private void wake()
  {
    boolean isWait = _isWait;
    _isWait = false;
    
    if (isWait) {
      notifyAll();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
