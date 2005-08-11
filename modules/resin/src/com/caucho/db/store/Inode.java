/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.Log;
import com.caucho.util.L10N;

import com.caucho.vfs.OutputStreamWithBuffer;

/**
 * Represents the indexes for a BLOB or CLOB.
 *
 * The inode contains 16 long values
 * <pre>
 *  0) length of the saved file
 *  1-14) direct fragment addresses (to 224k)
 *  15) pointer to the indirect block
 * </pre>
 *
 * The indirect block itself is divided into sections:
 * <pre>
 *  0-4095) fragment addresses (to 64M)
 *  4096-6143) single indirect fragment addresses (to 256G)
 *  6144-7167) double indirect fragment addresses (to 1P)
 *  7168-8191) triple indirect fragment addresses (to 2^63)
 * </pre>
 */
public class Inode {
  private static final L10N L = new L10N(Inode.class);
  private static final Logger log = Log.open(Inode.class);
  
  public static final int INODE_SIZE = 128;
  public static final int INLINE_BLOB_SIZE = 120;
  public static final int INODE_BLOCK_SIZE = Store.FRAGMENT_SIZE;

  // direct addresses are stored in the inode itself (56k of data).
  public static final int DIRECT_BLOCKS = 14;
  // single indirect addresses are stored in the indirect block (13M data)
  public static final int SINGLE_INDIRECT_BLOCKS = 4096;
  // double indirect addresses (2^36 = 64G data)
  public static final int DOUBLE_INDIRECT_BLOCKS = 2048;
  // triple indirect addresses (2^48 = 256T data)
  public static final int TRIPLE_INDIRECT_BLOCKS = 1024;
  // quad indirect addresses (2^61 = 2E data)
  public static final int QUAD_INDIRECT_BLOCKS = 1024;
  
  private static final byte []NULL_BYTES = new byte[INODE_SIZE];
  
  private Store _store;
  private byte []_bytes = new byte[INODE_SIZE];

  public Inode()
  {
  }

  public Inode(Store store)
  {
    _store = store;
  }

  /**
   * Returns the backing store.
   */
  public Store getStore()
  {
    return _store;
  }
  
  /**
   * Returns the buffer.
   */
  public byte []getBuffer()
  {
    return _bytes;
  }

  /**
   * Returns the length.
   */
  public long getLength()
  {
    return readLong(_bytes, 0);
  }

  public void init(Store store, byte []buffer, int offset)
  {
    _store = store;

    System.arraycopy(buffer, offset, _bytes, 0, _bytes.length);
  }

  /**
   * Opens a read stream to the inode.
   */
  public InputStream openInputStream()
  {
    return new BlobInputStream(this);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os)
    throws IOException
  {
    writeToStream(os, 0, Long.MAX_VALUE / 2);
  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToStream(OutputStreamWithBuffer os,
			    long offset, long length)
    throws IOException
  {
    byte []buffer = os.getBuffer();
    int writeLength = buffer.length;
    int writeOffset = os.getBufferOffset();
    
    while (length > 0) {
      int sublen = writeLength - writeOffset;

      if (sublen == 0) {
	buffer = os.nextBuffer(writeOffset);
	writeOffset = os.getBufferOffset();
	sublen = writeLength;
      }

      if (length < sublen)
	sublen = (int) length;

      int len = read(buffer, writeOffset, sublen,
		     offset, _bytes, 0, _store);

      if (len <= 0)
	break;

      writeOffset += len;
      offset += len;
      length -= len;
    }

    os.setBufferOffset(writeOffset);
  }

  /**
   * Reads a buffer.
   */
  static int read(byte []buffer, int bufferOffset, int bufferLength,
		  long offset, byte []inode, int inodeOffset, Store store)
    throws IOException
  {
    int fragOffset;

    long fragAddr;
    
    synchronized (inode) {
      long length = readLong(inode, inodeOffset);
      
      if (length <= offset)
	return -1;

      if (length <= Inode.INLINE_BLOB_SIZE) {
	if (length - offset < bufferLength)
	  bufferLength = (int) (length - offset);

	System.arraycopy(inode, inodeOffset + 8 + (int) offset,
			 buffer, bufferOffset, bufferLength);

	return bufferLength;
      }

      int blockCount = (int) (offset / Inode.INODE_BLOCK_SIZE);

      fragOffset = (int) offset % Inode.INODE_BLOCK_SIZE;

      fragAddr = readFragmentAddr(blockCount, inode, inodeOffset, store);
    }

    int len = store.readFragment(fragAddr, fragOffset,
				 buffer, bufferOffset, bufferLength);
    
    return len;
  }

  /**
   * Opens a byte output stream to the inode.
   */
  public OutputStream openOutputStream()
  {
    return new BlobOutputStream(this);
  }

  /**
   * Closes the output stream.
   */
  void closeOutputStream()
  {
  }

  /**
   * Opens a char reader to the inode.
   */
  public Reader openReader()
  {
    return new ClobReader(this);
  }

  /**
   * Opens a char writer to the inode.
   */
  public Writer openWriter()
  {
    return new ClobWriter(this);
  }

  /**
   * Deletes the inode
   */
  public void remove()
  {
    synchronized (_bytes) {
      long length = readLong(_bytes, 0);

      byte []bytes = _bytes;

      try {
	if (length <= INLINE_BLOB_SIZE || bytes == null)
	  return;

	int fragCount = (int) ((length - 1) / INODE_BLOCK_SIZE);

	for (; fragCount >= 0; fragCount--) {
	  long fragAddr = readFragmentAddr(fragCount, bytes, 0, _store);

	  if ((fragAddr & Store.BLOCK_MASK) == 0) {
	    String msg = _store + ": inode block " + fragCount + " has 0 fragment";
	    throw stateError(msg);
	  }

	  _store.deleteFragment(fragAddr);

	  // remove the indirect blocks
	  if (fragCount == DIRECT_BLOCKS) {
	    long blockId = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);

	    if (blockId != 0) {
	      _store.freeBlock(blockId);
	    }
	  }
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      } finally {
	System.arraycopy(NULL_BYTES, 0, _bytes, 0, NULL_BYTES.length);
      }
    }
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readFragmentAddr(int fragCount,
			       byte []inode, int inodeOffset,
			       Store store)
    throws IOException
  {
    if (fragCount < DIRECT_BLOCKS)
      return readLong(inode, inodeOffset + 8 * (1 + fragCount));
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long blockId = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (blockId == 0)
	throw new IllegalStateException(L.l("null block id"));

      Block block = null;

      try {
	block = store.readBlock(blockId);

	synchronized (block) {
	  int blockOffset = 8 * (fragCount - DIRECT_BLOCKS);

	  return readLong(block.getBuffer(), blockOffset);
	}
      } finally {
	if (block != null)
	  block.free();
      }
    }
    else
      throw new IllegalStateException(L.l("Can't yet support data over 64M"));
  }

  /**
   * Writes the block id into the inode.
   */
  static void writeFragmentAddr(int fragCount, long fragAddr,
				byte []inode, int offset,
				Store store)
    throws IOException
  {
    // XXX: not sure if correct, needs XA?
    if ((fragAddr & Store.BLOCK_MASK) == 0) {
      String msg = store + ": inode block " + fragCount + " writing 0 fragment";
      throw stateError(msg);
    }
    
    if (fragCount < DIRECT_BLOCKS)
      writeLong(inode, offset + (fragCount + 1) * 8, fragAddr);
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long blockId = readLong(inode, offset + (DIRECT_BLOCKS + 1) * 8);

      Block block = null;

      try {
	if (blockId == 0) {
	  block = store.allocate();

	  blockId = block.getBlockId();

	  writeLong(inode, offset + (DIRECT_BLOCKS + 1) * 8, blockId);
	}
	else {
	  block = store.readBlock(blockId);
	}

	synchronized (block.getBuffer()) {
	  int blockOffset = 8 * (fragCount - DIRECT_BLOCKS);

	  writeLong(block.getBuffer(), blockOffset, fragAddr);
	}

	block.setDirty(offset, offset + INODE_SIZE);
      } finally {
	if (block != null)
	  block.free();
      }
    }
    else
      throw new IllegalStateException(L.l("Can't yet support data over 64M"));
  }

  /**
   * Reads the long.
   */
  public static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56) +
	    ((buffer[offset + 1] & 0xffL) << 48) +
	    ((buffer[offset + 2] & 0xffL) << 40) +
	    ((buffer[offset + 3] & 0xffL) << 32) +
	    ((buffer[offset + 4] & 0xffL) << 24) +
	    ((buffer[offset + 5] & 0xffL) << 16) +
	    ((buffer[offset + 6] & 0xffL) << 8) +
	    ((buffer[offset + 7] & 0xffL)));
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

  /**
   * Reads the short.
   */
  private static int readShort(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 8) +
	    ((buffer[offset + 1] & 0xff)));
  }

  /**
   * Writes the short.
   */
  private static void writeShort(byte []buffer, int offset, int v)
  {
    buffer[offset + 0] = (byte) (v >> 8);
    buffer[offset + 1] = (byte) v;
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }
}
