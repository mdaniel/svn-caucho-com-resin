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

package com.caucho.db.index;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

import com.caucho.db.Database;

import com.caucho.db.store.Block;
import com.caucho.db.store.BlockManager;
import com.caucho.db.store.Store;
import com.caucho.db.store.Transaction;
import com.caucho.db.store.WriteBlock;

/**
 * Structure of the table:
 *
 * <pre>
 * b4 - flags
 * b4 - length
 * b8 - parent
 * b8 - next
 * tuples*
 * </pre>
 * 
 * Structure of a tuple:
 *
 * <pre>
 * b8  - ptr to the actual data
 * key - the tuple's key
 * </pre>
 */
public class BTree {
  private final static L10N L = new L10N(BTree.class);
  private final static Logger log = Log.open(BTree.class);
  
  public final static long FAIL = Long.MIN_VALUE;
  private final static int BLOCK_SIZE = Store.BLOCK_SIZE;
  private final static int PTR_SIZE = 8;

  private final static int FLAGS_OFFSET = 0;
  private final static int LENGTH_OFFSET = FLAGS_OFFSET + 4;
  private final static int PARENT_OFFSET = LENGTH_OFFSET + 4;
  private final static int NEXT_OFFSET = PARENT_OFFSET + PTR_SIZE;
  private final static int HEADER_SIZE = NEXT_OFFSET + PTR_SIZE;

  private final static int LEAF_FLAG = 1;

  private BlockManager _blockManager;
  private Store _store;
  private long _indexRoot;
  private int _keySize;
  private int _tupleSize;
  private int _n;
  
  private KeyCompare _keyCompare;

  private int _blockCount;

  private volatile boolean _isStarted;

  /**
   * Creates a new BTree with the given backing.
   *
   * @param backing the underlying file containing the btree.
   */
  public BTree(Store store, long indexRoot, int keySize, KeyCompare keyCompare)
    throws IOException
  {
    if (keyCompare == null)
      throw new NullPointerException();
    
    _store = store;
    _blockManager = _store.getBlockManager();
    _indexRoot = indexRoot;

    if (BLOCK_SIZE < keySize + HEADER_SIZE)
      throw new IOException(L.l("BTree key size `{0}' is too large.",
                                keySize));

    _keySize = keySize;

    _tupleSize = keySize + 2 * PTR_SIZE - 1;
    _tupleSize -= _tupleSize % PTR_SIZE;

    _n = (BLOCK_SIZE - HEADER_SIZE) / _tupleSize;

    _keyCompare = keyCompare;
  }

  /**
   * Returns the index root.
   */
  public long getIndexRoot()
  {
    return _indexRoot;
  }

  /**
   * Creates and initializes the btree.
   */
  public void create()
    throws IOException
  {
  }

  /**
   * Looks up the block for the given key in the btree, returning
   * BTree.FAIL for a failed lookup.
   */
  public long lookup(byte []keyBuffer,
                     int keyOffset,
                     int keyLength,
		     Transaction xa)
    throws IOException
  {
    long index = _indexRoot;

    while (index != 0) {
      Block block = xa.readBlock(_store, index);
      boolean isLeaf = true;
      
      try {
	byte []buffer = block.getBuffer();

	int flags = getInt(buffer, FLAGS_OFFSET);
	isLeaf = (flags & LEAF_FLAG) == 0;
      
	index = lookupTuple(block.getBlockId(),
			    buffer, keyBuffer, keyOffset, keyLength, isLeaf);
      } finally {
	block.free();
      }

      if (isLeaf || index == FAIL)
	return index;
    }

    return FAIL;
  }

  /**
   * Inserts the new value for the given key.
   */
  public void insert(byte []keyBuffer,
                     int keyOffset,
                     int keyLength,
                     long value,
		     Transaction xa)
    throws SQLException
  {
    try {
      if (value == FAIL)
	throw new IllegalArgumentException();
    
      long lastIndex;
      long index = _indexRoot;
      long parentIndex = 0;
    
      while (index != FAIL) {
	lastIndex = index;
        
	Block block = xa.readBlock(_store, index);

	try {
	  byte []buffer = block.getBuffer();

	  int flags = getInt(buffer, FLAGS_OFFSET);
	
	  boolean isLeaf = (flags & LEAF_FLAG) == 0;

	  int length = getInt(buffer, LENGTH_OFFSET);

	  if (length == _n) {
	    if (index == _indexRoot) {
	      split(index, xa);

	      continue;
	    }
	    else if (parentIndex != 0) {
	      split(parentIndex, index, xa);
	    
	      index = parentIndex;
	      parentIndex = 0;

	      continue;
	    }
	  }
	  
	  if (! isLeaf) {
	    parentIndex = index;
	    index = lookupTuple(block.getBlockId(),
				buffer, keyBuffer, keyOffset, keyLength,
				isLeaf);
	  }
	  else {
	    Block writeBlock;
	    
	    writeBlock = xa.createWriteBlock(block);
	    block = writeBlock;
	    
	    insertLeafBlock(index, writeBlock.getBuffer(),
			    keyBuffer, keyOffset, keyLength,
			    value);

	    return;
	  }
	} finally {
	  block.free();
	}
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Inserts into the next block given the current block and the given key.
   */
  private long insertLeafBlock(long blockIndex,
                               byte []block,
                               byte []keyBuffer,
                               int keyOffset,
                               int keyLength,
                               long value)
    throws IOException
  {
    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int length = getInt(block, LENGTH_OFFSET);

    for (int i = 0; i < length; i++) {
      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
				    block, offset + PTR_SIZE,
				    keyLength);

      if (0 < cmp) {
        offset += tupleSize;
        continue;
      }
      else if (cmp == 0) {
        setPointer(block, offset, value);
        //writeBlock(blockIndex, block);
        
        return 0;
      }
      else if (length < _n) {
        return addKey(blockIndex, block, offset, i, length,
                      keyBuffer, keyOffset, keyLength, value);
      }
      else {
	throw new IllegalStateException("ran out of key space");
      }
    }

    if (length < _n) {
      return addKey(blockIndex, block, offset, length, length,
                    keyBuffer, keyOffset, keyLength, value);
    }

    throw new IllegalStateException();

    // return split(blockIndex, block);
  }

  private long addKey(long blockIndex, byte []block,
                      int offset, int index, int length,
                      byte []keyBuffer, int keyOffset, int keyLength,
                      long value)
    throws IOException
  {
    int tupleSize = _tupleSize;

    if (index < length) {
      System.arraycopy(block, offset,
                       block, offset + tupleSize,
                       (length - index) * tupleSize);
    }
    
    setPointer(block, offset, value);
    setLength(block, length + 1);

    if (log.isLoggable(Level.FINER))
      log.finer("btree insert at " + (blockIndex / Store.BLOCK_SIZE) + ":" + offset + " value:" + (value / Store.BLOCK_SIZE) + ":" + (value % Store.BLOCK_SIZE));

    System.arraycopy(keyBuffer, keyOffset,
		     block, offset + PTR_SIZE,
		     keyLength);
          
    for (int j = tupleSize - PTR_SIZE - keyLength; j >= 0; j--)
      block[offset + tupleSize - j] = 0;

    return -value;
  }

  /**
   * The length in lBuf is assumed to be the length of the buffer.
   */
  private void split(long parentIndex, long index, Transaction xa)
    throws IOException
  {
    log.finer("btree splitting " + (index / BLOCK_SIZE));

    Block parentBlock = null;
    Block leftBlock = null;
    Block rightBlock = null;

    try {
      parentBlock = xa.createWriteBlock(_store, parentIndex);
    
      byte []parentBuffer = parentBlock.getBuffer();
      int parentLength = getInt(parentBuffer, LENGTH_OFFSET);
    
      rightBlock = xa.createWriteBlock(_store, index);

      byte []rightBuffer = rightBlock.getBuffer();
      long rightBlockId = rightBlock.getBlockId();
    
      leftBlock = xa.createWriteBlock(_store.allocate());
      byte []leftBuffer = leftBlock.getBuffer();
      long leftBlockId = leftBlock.getBlockId();

      int length = getInt(rightBuffer, LENGTH_OFFSET);
      int pivot = (length - 1) / 2;

      int pivotOffset = HEADER_SIZE + pivot * _tupleSize;

      System.arraycopy(rightBuffer, HEADER_SIZE,
		       leftBuffer, HEADER_SIZE,
		       pivotOffset + _tupleSize - HEADER_SIZE);

      setInt(leftBuffer, FLAGS_OFFSET, getInt(rightBuffer, FLAGS_OFFSET));
      setLength(leftBuffer, pivot + 1);
      setPointer(leftBuffer, NEXT_OFFSET, rightBlockId);
      setPointer(leftBuffer, PARENT_OFFSET, parentIndex);

      System.arraycopy(rightBuffer, pivotOffset + _tupleSize,
		       rightBuffer, HEADER_SIZE,
		       (length - pivot - 1) * _tupleSize);

      setLength(rightBuffer, length - pivot - 1);

      insertLeafBlock(parentIndex, parentBuffer,
		      leftBuffer, pivotOffset + PTR_SIZE, _keySize,
		      leftBlockId);
    } finally {
      if (parentBlock != null)
	parentBlock.free();
      
      if (leftBlock != null)
	leftBlock.free();
      
      if (rightBlock != null)
	rightBlock.free();
    }
  }

  /**
   * Splits the current leaf into two.  Half of the entries go to the
   * left leaf and half go to the right leaf.
   */
  private long split(long index, Transaction xa)
    throws IOException
  {
    log.finer("btree splitting " + (index / BLOCK_SIZE));

    WriteBlock parentBlock = null;
    WriteBlock leftBlock = null;
    WriteBlock rightBlock = null;

    try {
      parentBlock = xa.createWriteBlock(_store, index);

      byte []parentBuffer = parentBlock.getBuffer();

      int parentFlags = getInt(parentBuffer, FLAGS_OFFSET);

      leftBlock = xa.createWriteBlock(_store.allocate());
      long leftBlockId = leftBlock.getBlockId();
    
      rightBlock = xa.createWriteBlock(_store.allocate());
      long rightBlockId = rightBlock.getBlockId();

      int length = getInt(parentBuffer, LENGTH_OFFSET);

      int pivot = (length - 1) / 2;

      int pivotOffset = HEADER_SIZE + pivot * _tupleSize;
      long pivotValue = getPointer(parentBuffer, pivotOffset);

      byte []leftBuffer = leftBlock.getBuffer();

      System.arraycopy(parentBuffer, HEADER_SIZE,
		       leftBuffer, HEADER_SIZE,
		       pivotOffset + _tupleSize - HEADER_SIZE);

      setInt(leftBuffer, FLAGS_OFFSET, parentFlags);
      setLength(leftBuffer, pivot + 1);
      setPointer(leftBuffer, PARENT_OFFSET, index);
      setPointer(leftBuffer, NEXT_OFFSET, rightBlockId);

      byte []rightBuffer = rightBlock.getBuffer();

      System.arraycopy(parentBuffer, pivotOffset + _tupleSize,
		       rightBuffer, HEADER_SIZE,
		       (length - pivot - 1) * _tupleSize);

      setInt(rightBuffer, FLAGS_OFFSET, parentFlags);
      setLength(rightBuffer, length - pivot - 1);
      setPointer(rightBuffer, PARENT_OFFSET, index);
      setPointer(rightBuffer, NEXT_OFFSET,
		 getPointer(parentBuffer, NEXT_OFFSET));

      System.arraycopy(parentBuffer, pivotOffset,
		       parentBuffer, HEADER_SIZE,
		       _tupleSize);
      setPointer(parentBuffer, HEADER_SIZE, leftBlockId);

      setInt(parentBuffer, FLAGS_OFFSET, LEAF_FLAG);
      setLength(parentBuffer, 1);
      setPointer(parentBuffer, NEXT_OFFSET, rightBlockId);
    
      return index;
    } finally {
      if (parentBlock != null)
	parentBlock.free();
      
      if (leftBlock != null)
	leftBlock.free();
      
      if (rightBlock != null)
	rightBlock.free();
    }
  }

  /**
   * Inserts the new value for the given key.
   */
  public void remove(byte []keyBuffer,
                     int keyOffset,
                     int keyLength,
		     Transaction xa)
    throws SQLException
  {
    try {
      long lastIndex;
      long index = _indexRoot;
    
      while (index != FAIL) {
	Block block = xa.readBlock(_store, index);

	try {
	  byte []buffer = block.getBuffer();

	  int flags = getInt(buffer, FLAGS_OFFSET);
	  boolean isLeaf = (flags & LEAF_FLAG) == 0;

	  if (isLeaf) {
	    if (! xa.isAutoCommit())
	      block = xa.createWriteBlock(block);
	    
	    removeLeafBlock(index, block.getBuffer(),
			    keyBuffer, keyOffset, keyLength);

	    return;
	  }
	  else {
	    index = lookupTuple(block.getBlockId(),
				buffer, keyBuffer, keyOffset, keyLength,
				false);
	  }
	} finally {
	  block.free();
	}
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Looks up the next block given the current block and the given key.
   */
  private long lookupTuple(long blockId,
			   byte []block,
                           byte []keyBuffer,
                           int keyOffset,
                           int keyLength,
			   boolean isLeaf)
    throws IOException
  {
    int length = getInt(block, LENGTH_OFFSET);

    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int end = offset + length * tupleSize;

    while (length > 0) {
      int tail = offset + tupleSize * length;
      int delta = tupleSize * (length / 2);
      int newOffset = offset + delta;

      if (newOffset > 65536) {
	System.out.println("OVERFLOW: " + (blockId / Store.BLOCK_SIZE) + ":" + (blockId % Store.BLOCK_SIZE)  + " LENGTH:" + length + " STU:" + getInt(block, LENGTH_OFFSET) + " DELTA:" + delta);
			   
      }

      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
				    block, PTR_SIZE + newOffset, keyLength);
      
      if (cmp == 0)
        return getPointer(block, newOffset);
      else if (cmp > 0) {
        offset = newOffset + tupleSize;
	length = (tail - offset) / tupleSize;
      }
      else if (cmp < 0) {
	length = length / 2;
      }

      if (length > 0) {
      }
      else if (isLeaf)
	return 0;
      else if (cmp < 0)
	return getPointer(block, newOffset);
      else if (offset == end)
	return getPointer(block, NEXT_OFFSET);
      else
	return getPointer(block, offset);
    }

    if (isLeaf)
      return 0;
    else
      return getPointer(block, NEXT_OFFSET);
  }

  /**
   * Inserts into the next block given the current block and the given key.
   */
  private long removeLeafBlock(long blockIndex,
                               byte []block,
                               byte []keyBuffer,
                               int keyOffset,
                               int keyLength)
    throws IOException
  {
    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;
    int length = getInt(block, LENGTH_OFFSET);

    for (int i = 0; i < length; i++) {
      int cmp = _keyCompare.compare(keyBuffer, keyOffset,
				    block, offset + PTR_SIZE,
				    keyLength);
      
      if (0 < cmp) {
        offset += tupleSize;
        continue;
      }
      else if (cmp == 0) {
	int tupleLength = length * tupleSize;

	if (offset + tupleSize < HEADER_SIZE + tupleLength)
	  System.arraycopy(block, offset + tupleSize, block, offset,
			   HEADER_SIZE + tupleLength - offset - tupleSize);

	setLength(block, length - 1);
        
        return i;
      }
      else {
        return 0;
      }
    }

    return 0;
  }

  /**
   * Compares the key to the block data.
   */
  /*
  private int compare(byte []keyBuffer, int keyOffset,
                      byte []block, int offset, int length)
  {
    for (; length > 0; length--) {
      byte keyByte = keyBuffer[keyOffset++];
      byte blockByte = block[offset++];

      if (keyByte < blockByte)
        return -1;
      else if (blockByte < keyByte)
        return 1;
    }

    return 0;
  }
  */

  /**
   * Reads an int
   */
  private int getInt(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 24) +
            ((buffer[offset + 1] & 0xff) << 16) +
            ((buffer[offset + 2] & 0xff) << 8) +
            ((buffer[offset + 3] & 0xff)));
  }

  /**
   * Reads a pointer.
   */
  private long getPointer(byte []buffer, int offset)
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
   * Sets an int
   */
  private void setInt(byte []buffer, int offset, int value)
  {
    buffer[offset + 0] = (byte) (value >> 24);
    buffer[offset + 1] = (byte) (value >> 16);
    buffer[offset + 2] = (byte) (value >> 8);
    buffer[offset + 3] = (byte) (value);
  }

  /**
   * Sets the length
   */
  private void setLength(byte []buffer, int value)
  {
    if (value < 0 || value > 65536)
      System.out.println("BAD-LENGTH: " + value);

    setInt(buffer, LENGTH_OFFSET, value);
  }

  /**
   * Sets a pointer.
   */
  private void setPointer(byte []buffer, int offset, long value)
  {
    if (offset <= LENGTH_OFFSET)
      System.out.println("BAD_POINTER: " + offset);
    
    buffer[offset + 0] = (byte) (value >> 56);
    buffer[offset + 1] = (byte) (value >> 48);
    buffer[offset + 2] = (byte) (value >> 40);
    buffer[offset + 3] = (byte) (value >> 32);
    buffer[offset + 4] = (byte) (value >> 24);
    buffer[offset + 5] = (byte) (value >> 16);
    buffer[offset + 6] = (byte) (value >> 8);
    buffer[offset + 7] = (byte) (value);
  }

  /**
   * Opens the BTree.
   */
  private synchronized void start()
    throws IOException
  {
    if (_isStarted)
      return;

    _isStarted = true;
  }
  
  /**
   * Testing: returns the keys for a block
   */
  public ArrayList<String> getBlockKeys(long blockIndex)
    throws IOException
  {
    long blockId = _store.addressToBlockId(blockIndex * BLOCK_SIZE);
    Block block = _store.readBlock(blockId);

    block.read();
    byte []buffer = block.getBuffer();
      
    int length = getInt(buffer, LENGTH_OFFSET);
    int offset = HEADER_SIZE;
    int tupleSize = _tupleSize;

    ArrayList<String> keys = new ArrayList<String>();
    for (int i = 0; i < length; i++) {
      CharBuffer cb = new CharBuffer();

      for (int j = PTR_SIZE; j < tupleSize; j++) {
	int ch = buffer[offset + i * tupleSize + j];

	if (ch == 0)
	  break;
	cb.append((char) ch);
      }

      keys.add(cb.toString());
    }

    block.free();
    
    return keys;
  }

  public static BTree createTest(Path path, int keySize)
    throws IOException, java.sql.SQLException
  {
    Database db = new Database();
    db.setPath(path);
    db.init();

    Store store = new Store(db, "test", null);
    store.create();

    Block block = store.allocate();
    long blockId = block.getBlockId();
    block.free();

    return new BTree(store, blockId, keySize, new KeyCompare());
  }

  public String toString()
  {
    return "BTree[" + _store + "," + (_indexRoot / BLOCK_SIZE) + "]";
  }
}
