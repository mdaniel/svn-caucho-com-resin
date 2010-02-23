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

package com.caucho.db.store;

import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.SyncCacheListener;
import com.caucho.util.CacheListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a versioned row
 */
abstract public class Block implements SyncCacheListener, CacheListener {
  private static final Logger log
    = Logger.getLogger(Block.class.getName());
  private static final L10N L = new L10N(Block.class);

  private static final FreeList<byte[]> _freeBuffers
    = new FreeList<byte[]>(64);

  private final Store _store;
  private final long _blockId;

  private final Lock _lock;

  private final AtomicInteger _useCount = new AtomicInteger(1);

  private final Object _writeLock = new Object();
  private final AtomicBoolean _isWriting = new AtomicBoolean();
  private final AtomicInteger _writeCount = new AtomicInteger();
  private volatile boolean _isWriteRequired;

  private boolean _isFlushDirtyOnCommit;
  private boolean _isValid;

  private int _dirtyMin = Store.BLOCK_SIZE;
  private int _dirtyMax;

  Block(Store store, long blockId)
  {
    store.validateBlockId(blockId);

    _store = store;
    _blockId = blockId;

    _lock = new Lock("block:" + store.getName() + ":" + Long.toHexString(_blockId));

    _isFlushDirtyOnCommit = _store.isFlushDirtyBlocksOnCommit();

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

  final boolean allocateDirty()
  {
    _useCount.incrementAndGet();

    // System.out.println("DIRTY: " + this + " " + _useCount.get());

    // the dirty block can be allocated only if it's not freed yet
    if (getBuffer() != null) {
      return true;
    }
    else {
      _useCount.decrementAndGet();

      return false;
    }
  }

  /**
   * Returns the block's table.
   */
  Store getStore()
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

  /**
   * Returns the block's buffer.
   */
  abstract public byte []getBuffer();

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

        _store.readBlock(_blockId & Store.BLOCK_MASK,
                         getBuffer(), 0, Store.BLOCK_SIZE);
        _isValid = true;

        clearDirty();
      }
    }
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
      write();
  }

  /**
   * Forces a write of the data (should be private?)
   */
  public void write()
    throws IOException
  {
    // this lock is critical because if the write is missed, the
    // block will not be written, but the synchronized block will
    // pile up threads.

    while (_dirtyMin <= _dirtyMax
           && _writeCount.compareAndSet(0, 1)) {
      try {
        synchronized (_writeLock) {
          writeImpl(false);
        }
      } finally {
        _writeCount.set(0);
      }
    }
  }

  public void saveAllocation()
    throws IOException
  {
    getStore().saveAllocation();
  }

  private void writeImpl(boolean isPriority)
    throws IOException
  {
    int dirtyMin = 0;
    int dirtyMax = 0;

    synchronized (this) {
      dirtyMin = _dirtyMin;
      dirtyMax = _dirtyMax;

      clearDirty();

      if (dirtyMax <= dirtyMin) {
        return;
      }

      // temp alloc for sync, matched by the free() below
      _useCount.incrementAndGet();

      if (log.isLoggable(Level.FINEST))
        log.finest("write alloc " + this + " (" + _useCount + ")");
    }

    try {
      if (log.isLoggable(Level.FINEST))
        log.finest("write db-block " + this + " [" + dirtyMin + ", " + dirtyMax + "]");

      writeImpl(dirtyMin, dirtyMax - dirtyMin, isPriority);
    } finally {
      free();
    }
  }

  /**
   * Write the dirty block.
   */
  protected void writeImpl(int offset, int length, boolean isPriority)
    throws IOException
  {
    _store.writeBlock((_blockId & Store.BLOCK_MASK) + offset,
                      getBuffer(), offset, length,
                      isPriority);
  }

  public final boolean isValid()
  {
    return _isValid;
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
    if (Store.BLOCK_SIZE < max)
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
   * Returns true if the block needs writing
   */
  public boolean isDirty()
  {
    return _dirtyMin < _dirtyMax;
  }

  protected int getDirtyMin()
  {
    return _dirtyMin;
  }

  protected int getDirtyMax()
  {
    return _dirtyMax;
  }

  /**
   * Callable only by the block itself, and must synchronize the Block.
   */
  protected void clearDirty()
  {
    _dirtyMax = 0;
    _dirtyMin = Store.BLOCK_SIZE;
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

  public boolean isIndex()
  {
    return getStore().getAllocation(Store.blockIdToIndex(getBlockId())) == Store.ALLOC_INDEX;
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

    if (useCount > 0)
      return;

    // If the block is clean, just discard it
    if (_dirtyMax <= _dirtyMin) {
      freeImpl();

      return;
    }
    else {
      BlockManager.getBlockManager().wakeWriter();
    }
  }

  /**
   * Called when the block is removed from the cache.
   */
  public final void syncRemoveEvent()
  {
    int useCount = _useCount.decrementAndGet();

    if (useCount > 0 || _dirtyMin < _dirtyMax) {
      // dirty blocks get queued for writing
      BlockManager.getBlockManager().addLruDirtyWriteBlock(this);
    }
    else {
      freeImpl();
    }
  }

  /**
   * Called when the block is removed from the cache.
   */
  public final void removeEvent()
  {
    /*
    if (_useCount.get() > 0)
      System.out.println("REMOVE with live: " + this + " " + _useCount);
    */
  }

  /**
   * Copies the contents to a target block. Used by the BlockManager
   * for LRU'd blocks
   */
  protected boolean copyToBlock(Block block)
  {
    return false;
  }

  /**
   * Called when the block is removed from the cache.
   */
  // called only from BlockManagerWriter.run()
  void closeWrite()
  {
    // System.out.println("CLOSE-WRITE: " + this + " " + _dirtyMin + " " + _dirtyMax);
    if (_useCount.get() != 0)
      throw new IllegalStateException(this + " must be free in closeWait");

    if (_dirtyMin < _dirtyMax) {
      try {
        synchronized (_writeLock) {
          writeImpl(true);
        }
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    freeImpl();

    if (log.isLoggable(Level.FINEST))
      log.finest("db-block remove " + this);
  }

  /**
   * Frees any resources.
   */
  protected void freeImpl()
  {
    if (_dirtyMin < _dirtyMax)
      Thread.dumpStack();
  }

  protected byte []allocateBuffer()
  {
    byte []buffer = _freeBuffers.allocate();

    if (buffer == null) {
      buffer = new byte[Store.BLOCK_SIZE];
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
