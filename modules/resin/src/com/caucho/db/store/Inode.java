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
 *  1-14) direct fragment addresses (to 112k)
 *  15) pointer to the indirect block
 * </pre>
 *
 * The indirect block (an 8k fragment) itself is divided into sections:
 * <pre>
 *  0-511) single indirect fragment addresses (4M)
 *  512-639) double indirect fragment addresses (1G, 2^30)
 *  640-767) triple indirect fragment addresses (to 1T, 2^40)
 *  768-895) quad indirect fragment addresses (to 1P, 2^50)
 *  896-1023) penta indirect fragment addresses (to 1X, to 2^60)
 * </pre>
 */
public class Inode {
  private static final L10N L = new L10N(Inode.class);
  private static final Logger log = Log.open(Inode.class);
  
  public static final int INODE_SIZE = 128;
  public static final int INLINE_BLOB_SIZE = 120;
  public static final int INODE_BLOCK_SIZE = Store.FRAGMENT_SIZE;

  public static final int INDIRECT_BLOCKS = INODE_BLOCK_SIZE / 8;

  // direct addresses are stored in the inode itself (112k of data).
  public static final int DIRECT_BLOCKS = 14;
  // single indirect addresses are stored in the indirect block (4M data)
  public static final int SINGLE_INDIRECT_BLOCKS = 512;
  // double indirect addresses (2^30 = 1G data)
  public static final int DOUBLE_INDIRECT_BLOCKS = 128;
  // triple indirect addresses (2^40 = 1T data)
  public static final int TRIPLE_INDIRECT_BLOCKS = 128;
  // quad indirect addresses (2^50 = 2P data)
  public static final int QUAD_INDIRECT_BLOCKS = 128;
  // penta indirect addresses (2^60 = 2X data)
  public static final int PENTA_INDIRECT_BLOCKS = 128;
  
  private static final byte []NULL_BYTES = new byte[INODE_SIZE];

  private Store _store;
  private StoreTransaction _xa;

  private byte []_bytes = new byte[INODE_SIZE];

  public Inode()
  {
  }

  public Inode(Store store, StoreTransaction xa)
  {
    _store = store;
    _xa = xa;
  }

  public Inode(Store store)
  {
    this(store, RawTransaction.create());
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

  public void init(Store store, StoreTransaction xa,
		   byte []buffer, int offset)
  {
    _store = store;
    _xa = xa;

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
	sublen = writeLength - writeOffset;
      }

      if (length < sublen)
	sublen = (int) length;

      int len = read(_bytes, 0, _store,
		     offset,
		     buffer, writeOffset, sublen);

      if (len <= 0)
	break;

      writeOffset += len;
      offset += len;
      length -= len;
    }

    os.setBufferOffset(writeOffset);
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of bytes to read
   *
   * @return the number of bytes read
   */
  static int read(byte []inode, int inodeOffset,
		  Store store,
		  long fileOffset, 
		  byte []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int sublen = bufferLength;
    if (fileLength - fileOffset < sublen)
      sublen = (int) (fileLength - fileOffset);
      
    if (sublen <= 0)
      return -1;

    if (fileLength <= Inode.INLINE_BLOB_SIZE) {
      System.arraycopy(inode, inodeOffset + 8 + (int) fileOffset,
		       buffer, bufferOffset, sublen);

      return sublen;
    }

    long fragAddr = readFragmentAddr(inode, inodeOffset, store, fileOffset);
    int fragOffset = (int) (fileOffset % Inode.INODE_BLOCK_SIZE);

    if (INODE_BLOCK_SIZE - fragOffset < sublen)
      sublen = INODE_BLOCK_SIZE - fragOffset;

    store.readFragment(fragAddr, fragOffset, buffer, bufferOffset, sublen);
    
    return sublen;
  }
  
  /**
   * Updates the buffer.  Called only from the blob classes.
   */
  static void append(byte []inode, int inodeOffset,
		     Store store, StoreTransaction xa,
		     byte []buffer, int offset, int length)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + length;
    
    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_BLOB_SIZE) {
      System.arraycopy(buffer, offset,
		       inode, (int) (inodeOffset + 8 + currentLength),
		       length);
    }
    else {
      // XXX: theoretically deal with case of appending to inline, although
      // the blobs are the only writers and will avoid that case.

      if (currentLength % INODE_BLOCK_SIZE != 0) {
	long fragAddr = readFragmentAddr(inode, inodeOffset,
					 store,
					 currentLength);

	int fragOffset = (int) (currentLength % INODE_BLOCK_SIZE);
	int sublen = length;

	if (INODE_BLOCK_SIZE - fragOffset < sublen)
	  sublen = INODE_BLOCK_SIZE - fragOffset;

	store.writeFragment(xa, fragAddr, fragOffset, buffer, offset, sublen);

	offset += sublen;
	length -= sublen;

	currentLength += sublen;
      }
      
      while (length > 0) {
	int sublen = length;

	if (INODE_BLOCK_SIZE < sublen)
	  sublen = INODE_BLOCK_SIZE;

	long fragAddr = store.allocateFragment(xa);

	writeFragmentAddr(inode, inodeOffset,
			  store, xa,
			  currentLength, fragAddr);

	store.writeFragment(xa, fragAddr, 0, buffer, offset, sublen);	

	offset += sublen;
	length -= sublen;

	currentLength += sublen;
      }
    }
  }

  /**
   * Reads into a buffer.
   *
   * @param inode the inode buffer
   * @param inodeOffset the offset of the inode data in the buffer
   * @param store the owning store
   * @param fileOffset the offset into the file to read
   * @param buffer the buffer receiving the data
   * @param bufferOffset the offset into the receiving buffer
   * @param bufferLength the maximum number of bytes to read
   *
   * @return the number of chars read
   */
  static int read(byte []inode, int inodeOffset, Store store,
		  long fileOffset, 
		  char []buffer, int bufferOffset, int bufferLength)
    throws IOException
  {
    long fileLength = readLong(inode, inodeOffset);

    int sublen = 2 * bufferLength;
    if (fileLength - fileOffset < sublen)
      sublen = (int) (fileLength - fileOffset);
      
    if (sublen <= 0)
      return -1;

    if (fileLength <= Inode.INLINE_BLOB_SIZE) {
      int baseOffset = inodeOffset + 8 + (int) fileOffset;
      
      for (int i = 0; i < sublen; i += 2) {
	char ch = (char) (((inode[baseOffset + i] & 0xff) << 8) +
			  ((inode[baseOffset + i + 1] & 0xff)));

	buffer[bufferOffset + i] = ch;
      }

      return sublen / 2;
    }

    long fragAddr = readFragmentAddr(inode, inodeOffset, store, fileOffset);
    int fragOffset = (int) (fileOffset % Inode.INODE_BLOCK_SIZE);

    if (INODE_BLOCK_SIZE - fragOffset < sublen)
      sublen = INODE_BLOCK_SIZE - fragOffset;

    store.readFragment(fragAddr, fragOffset, buffer, bufferOffset, sublen);
    
    return sublen;
  }

  /**
   * Updates the buffer.  Called only from the clob classes.
   */
  static void append(byte []inode, int inodeOffset,
		     Store store, StoreTransaction xa,
		     char []buffer, int offset, int length)
    throws IOException
  {
    long currentLength = readLong(inode, inodeOffset);
    long newLength = currentLength + length;
    
    writeLong(inode, inodeOffset, newLength);

    if (newLength <= INLINE_BLOB_SIZE) {
      int writeOffset = (int) (inodeOffset + 8 + currentLength);
      
      for (int i = 0; i < length; i++) {
	char ch = buffer[offset + i];

	inode[writeOffset++] = (byte) (ch >> 8);
	inode[writeOffset++] = (byte) (ch);
      }
    }
    else {
      // XXX: theoretically deal with case of appending to inline, although
      // the blobs are the only writers and will avoid that case.

      if (currentLength % INODE_BLOCK_SIZE != 0) {
	long fragAddr = readFragmentAddr(inode, inodeOffset,
					 store,
					 currentLength);

	int fragOffset = (int) (currentLength % INODE_BLOCK_SIZE);
	int sublen = 2 * length;

	if (INODE_BLOCK_SIZE - fragOffset < sublen)
	  sublen = INODE_BLOCK_SIZE - fragOffset;

	store.writeFragment(xa, fragAddr, fragOffset, buffer, offset, sublen);

	offset += sublen / 2;
	length -= sublen / 2;

	currentLength += sublen;
      }
      
      while (length > 0) {
	int sublen = 2 * length;

	if (INODE_BLOCK_SIZE < sublen)
	  sublen = INODE_BLOCK_SIZE;

	long fragAddr = store.allocateFragment(xa);

	writeFragmentAddr(inode, inodeOffset,
			  store, xa,
			  currentLength, fragAddr);

	store.writeFragment(xa, fragAddr, 0, buffer, offset, sublen);	

	offset += sublen / 2;
	length -= sublen / 2;

	currentLength += sublen;
      }
    }
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

	for (; length > 0; length -= INODE_BLOCK_SIZE) {
	  long fragAddr = readFragmentAddr(bytes, 0, _store, length - 1);

	  if ((fragAddr & Store.BLOCK_MASK) == 0) {
	    String msg = _store + ": inode block " + length + " has 0 fragment";
	    throw stateError(msg);
	  }

	  _store.deleteFragment(_xa, fragAddr);

	  int fragCount = (int) ((length - 1) / INODE_BLOCK_SIZE);

	  int dblFragCount = fragCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS;

	  // remove the double indirect blocks
	  if (dblFragCount >= 0 &&
	      dblFragCount % INDIRECT_BLOCKS == 0) {
	    fragAddr = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);
	    
	    int dblIndex = (int) (fragCount / INDIRECT_BLOCKS);

	    fragAddr = _store.readFragmentLong(fragAddr, dblIndex);

	    if (fragAddr != 0)
	      _store.deleteFragment(_xa, fragAddr);
	  }

	  // remove the indirect blocks
	  if (fragCount == DIRECT_BLOCKS) {
	    fragAddr = readLong(bytes, (DIRECT_BLOCKS + 1) * 8);

	    if (fragAddr != 0) {
	      _store.deleteFragment(_xa, fragAddr);
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
   * Clears the inode.
   */
  static void clear(byte []inode, int inodeOffset)
  {
    int end = inodeOffset + INODE_SIZE;

    for (; inodeOffset < end; inodeOffset++)
      inode[inodeOffset] = 0;
  }

  /**
   * Returns the fragment id for the given offset.
   */
  static long readFragmentAddr(byte []inode, int inodeOffset,
			       Store store,
			       long fileOffset)
    throws IOException
  {
    long fragCount = fileOffset / INODE_BLOCK_SIZE;
    
    if (fragCount < DIRECT_BLOCKS)
      return readLong(inode, (int) (inodeOffset + 8 * (1 + fragCount)));
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long indirectAddr;
      indirectAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indirectAddr == 0)
	throw new IllegalStateException(L.l("null block id"));

      int offset = (int) (8 * (fragCount - DIRECT_BLOCKS));

      return store.readFragmentLong(indirectAddr, offset);
    }
    else if (fragCount < (DIRECT_BLOCKS +
			  SINGLE_INDIRECT_BLOCKS +
			  DOUBLE_INDIRECT_BLOCKS * INDIRECT_BLOCKS)) {
      long indirectAddr;
      indirectAddr = readLong(inode, inodeOffset + (DIRECT_BLOCKS + 1) * 8);

      if (indirectAddr == 0)
	throw new IllegalStateException(L.l("null block id"));

      fragCount -= DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS;

      int index = (int) (fragCount / INDIRECT_BLOCKS);
      
      long doubleIndirectAddr = store.readFragmentLong(indirectAddr, index);
					  
      int offset = (int) (8 * (fragCount % INDIRECT_BLOCKS));

      return store.readFragmentLong(doubleIndirectAddr, offset);
    }
    else
      throw new IllegalStateException(L.l("Can't yet support data over 64M"));
  }

  /**
   * Writes the block id into the inode.
   */
  private static void writeFragmentAddr(byte []inode, int offset,
					Store store, StoreTransaction xa,
					long fragLength, long fragAddr)
    throws IOException
  {
    int fragCount = (int) (fragLength / Store.FRAGMENT_SIZE);
    
    // XXX: not sure if correct, needs XA?
    if ((fragAddr & Store.BLOCK_MASK) == 0) {
      String msg = store + ": inode block " + fragCount + " writing 0 fragment";
      throw stateError(msg);
    }

    if (fragCount < DIRECT_BLOCKS) {
      writeLong(inode, offset + (fragCount + 1) * 8, fragAddr);
    }
    else if (fragCount < DIRECT_BLOCKS + SINGLE_INDIRECT_BLOCKS) {
      long indAddr = readLong(inode, offset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
	indAddr = store.allocateFragment(xa);

	writeLong(inode, offset + (DIRECT_BLOCKS + 1) * 8, indAddr);
      }

      int fragOffset = 8 * (fragCount - DIRECT_BLOCKS);
      
      store.writeFragmentLong(xa, indAddr, fragOffset, fragAddr);
    }
    else if (fragCount < (DIRECT_BLOCKS +
			  SINGLE_INDIRECT_BLOCKS +
			  DOUBLE_INDIRECT_BLOCKS * INDIRECT_BLOCKS)) {
      long indAddr = readLong(inode, offset + (DIRECT_BLOCKS + 1) * 8);

      if (indAddr == 0) {
	indAddr = store.allocateFragment(xa);

	writeLong(inode, offset + (DIRECT_BLOCKS + 1) * 8, indAddr);
      }

      int count = fragCount - DIRECT_BLOCKS - SINGLE_INDIRECT_BLOCKS;

      int dblIndCount = count / INDIRECT_BLOCKS;

      long dblIndAddr = store.readFragmentLong(indAddr, dblIndCount * 8);

      if (dblIndAddr == 0) {
	dblIndAddr = store.allocateFragment(xa);

	store.writeFragmentLong(xa, indAddr, dblIndCount * 8, dblIndAddr);
      }

      int fragOffset = 8 * (count % INDIRECT_BLOCKS);
      
      store.writeFragmentLong(xa, dblIndAddr, fragOffset, fragAddr);
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
