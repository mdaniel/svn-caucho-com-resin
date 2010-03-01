/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.lock.Lock;
import com.caucho.util.CacheListener;
import com.caucho.util.FreeList;
import com.caucho.util.SyncCacheListener;

/**
 * Represents a versioned row
 */
public final class Block implements SyncCacheListener {
  private static final Logger log
    = Logger.getLogger(Block.class.getName());
  private static final FreeList<byte[]> _freeBuffers
    = new FreeList<byte[]>(64);

  private final BlockStore _store;
  private final long _blockId;

  private final Lock _lock;

  private final AtomicInteger _useCount = new AtomicInteger(1);

  private final Object _writeLock = new Object();
  private final AtomicBoolean _isWriteQueued = new AtomicBoolean();
  private boolean _isFlushDirtyOnCommit;
  private boolean _isValid;
  
  private byte []_buffer;

  private int _dirtyMin = BlockStore.BLOCK_SIZE;
  private int _dirtyMax;

  Block(BlockStore store, long blockId)
  {
    store.validateBlockId(blockId);

    _store = store;
    _blockId = blockId;

    _lock = new Lock("block:" + store.getName() + ":" + Long.toHexString(_blockId));

    _isFlushDirtyOnCommit = _store.isFlushDirtyBlocksOnCommit();

    _buffer = allocateBuffer();
    
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " create");
  }

  /**
   * Returns true if the block should be flushed on a commit.
   */
  public boolean isFlushDirtyOnCommit()
  {
    return _isFlushDirtyOnCommit;
  }

  /**
   * True if the block should be flushed on a commit.
   */
  public void setFlushDirtyOnCommit(boolean isFlush)
  {
    _isFlushDirtyOnCommit = isFlush;
  }

  public boolean isIndex()
  {
    return getStore().getAllocation(BlockStore.blockIdToIndex(getBlockId())) == BlockStore.ALLOC_INDEX;
  }

  /**
   * Returns the block's table.
   */
  public BlockStore getStore()
  {
    return _store;
  }

  /**
   * Returns the block's id.
   */
  public long getBlockId()
  {
    return _blockId;
  }

  public final Lock getLock()
  {
    return _lock;
  }

  public final boolean isValid()
  {
    return _isValid;
  }

  /**
   * Returns true if the block needs writing
   */
  public boolean isDirty()
  {
    return _dirtyMin < _dirtyMax;
  }

  /**
   * Allocates the block for a query.
   */
  public final boolean allocate()
  {
    int useCount;

    do {
      useCount = _useCount.get();

      if (useCount == 0) {
        // The block might be LRU'd just as we're about to allocated it.
        // in that case, we need to allocate a new block, not reuse
        // the old one.
        return false;
      }
    } while (! _useCount.compareAndSet(useCount, useCount + 1));

    if (getBuffer() == null) {
      _useCount.decrementAndGet();
      Thread.dumpStack();
      log.fine(this + " null buffer " + this + " " + _useCount.get());
      return false;
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " allocate (" + useCount + ")");

    //System.out.println(this + " ALLOCATE " + _useCount);

    if (useCount > 32 && log.isLoggable(Level.FINE)) {
      Thread.dumpStack();
      log.fine("using " + this + " " + useCount + " times");
    }

    return true;
  }

  /**
   * Reads into the block.
   */
  public void read()
    throws IOException
  {
    if (_isValid)
      return;

    synchronized (this) {
      if (! _isValid) {
        if (log.isLoggable(Level.FINEST))
          log.finest("read db-block " + this);

        BlockReadWrite readWrite = _store.getReadWrite();
        
        readWrite.readBlock(_blockId & BlockStore.BLOCK_MASK,
                            getBuffer(), 0, BlockStore.BLOCK_SIZE);
        _isValid = true;

        clearDirty();
      }
    }
  }

  /**
   * Copies the contents to a target block. Used by the BlockWriter
   * for blocks being written
   */
  void copyToBlock(Block block)
  {
    synchronized (this) {
      byte []buffer = _buffer;

      if (buffer != null && isValid()) {
        System.arraycopy(buffer, 0, block.getBuffer(), 0, buffer.length);
        block.validate();
      }
    }
  }

  /**
   * Marks the block's data as invalid.
   */
  public void invalidate()
  {
    synchronized (this) {
      if (_dirtyMin < _dirtyMax)
        throw new IllegalStateException();

      _isValid = false;
      clearDirty();
    }
  }

  /**
   * Marks the data as valid.
   */
  void validate()
  {
    _isValid = true;
  }

  /**
   * Marks the block's data as dirty
   */
  public void setDirty(int min, int max)
  {
    if (BlockStore.BLOCK_SIZE < max)
      Thread.dumpStack();

    synchronized (this) {
      if (! _isValid)
        throw new IllegalStateException(this + " set dirty without valid");

      if (min < _dirtyMin)
        _dirtyMin = min;

      if (_dirtyMax < max)
        _dirtyMax = max;
    }
  }

  /**
   * Callable only by the block itself, and must synchronize the Block.
   */
  private void clearDirty()
  {
    _dirtyMax = 0;
    _dirtyMin = BlockStore.BLOCK_SIZE;
  }

  /**
   * Handle any database writes necessary at commit time.  If
   * isFlushDirtyOnCommit() is true, this will write the data to
   * the backing file.
   */
  public void commit()
    throws IOException
  {
    if (! _isFlushDirtyOnCommit)
      return;
    else
      save();
  }

  /**
   * Forces a write of the data (should be private?)
   */
  public void save()
  {
    if (_dirtyMin < _dirtyMax
        && _isWriteQueued.compareAndSet(false, true)) {
      _useCount.incrementAndGet();
      _store.getWriter().addDirtyBlock(this);
    }
  }

  void writeImpl()
    throws IOException
  {
    _useCount.incrementAndGet();
    
    try {
      int dirtyMin = 0;
      int dirtyMax = 0;

      synchronized (this) {
        _isWriteQueued.set(false);
      
        dirtyMin = _dirtyMin;
        dirtyMax = _dirtyMax;

        clearDirty();
      }
      
      if (dirtyMin < dirtyMax) {
        if (log.isLoggable(Level.FINEST))
          log.finest("write db-block " + this + " [" + dirtyMin + ", " + dirtyMax + "]");
      
        boolean isPriority = false;

        writeImpl(dirtyMin, dirtyMax - dirtyMin, isPriority);
      }
    } finally {
      free();
    }
  }

  /**
   * Write the dirty block.
   */
  private void writeImpl(int offset, int length, boolean isPriority)
    throws IOException
  {
    BlockReadWrite readWrite = _store.getReadWrite();
    
    readWrite.writeBlock((_blockId & BlockStore.BLOCK_MASK) + offset,
                         getBuffer(), offset, length,
                         isPriority);
  }

  public void deallocate()
    throws IOException
  {
    if (_useCount.get() > 2)
      throw new IllegalStateException(this + " deallocate requires a single active thread:" + _useCount.get());

    // the deallocated block is not written back
    _isValid = false;
    clearDirty();

    getStore().freeBlock(getBlockId());
  }

  public void saveAllocation()
    throws IOException
  {
    getStore().saveAllocation();
  }

  /**
   * Return true if this is a free block.
   */
  public final boolean isFree()
  {
    return _useCount.get() == 0;
  }

  /**
   * Frees a block from a query.
   */
  public final void free()
  {
    int useCount = _useCount.decrementAndGet();

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " free (" + useCount + ")");

    if (useCount < 0)
      Thread.dumpStack();

    //System.out.println(this + " FREE " + _useCount);

    if (useCount > 0) {
      return;
    }
    else if (_dirtyMin < _dirtyMax) {
      save();
    }
    else {
      // If the block is clean, just discard it
      
      freeImpl();
    }
  }

  /**
   * Called when the block is removed from the cache.
   */
  @Override
  public final void syncRemoveEvent()
  {
    save();
    free();
  }

  /**
   * Returns the block's buffer.
   */
  public final byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Called when the block is removed from the cache.
   */
  protected void freeImpl()
  {
    //System.out.println(this + " FREE-IMPL");
    synchronized (this) {
      // timing for block reuse. The useCount can be reactivated from
      // the BlockManager writeQueue
      if (isFree()) {
        byte []buffer = _buffer;
        _buffer = null;

        freeBuffer(buffer);
      }
    }
  }

  protected byte []allocateBuffer()
  {
    byte []buffer = _freeBuffers.allocate();

    if (buffer == null) {
      buffer = new byte[BlockStore.BLOCK_SIZE];
    }

    return buffer;
  }

  protected void freeBuffer(byte []buffer)
  {
    if (_dirtyMin < _dirtyMax)
      Thread.dumpStack();

    if (buffer != null)
      _freeBuffers.freeCareful(buffer);
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _store + "," + Long.toHexString(_blockId) + "]");
  }
}
