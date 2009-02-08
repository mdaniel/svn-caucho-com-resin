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

import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.SyncCacheListener;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a versioned row
 */
abstract public class Block implements SyncCacheListener {
  private static final Logger log
    = Logger.getLogger(Block.class.getName());
  private static final L10N L = new L10N(Block.class);

  protected static final FreeList<byte[]> _freeBuffers
    = new FreeList<byte[]>(4);

  private final Store _store;
  private final long _blockId;

  private final Lock _lock;

  private final AtomicInteger _useCount = new AtomicInteger(1);

  private final Object _writeLock = new Object();
  private final AtomicBoolean _isWriting = new AtomicBoolean();
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
    
    _lock = new Lock("block:" + store.getName() + ":" + _blockId);
    
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
  public boolean allocate()
  {
    int useCount = _useCount.incrementAndGet();

    if (getBuffer() == null)
      return false;
      
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

  public Lock getLock()
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
    synchronized (this) {
      if (! _isValid) {
	if (log.isLoggable(Level.FINEST))
	  log.finest("read db-block " + this);
      
	_store.readBlock(_blockId & Store.BLOCK_MASK,
			 getBuffer(), 0, Store.BLOCK_SIZE);
	_isValid = true;

	_dirtyMin = Store.BLOCK_SIZE;
	_dirtyMax = 0;
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
    synchronized (_writeLock) {
      int dirtyMin = 0;
      int dirtyMax = 0;
    
      synchronized (this) {
	dirtyMin = _dirtyMin;
	_dirtyMin = Store.BLOCK_SIZE;

	dirtyMax = _dirtyMax;
	_dirtyMax = 0;

	if (dirtyMax <= dirtyMin) {
	  return;
	}

	// temp alloc for sync, matched by the free() below
	_useCount.incrementAndGet();
	_isValid = true;
      
	if (log.isLoggable(Level.FINEST))
	  log.finest("write alloc " + this + " (" + _useCount + ")");
      }

      try {
	if (log.isLoggable(Level.FINEST))
	  log.finest("write db-block " + this + " [" + dirtyMin + ", " + dirtyMax + "]");

	//System.out.println(this + " WRITE_BEGIN");
	writeImpl(dirtyMin, dirtyMax - dirtyMin);
	//System.out.println(this + " WRITE_END");
      } finally {
	free();
      }
    }
  }

  /**
   * Write the dirty block.
   */
  protected void writeImpl(int offset, int length)
    throws IOException
  {
    _store.writeBlock((_blockId & Store.BLOCK_MASK) + offset,
		      getBuffer(), offset, length);
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
      _dirtyMin = Store.BLOCK_SIZE;
      _dirtyMax = 0;
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
      _isValid = true;

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

  /**
   * Return true if this is a free block.
   */
  public boolean isFree()
  {
    return _useCount.get() == 0;
  }

  /**
   * Frees a block from a query.
   */
  public final void free()
  {
    if (log.isLoggable(Level.FINEST))
      log.finest(this + " free (" + _useCount + ")");

    int useCount = _useCount.decrementAndGet();
      
    //System.out.println(this + " FREE " + _useCount);

    if (useCount > 0)
      return;
      
    // If the block is clean, just discard it
    if (_dirtyMax <= _dirtyMin) {
      freeImpl();

      return;
    }

    // dirty blocks get queued for writing
    BlockManager.getBlockManager().addLruDirtyWriteBlock(this);
  }

  /**
   * Called when the block is removed from the cache.
   */
  public final void syncRemoveEvent()
  {
    free();
  }
  
  /**
   * Called when the block is removed from the cache.
   */
  // called only from BlockManagerWriter.run()
  void close()
  {
    synchronized (this) {
      if (_dirtyMin < _dirtyMax) {
	try {
	  write();
	} catch (Throwable e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }

      if (_useCount.get() <= 0)
	freeImpl();

      if (log.isLoggable(Level.FINEST))
	log.finest("db-block remove " + this);
    }
  }

  /**
   * Frees any resources.
   */
  protected void freeImpl()
  {
  }

  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _store + "," + _blockId / Store.BLOCK_SIZE + "]");
  }
}
