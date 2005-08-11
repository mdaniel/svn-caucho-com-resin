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

import java.io.IOException;
import java.io.OutputStream;

import java.sql.SQLException;

import com.caucho.vfs.TempBuffer;

import com.caucho.util.CharBuffer;

import com.caucho.db.sql.Expr;

public class BlobOutputStream extends OutputStream {
  private Store _store;

  private Transaction _xa;
  
  private TempBuffer _tempBuffer;
  private byte []_buffer;
  private int _offset;
  private int _bufferEnd;

  private long _length;

  private Inode _inode;

  private byte []_inodeBuffer;
  private int _inodeOffset;

  private int _blockCount;
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  public BlobOutputStream(Transaction xa, Store store,
			  byte []inode, int inodeOffset)
  {
    init(xa, store, inode, inodeOffset);
  }
  
  /**
   * Creates a blob output stream.
   *
   * @param store the output store
   */
  BlobOutputStream(Inode inode)
  {
    init(null, inode.getStore(), inode.getBuffer(), 0);

    _inode = inode;
  }

  /**
   * Initialize the output stream.
   */
  public void init(Transaction xa, Store store, byte []inode, int inodeOffset)
  {
    _xa = xa;
    
    _store = store;

    _length = 0;

    _inodeBuffer = inode;
    _inodeOffset = inodeOffset;

    _blockCount = 0;
    _offset = 0;
    
    _tempBuffer = TempBuffer.allocate();
    _buffer = _tempBuffer.getBuffer();
    _bufferEnd = _buffer.length;
  }

  /**
   * Writes a byte.
   */
  public void write(int v)
    throws IOException
  {
    if (_bufferEnd <= _offset) {
      flushBlock();
    }

    _buffer[_offset++] = (byte) v;
    _length++;
  }

  /**
   * Writes a buffer.
   */
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    while (length > 0) {
      if (_bufferEnd <= _offset) {
	flushBlock();
      }

      int sublen = _bufferEnd - _offset;
      if (length < sublen)
	sublen = length;

      System.arraycopy(buffer, offset, _buffer, _offset, sublen);

      offset += sublen;
      _offset += sublen;

      length -= sublen;
      _length += sublen;
    }
  }

  /**
   * Completes the stream.
   */
  public void close()
    throws IOException
  {
    try {
      if (_tempBuffer == null)
	return;
      
      flushBlock();
      
      writeLong(_inodeBuffer, _inodeOffset, _length);
    } finally {
      Inode inode = _inode;
      _inode = null;
      
      if (inode != null)
	inode.closeOutputStream();

      _inodeBuffer = null;

      TempBuffer tempBuffer = _tempBuffer;
      _tempBuffer = null;

      if (tempBuffer != null)
	TempBuffer.free(tempBuffer);
    }
  }

  /**
   * Updates the buffer.
   */
  private void flushBlock()
    throws IOException
  {
    if (_blockCount == 0 && _length <= Inode.INLINE_BLOB_SIZE) {
      System.arraycopy(_buffer, 0, _inodeBuffer, _inodeOffset + 8, _offset);
    
      Inode.writeLong(_inodeBuffer, _inodeOffset, _length);
    }
    else {
      int length = _offset;
      int offset = 0;

      _offset = 0;

      long currentLength = Inode.readLong(_inodeBuffer, _inodeOffset);

      while (offset < length) {
	int sublen = length - offset;

	if (Inode.INODE_BLOCK_SIZE < sublen)
	  sublen = Inode.INODE_BLOCK_SIZE;

	long fragAddr = _store.writeFragment(_xa, _buffer, offset, sublen);

	writeFragmentAddr(_blockCount++, fragAddr);

	currentLength += sublen;
	Inode.writeLong(_inodeBuffer, _inodeOffset, currentLength);

	offset += sublen;
      }
    }
  }

  /**
   * Writes the block id into the inode.
   */
  private void writeFragmentAddr(int count, long fragAddr)
    throws IOException
  {
    Inode.writeFragmentAddr(count, fragAddr,
			    _inodeBuffer, _inodeOffset, _store);
  }

  /**
   * Writes the long.
   */
  private static void writeLong(byte []buffer, int offset, long v)
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
   * Reads a long.
   */
  private static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56) |
	    ((buffer[offset + 1] & 0xffL) << 48) |
	    ((buffer[offset + 2] & 0xffL) << 40) |
	    ((buffer[offset + 3] & 0xffL) << 32) |
	    
	    ((buffer[offset + 4] & 0xffL) << 24) |
	    ((buffer[offset + 4] & 0xffL) << 16) |
	    ((buffer[offset + 4] & 0xffL) << 8) |
	    ((buffer[offset + 4] & 0xffL)));
  }

  /**
   * Writes the short.
   */
  private static void writeShort(byte []buffer, int offset, short v)
  {
    buffer[offset + 0] = (byte) (v >> 8);
    buffer[offset + 1] = (byte) (v);
  }

  /**
   * Reads a short.
   */
  private static int readShort(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 8) |
	    ((buffer[offset + 1] & 0xff)));
  }
}
