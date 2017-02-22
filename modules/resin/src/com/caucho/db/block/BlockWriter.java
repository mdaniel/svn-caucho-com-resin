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
import java.util.concurrent.TimeUnit;

import com.caucho.env.shutdown.ShutdownSystem;
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

  private final BlockWriteQueue _blockWriteQueue
    = new BlockWriteQueue(this);

  // private int _queueSize = 2 * 1024;
  private int _queueSize = 16 * 1024;

  private final RingValueQueue<Block> _blockWriteRing
    = new RingValueQueue<Block>(_queueSize);

  BlockWriter(BlockStore store)
  {
    _store = store;

    store.getReadWrite();
  }

  void addDirtyBlock(Block block)
  {
    addDirtyBlockNoWake(block);

    if (! isQueueEmpty()) {
      wake();
    }
  }
  
  /**
   * Close is not automatic on environment shutdown because of timing.
   */
  @Override
  protected boolean isWeakClose()
  {
    return false;
  }
  
  /**
   * Adds a block that's needs to be flushed.
   */

  void XX_addDirtyBlockNoWake(Block block)
  {
    boolean isWake = false;

    synchronized (_blockWriteQueue) {
      if (_blockWriteQueue.isFilled())
        isWake = true;

      _blockWriteQueue.addDirtyBlock(block);
    }

    if (isWake)
      wake();
  }

  void addDirtyBlockNoWake(Block block)
  {
    /*if (_queueSize <= 2 * _blockWriteRing.getSize()) {
    wake();
  }
  */

    if (_blockWriteRing.offer(block, 0, TimeUnit.SECONDS)) {
      return;
    }

    wake();
    
    if (isClosed()) {
      return;
    }

    // if (findBlock(block.getBlockId()) != block) {
    //System.err.println(" OFFER: " + Long.toHexString(block.getBlockId()));
    if (! _blockWriteRing.offer(block, 180, TimeUnit.SECONDS)) {
      String message = "OFFER_FAILED: " + block
                       + " head:" + _blockWriteRing.getHead()
                       + " tail:" + _blockWriteRing.getTail();

      ShutdownSystem.getCurrent().startFailSafeShutdown("shutting down due to: "
                                                        + message);
    }

    // }

    /*
    if (_queueSize <= 2 * _blockWriteRing.getSize()) {
      wake();
    }
    */
    wake();
  }

  boolean XX_copyDirtyBlock(long blockId, Block block)
  {
    Block writeBlock = null;

    synchronized (_blockWriteQueue) {
      writeBlock = _blockWriteQueue.findBlock(blockId);
    }

    if (writeBlock != null)
      return writeBlock.copyToBlock(block);
    else
      return false;
  }

  boolean copyDirtyBlock(long blockId, Block block)
  {
    Block writeBlock;

    boolean isCopy = false;
    
    do {
      synchronized (_blockWriteQueue) {
        writeBlock = findBlock(blockId);

	if (writeBlock != null) {
	  isCopy = writeBlock.copyToBlock(block);
	}
      }
    } while (writeBlock != null && ! isCopy);

    return writeBlock != null;
  }

  /*
  private Block findBlock(long blockId)
  {
    long head = _blockWriteRing.getHead();
    int size = _blockWriteRing.size();

     Block matchBlock = null;

    long ptr = head;
    for (int i = size + 4; i >= 0; i--) {
      Block testBlock = _blockWriteRing.getValue(ptr);

      if (testBlock != null
          && testBlock.getBlockId() == blockId
          && testBlock.isValid()) {
        // matchBlock = testBlock;
        return testBlock;
      }

      ptr--;
    }

     return matchBlock;
    //return null;
  }
  */

  private Block findBlock(long blockId)
  {
    long ptr = _blockWriteRing.getTail();
    Block matchBlock = null;

    while (ptr < _blockWriteRing.getHead()) {
      Block testBlock = _blockWriteRing.getValue(ptr);

      if (testBlock == null) {
        ptr = Math.max(ptr, _blockWriteRing.getTail());
        continue;
      }
      else {
        ptr++;
      }

      if (testBlock.getBlockId() == blockId) {
        matchBlock = testBlock;
        /*
        if (testBlock.isValid()) {
          matchBlock = testBlock;
        }
        else {
          System.err.println("INVALID: " + testBlock);
        }
        */
      }
    }

    return matchBlock;
  }

  /*
  @Override
  public boolean isClosed()
  {
    // return super.isClosed() && _blockWriteQueue.isEmpty();

    return super.isClosed() && _blockWriteRing.isEmpty();
  }
  */

  boolean XX_waitForComplete(long timeout)
  {
    wake();

    _blockWriteQueue.waitForComplete(timeout);

    return true;
  }

  boolean waitForComplete(long timeout)
  {
    long expire = CurrentTime.getCurrentTimeActual() + timeout;
    
    boolean isEmpty = false;

    do {
      wake();
      try {
        Thread.sleep(10);
      } catch (Exception e) {
      }
    } while (! isClosed()
             && ! (isEmpty = _blockWriteRing.isEmpty())
             && CurrentTime.getCurrentTimeActual() < expire);
    
    return isClosed() || isEmpty;
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

          //System.err.println("WRITE: " + Long.toHexString(block.getBlockId()));
          try {
            block.writeFromBlockWriter();
          } finally {
            removeFirstBlock();
          }
        }
        else if (isQueueEmpty() && retry-- <= 0) {
          return -1;
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return -1;
  }

  private boolean isQueueEmpty()
  {
    // return _blockWriteQueue.isEmpty();
    return _blockWriteRing.isEmpty();
  }
  
  public void wakeIfPending()
  {
    if (! isQueueEmpty()) {
      wake();
    }
  }

  private Block peekFirstBlock()
  {
    /*
    synchronized (_blockWriteQueue) {
      return _blockWriteQueue.peekFirstBlock();
    }
    */

    return _blockWriteRing.peek();
  }

  private void removeFirstBlock()
  {
    /*
    synchronized (_blockWriteQueue) {
      _blockWriteQueue.removeFirstBlock();
    }
    */

    synchronized (_blockWriteRing) {
      _blockWriteRing.poll();
    }
  }

  @Override
  protected void onThreadStart()
  {
  }

  @Override
  protected void onThreadComplete()
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _store + "]";
  }
}
