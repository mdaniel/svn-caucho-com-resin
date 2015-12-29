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

package com.caucho.db.blob;

import java.io.IOException;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;

/**
 * Updates indirect indexes for a blob.
 */
public class InodeUpdate
{
  private final BlockStore _store;
  private final byte []_inode;
  private final int _inodeOffset;
  private Block _lastBlock;

  public InodeUpdate(BlockStore store, byte []bytes, int offset)
  {
    _store = store;
    _inode = bytes;
    _inodeOffset = offset;
  }

  /**
   * Returns the backing store.
   */
  public BlockStore getStore()
  {
    return _store;
  }

  /**
   * Returns the buffer.
   */
  public byte []getBuffer()
  {
    return _inode;
  }

  /**
   * Returns the length.
   */
  public long getLength()
  {
    return readLong(_inode, _inodeOffset);
  }

  /**
   * Reads a long value from a block.
   *
   * @return the long value
   */
  long readBlockLong(long blockAddress,
                     int offset)
    throws IOException
  {
    long blockId = _store.addressToBlockId(blockAddress);
    
    Block block = _lastBlock;
    
    if (block == null) {
      block = _store.readBlock(blockAddress);
      _lastBlock = block;
    }
    else if (block.getBlockId() != blockId) {
      block.free();
      block = _store.readBlock(blockAddress);
      _lastBlock = block;
    }
    
    byte []blockBuffer = block.getBuffer();

    return readLong(blockBuffer, offset);
  }
  
  public void close()
  {
    Block block = _lastBlock;
    _lastBlock = null;

    if (block != null) {
      block.free();
    }
  }

  /**
   * Writes a long value to a block
   *
   * @return the long value
   */
  public void writeBlockLong(long blockAddress, 
                             int offset,
                             long value)
    throws IOException
  {
    long blockId = _store.addressToBlockId(blockAddress);
    
    Block block = _lastBlock;
    
    if (block == null) {
      block = _store.readBlock(blockAddress);
      _lastBlock = block;
    }
    else if (block.getBlockId() != blockId) {
      block.free();
      block = _store.readBlock(blockAddress);
      _lastBlock = block;
    }

    byte []blockBuffer = block.getBuffer();

    writeLong(blockBuffer, offset, value);

    block.setDirty(offset, offset + 8);
  }

  /**
   * Reads the long.
   */
  public static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56)
            + ((buffer[offset + 1] & 0xffL) << 48)
            + ((buffer[offset + 2] & 0xffL) << 40)
            + ((buffer[offset + 3] & 0xffL) << 32)
            + ((buffer[offset + 4] & 0xffL) << 24)
            + ((buffer[offset + 5] & 0xffL) << 16)
            + ((buffer[offset + 6] & 0xffL) << 8)
            + ((buffer[offset + 7] & 0xffL)));
  }

  /**
   * Writes the long.
   */
  public static void writeLong(byte []buffer, int offset, long v)
  {
    buffer[offset + 0] = (byte) (v >> 56);
    buffer[offset + 1] = (byte) (v >> 48);
    buffer[offset + 2] = (byte) (v >> 40);
    buffer[offset + 3] = (byte) (v >> 32);

    buffer[offset + 4] = (byte) (v >> 24);
    buffer[offset + 5] = (byte) (v >> 16);
    buffer[offset + 6] = (byte) (v >> 8);
    buffer[offset + 7] = (byte) (v);
  }
}
