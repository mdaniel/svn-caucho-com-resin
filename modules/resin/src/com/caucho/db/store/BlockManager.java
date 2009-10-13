/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.db.store;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.BlockManagerMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LongKeyLruCache;
import com.caucho.util.TaskWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Manages the block cache
 */
public final class BlockManager
  extends AbstractManagedObject
  implements BlockManagerMXBean
{
  private static final Logger log
    = Logger.getLogger(BlockManager.class.getName());
  private static final L10N L = new L10N(BlockManager.class);

  private static BlockManager _staticManager;

  private final byte []_storeMask = new byte[8192];
  private LongKeyLruCache<Block> _blockCache;

  private final Object _copyLock = new Object();
  private final ArrayList<Block> _writeQueue = new ArrayList<Block>();
  private int _writeQueueMax = 256;

  private final AtomicLong _blockWriteCount = new AtomicLong();
  private final AtomicLong _blockReadCount = new AtomicLong();

  private final BlockManagerWriter _writer;

  private BlockManager(int capacity)
  {
    super(ClassLoader.getSystemClassLoader());

    _blockCache = new LongKeyLruCache<Block>(capacity);

    // the first store id is not available to allow for tests for zero.
    _storeMask[0] |= 1;

    registerSelf();

    _writer = new BlockManagerWriter();
  }

  /**
   * Returns the block manager, ensuring a minimum number of entries.
   */
  public static synchronized BlockManager create()
  {
    if (_staticManager == null) {
      int minEntries = (int) defaultCapacity();

      _staticManager = new BlockManager(minEntries);
    }

    return _staticManager;
  }

  private static long defaultCapacity()
  {
    long minSize = 1 * 1024 * 1024;

    long memorySize = getMaxMemory() / 64;

    if (memorySize < minSize)
      memorySize = minSize;

    return memorySize / Store.BLOCK_SIZE;
  }

  private static long getMaxMemory()
  {
    try {
      MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
      MemoryUsage heap = memoryBean.getHeapMemoryUsage();

      if (heap.getCommitted() < heap.getMax())
        return heap.getMax();
      else
        return heap.getCommitted();
    } catch (Exception e) {
      e.printStackTrace();
    }
      
    return Runtime.getRuntime().maxMemory();
  }

  public static BlockManager getBlockManager()
  {
    return _staticManager;
  }

  /**
   * Ensures the cache has a minimum number of blocks.
   *
   * @param minCapacity the minimum capacity in blocks
   */
  public void ensureCapacity(int minCapacity)
  {
    _blockCache = _blockCache.ensureCapacity(minCapacity);
  }

  /**
   * Ensures the cache has a minimum number of blocks.
   *
   * @param minCapacity the minimum capacity in blocks
   */
  public void setCapacity(int minCapacity)
  {
    if (minCapacity > 1024 * 1024 / Store.BLOCK_SIZE)
      _blockCache = _blockCache.setCapacity(minCapacity);
  }

  /**
   * Allocates a store id.
   */
  public int allocateStoreId()
  {
    synchronized (_storeMask) {
      for (int i = 0; i < _storeMask.length; i++) {
        int mask = _storeMask[i];

        if (mask != 0xff) {
          for (int j = 0; j < 8; j++) {
            if ((mask & (1 << j)) == 0) {
              _storeMask[i] |= (1 << j);

              return 8 * i + j;
            }
          }
        }
      }

      throw new IllegalStateException(L.l("All store ids used."));
    }
  }

  /**
   * Frees blocks with the given store.
   */
  public void flush(Store store)
  {
    ArrayList<Block> dirtyBlocks = null;

    synchronized (_blockCache) {
      Iterator<Block> values = _blockCache.values();

      while (values.hasNext()) {
        Block block = values.next();

        if (block != null && block.getStore() == store) {
          if (block.isDirty()) {
            if (dirtyBlocks == null)
              dirtyBlocks = new ArrayList<Block>();

            dirtyBlocks.add(block);
          }
        }
      }
    }

    for (int i = 0; dirtyBlocks != null && i < dirtyBlocks.size(); i++) {
      Block block = dirtyBlocks.get(i);

      try {
        synchronized (block) {
          block.write();
        }
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  /**
   * Frees blocks with the given store.
   */
  public void freeStore(Store store)
  {
    ArrayList<Block> removeBlocks = new ArrayList<Block>();

    synchronized (_blockCache) {
      Iterator<Block> iter = _blockCache.values();

      while (iter.hasNext()) {
        Block block = iter.next();

        if (block != null && block.getStore() == store)
          removeBlocks.add(block);
      }
    }

    for (Block block : removeBlocks) {
      _blockCache.remove(block.getBlockId());
    }

    long expires = Alarm.getCurrentTimeActual() + 60000L;

    synchronized (_writeQueue) {
      while (hasPendingStore(store)) {
        _writer.wake();
        try {
          long delta = expires - Alarm.getCurrentTimeActual();

          if (delta > 0)
            _writeQueue.wait(delta);
          else {
            log.warning(this + " block close for " + store + " timeout");
            break;
          }
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private boolean hasPendingStore(Store store)
  {
    for (int i = _writeQueue.size() - 1; i >= 0; i--) {
      Block block = _writeQueue.get(i);

      if (block.getStore() == store)
        return true;
    }

    return false;
  }

  /**
   * Frees a store id.
   */
  public void freeStoreId(int storeId)
  {
    synchronized (_storeMask) {
      if (storeId <= 0)
        throw new IllegalArgumentException(String.valueOf(storeId));

      _storeMask[storeId / 8] &= ~(1 << storeId % 8);
    }
  }

  /**
   * Gets the table's block.
   */
  Block getBlock(Store store, long blockId)
  {
    // XXX: proper handling of the synchronized is tricky because
    // the LRU dirty write might have timing issues

    long storeId = blockId & Store.BLOCK_INDEX_MASK;
    if (storeId != store.getId()) {
      throw stateError("illegal block: " + Long.toHexString(blockId));
    }

    Block block = _blockCache.get(blockId);

    while (block == null || ! block.allocate()) {
      // Find any matching block in the process of being written
      Block dirtyBlock = null;
      synchronized (_writeQueue) {
        int size = _writeQueue.size();

        for (int i = 0; i < size; i++) {
          dirtyBlock = _writeQueue.get(i);

          if (dirtyBlock.getBlockId() == blockId && dirtyBlock.allocate()) {
            break;
          }
          else
            dirtyBlock = null;
        }
      }

      /*
      if ((blockId & Store.BLOCK_MASK) == 0)
        throw stateError(L.l("Block 0 is reserved."));
      */

      block = new ReadBlock(store, blockId);

      if (dirtyBlock != null) {
        byte []dirtyBuffer = dirtyBlock.getBuffer();

        if (dirtyBuffer != null && dirtyBlock.isValid()) {
          System.arraycopy(dirtyBuffer, 0, block.getBuffer(), 0, dirtyBuffer.length);
          block.validate();
        }

        // System.out.println("COPY: " + dirtyBlock + " " + _writeQueue.size());

        dirtyBlock.free();
      }

      // needs to be outside the synchronized because the put
      // can cause an LRU drop which might lead to a dirty write
      Block oldBlock = _blockCache.putIfNew(blockId, block);
      
      if (block != oldBlock) {
        block.free();
      }
      
      block = oldBlock;
    }

    if (blockId != block.getBlockId()
        || (blockId & Store.BLOCK_INDEX_MASK) != store.getId()
        || block.getStore() != store) {
      Thread.dumpStack();
      System.out.println("BLOCK: " + Long.toHexString(blockId) + " " + Long.toHexString(block.getBlockId()) + " " + store + " " + block.getStore());
    }



    return block;
  }

  /**
   * Adds a block that's needs to be flushed.
   */
  void addLruDirtyWriteBlock(Block block)
  {
    synchronized (_writeQueue) {
      if (_writeQueueMax < _writeQueue.size()) {
        try {
          _writeQueue.wait(100);
        } catch (InterruptedException e) {
        }
      }

      _writeQueue.add(block);
    }

    _writer.wake();
  }

  //
  // management/statistics
  //

  /**
   * The managed name is null
   */
  public String getName()
  {
    return null;
  }

  /**
   * The managed type is BlockManager
   */
  public String getType()
  {
    return "BlockManager";
  }

  /**
   * Returns the capacity.
   */
  public long getBlockCapacity()
  {
    return _blockCache.getCapacity();
  }

  /**
   * Returns the hit count.
   */
  public long getHitCountTotal()
  {
    return _blockCache.getHitCount();
  }

  /**
   * Returns the miss count.
   */
  public long getMissCountTotal()
  {
    return _blockCache.getMissCount();
  }

  final void addBlockRead()
  {
    _blockReadCount.incrementAndGet();
  }
  
  /**
   * Returns the read count.
   */
  public long getBlockReadCountTotal()
  {
    return _blockReadCount.get();
  }

  final void addBlockWrite()
  {
    _blockWriteCount.incrementAndGet();
  }

  /**
   * Returns the write count.
   */
  public long getBlockWriteCountTotal()
  {
    return _blockWriteCount.get();
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }

  class BlockManagerWriter extends TaskWorker {
    public void runTask()
    {
      try {
        while (true) {
          Block block = null;

          synchronized (_writeQueue) {
            for (int i = 0; i < _writeQueue.size(); i++) {
              block = _writeQueue.get(i);

              if (block.isFree())
                break;
              else {
                block = null;
              }
            }
          }

          if (block == null)
            return;

          block.close();

          synchronized (_writeQueue) {
            int size = _writeQueue.size();
            boolean isMax = _writeQueueMax <= size;
          
            for (int i = size - 1; i >= 0; i--) {
              if (block == _writeQueue.get(i)) {
                _writeQueue.remove(i);
                break;
              }
            }

            _writeQueue.notifyAll();
          }
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
