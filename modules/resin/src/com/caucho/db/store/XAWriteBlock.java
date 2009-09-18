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

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents a write (dirty) block.
 */
public class XAWriteBlock extends WriteBlock {
  private static final L10N L = new L10N(XAWriteBlock.class);

  private byte []_buffer;

  public XAWriteBlock(Block block)
    throws IOException
  {
    super(block);

    _buffer = allocateBuffer();

    block.read();

    System.arraycopy(block.getBuffer(), 0, getBuffer(), 0, Store.BLOCK_SIZE);
  }
  
  /**
   * Returns the block's buffer.
   */
  public byte []getBuffer()
  {
    return _buffer;
  }

  public void writeImpl(int offset, int length)
    throws IOException
  {
    System.arraycopy(getBuffer(), offset, _block.getBuffer(), offset,
		     length);
    
    _block.setDirty(offset, offset + length);
    // _block.write();
  }

  /**
   * Called when the block is removed from the cache.
   */
  protected void freeImpl()
  {
    synchronized (this) {
      byte []buffer = _buffer;
      _buffer = null;
    
      freeBuffer(buffer);
    }
  }

  public String toString()
  {
    return "XAWriteBlock[" + getStore() + "," + getBlockId() / Store.BLOCK_SIZE + "]";
  }
}
