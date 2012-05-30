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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.AbstractTaskWorker;
import com.caucho.util.CurrentTime;
import com.caucho.util.RingValueQueue;

/**
 * Writer thread serializing dirty blocks.
 */
public class BlockWriter extends AbstractTaskWorker {
  private final static Logger log
    = Logger.getLogger(BlockWriter.class.getName());
  
  private final BlockStore _store;
  
  private int _queueSize = 1024;
  // private int _writeQueueMax = 256;
  
  private final RingValueQueue<Block> _blockWriteQueue
    = new RingValueQueue<Block>(_queueSize);
  
  BlockWriter(BlockStore store)
  {
    _store = store;
    
    store.getReadWrite();
  }

  void addDirtyBlock(Block block)
  {
    addDirtyBlockNoWake(block);
    
    wake();
  }
  /**
   * Adds a block that's needs to be flushed.
   */
  void addDirtyBlockNoWake(Block block)
  {
    if (_queueSize >= 2 * _blockWriteQueue.getSize()) {
      wake();
    }
    
    _blockWriteQueue.offer(block);
  }

  boolean copyDirtyBlock(long blockId, Block block)
  {
    int head = _blockWriteQueue.getHeadAlloc();
    int tail = _blockWriteQueue.getTail();
    
    for (; head != tail; tail = _blockWriteQueue.nextIndex(tail)) {
      Block writeBlock = _blockWriteQueue.getValue(tail);
      
      if (writeBlock != null && writeBlock.getBlockId() == blockId) {
        return writeBlock.copyToBlock(block);
      }
    }
    
    return false;
  }
  
  @Override
  public boolean isClosed()
  {
    return super.isClosed() && _blockWriteQueue.isEmpty();
    
    // return super.isClosed() && _blockWriteQueue.isEmpty();
  }
  
  boolean waitForComplete(long timeout)
  {
    wake();
    
    long expire = CurrentTime.getCurrentTimeActual() + timeout;
    
    while (! _blockWriteQueue.isEmpty()
           && CurrentTime.getCurrentTimeActual() < expire) {
      try {
        Thread.sleep(10);
      } catch (Exception e) {
        
      }
    }
    
    return ! _blockWriteQueue.isEmpty();
  }
  
  @Override
  public long runTask()
  {
    try {
      int retryMax = 25;
      int retry = retryMax;

      while (true) {
        Block block = peekFirstBlock();

        if (block != null) {
          retry = retryMax;

          try {
            block.writeFromBlockWriter();
          } finally {
            removeFirstBlock();
          }
        }
        else if (retry-- <= 0) {
          return -1;
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    return -1;
  }
  
  @Override
  protected void onThreadStart()
  {
  }
  
  @Override
  protected void onThreadComplete()
  {
  }

  private Block peekFirstBlock()
  {
    /*
    synchronized (_writeQueue) {
      if (_writeQueue.size() > 0) {
        Block block = _writeQueue.get(0);
        
        return block;
      }
    }
    
    return null;
    */
    
    return _blockWriteQueue.peek();
  }

  private void removeFirstBlock()
  {
    /*
    synchronized (_writeQueue) {
      if (_writeQueue.size() > 0) {
        _writeQueue.remove(0);
        
        _writeQueue.notifyAll();
      }
    }
    */
    
    _blockWriteQueue.poll();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _store + "]";
  }
}
