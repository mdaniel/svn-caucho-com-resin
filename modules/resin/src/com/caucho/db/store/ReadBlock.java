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

import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a versioned row
 */
public class ReadBlock extends Block {
  private static final Logger log
    = Logger.getLogger(ReadBlock.class.getName());
  private static final L10N L = new L10N(ReadBlock.class);

  private byte []_buffer;

  ReadBlock(Store store, long blockId)
  {
    super(store, blockId);

    _buffer = allocateBuffer();
  }

  /**
   * Returns the block's buffer.
   */
  public byte []getBuffer()
  {
    return _buffer;
  }

  /**
   * Copies the contents to a target block. Used by the BlockManager
   * for LRU'd blocks
   */
  @Override
  protected boolean copyToBlock(Block block)
  {
    synchronized (this) {
      byte []buffer = _buffer;

      if (buffer == null || ! isValid()) {
        // System.out.println(block + " COPY FROM FAIL " + this);
        return false;
      }

      System.arraycopy(buffer, 0, block.getBuffer(), 0, buffer.length);
      block.validate();
      
      // The new block takes over responsibility for the writing, because
      // it may modify the block before the write completes
      block.setDirty(getDirtyMin(), getDirtyMax());

      clearDirty();
      // System.out.println(block + " COPY FROM " + this);

      return true;
    }
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
}
