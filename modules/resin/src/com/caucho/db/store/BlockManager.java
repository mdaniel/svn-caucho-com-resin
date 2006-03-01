/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.util.LongKeyLruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the block cache
 */
public class BlockManager {
  private static final Logger log = Log.open(BlockManager.class);
  private static final L10N L = new L10N(BlockManager.class);

  private static BlockManager _staticManager;

  private final byte []_storeMask = new byte[8192];
  private final LongKeyLruCache<Block> _blockCache;

  private BlockManager(int capacity)
  {
    _blockCache = new LongKeyLruCache<Block>(capacity);

    // the first store id is not available to allow for tests for zero.
    _storeMask[0] |= 1;
  }

  /**
   * Returns the block manager, ensuring a minimum number of entries.
   */
  public static synchronized BlockManager create(int minEntries)
  {
    if (_staticManager == null)
      _staticManager = new BlockManager(minEntries);

    _staticManager.ensureCapacity(minEntries);

    return _staticManager;
  }

  /**
   * Ensures the cache has a minimum number of blocks.
   *
   * @param minCapacity the minimum capacity in blocks
   */
  public void ensureCapacity(int minCapacity)
  {
    _blockCache.ensureCapacity(minCapacity);
  }

  /**
   * Allocates a store id.
   */
  public synchronized int allocateStoreId()
  {
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

  /**
   * Frees blocks with the given store.
   */
  public void flush(Store store)
  {
    ArrayList<Block> dirtyBlocks = null;

    synchronized (this) {
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
	  if (block.isDirty())
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

    synchronized (this) {
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
  }

  /**
   * Frees a store id.
   */
  public synchronized void freeStoreId(int storeId)
  {
    if (storeId <= 0)
      throw new IllegalArgumentException(String.valueOf(storeId));

    _storeMask[storeId / 8] &= ~(1 << storeId % 8);
  }

  /**
   * Gets the table's block.
   */
  synchronized Block getBlock(Store store, long blockId)
  {
    // XXX: proper handling of the synchronized is tricky because
    // the LRU dirty write might have timing issues
    
    Block block = _blockCache.get(blockId);

    if (block != null && block.allocate())
      return block;
    
    if ((blockId & Store.BLOCK_MASK) == 0)
      throw stateError(L.l("Block 0 is reserved."));

    block = new ReadBlock(store, blockId);
    block.allocate();

    // needs to be outside the synchronized since the put
    // can cause an LRU drop which might lead to a dirty write
    _blockCache.put(blockId, block);

    return block;
  }

  /**
   * Returns the hit count.
   */
  public long getHitCount()
  {
    return _blockCache.getHitCount();
  }

  /**
   * Returns the miss count.
   */
  public long getMissCount()
  {
    return _blockCache.getMissCount();
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }
}
