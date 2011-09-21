/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.TaskWorker;
import com.caucho.util.Alarm;

/**
 * Queue for blocks to be written.
 */
public class BlockWriteQueue {
  private final static Logger log
    = Logger.getLogger(BlockWriteQueue.class.getName());
  
  private final int _queueSize = 1024;
  
  // private final Block []_writeQueue;
  private final AtomicReferenceArray<Block> _writeQueue
    = new AtomicReferenceArray<Block>(_queueSize);
  
  private final AtomicInteger _head = new AtomicInteger();
  private final AtomicInteger _tail = new AtomicInteger();
  
  private final AtomicBoolean _isWait = new AtomicBoolean();

  /**
   * Adds a block that's needs to be flushed.
   */
  void addDirtyBlock(Block block)
  {
    int head;
    int tail;
    int nextHead;
    
    do {
      head = _head.get();
      nextHead = (head + 1) % _queueSize;
      
      tail = _tail.get();
      
      if (nextHead == tail) {
        synchronized (_isWait) {
          try {
            _isWait.set(true);
            _isWait.wait(100); 
          } catch (Exception e) {
            log.log(Level.FINER, e.toString(), e);
          }
        }
      }
    } while (! _head.compareAndSet(head, nextHead));
    
    _writeQueue.set(head, block);
  }

  boolean copyDirtyBlock(long blockId, Block block)
  {
    Block writeBlock = null;
    
    int head = _head.get();
    int tail = _tail.get();
    
    for (; head != tail; head = (head + _queueSize - 1) % _queueSize) {
      Block testBlock = _writeQueue.get(head);
      
      if (testBlock != null && testBlock.getBlockId() == block.getBlockId()) {
        return testBlock.copyToBlock(block);
      }
    }
    
    return false;
  }

  Block findBlock(long blockId)
  {
    int head = _head.get();
    int tail = _tail.get();
    
    for (; head != tail; head = (head + _queueSize - 1) % _queueSize) {
      Block testBlock = _writeQueue.get(head);
      
      if (testBlock != null && testBlock.getBlockId() == blockId) {
        return testBlock;
      }
    }
    
    return null;
  }
 
  void waitForComplete(long timeout)
  {
    long expireTime = Alarm.getCurrentTimeActual() + timeout;
    
    while (Alarm.getCurrentTimeActual() < expireTime) {
      int head = _head.get();
      int tail = _tail.get();
      
      if (head == tail)
        return;
      
      synchronized (_isWait) {
        _isWait.set(true);
        
        try {
          _isWait.wait(100);
        } catch (Exception e) {
          
        }
      }
    }
  }

  Block peekFirstBlock()
  {
    int tail = _tail.get();
    
    Block block = _writeQueue.get(tail);
    
    return block;
  }
  
  boolean isEmpty()
  {
    return _head.get() == _tail.get();
  }

  void removeFirstBlock()
  {
    int head;
    int tail;
    
    do {
      tail = _tail.get();
      head = _head.get();
      
      if (head == tail)
        return;
      
      // Block block = _writeQueue.get(tail);
      
      _writeQueue.set(tail, null);
    } while (! _tail.compareAndSet(tail, (tail + 1) % _queueSize));
    
    wake();
  }
  
  private void wake()
  {
    synchronized (_isWait) {
      if (_isWait.getAndSet(false)) {
        _isWait.notifyAll();
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
