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

import java.lang.ref.SoftReference;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;

import com.caucho.sql.SQLExceptionWrapper;

import com.caucho.log.Log;

import com.caucho.lifecycle.Lifecycle;

import com.caucho.db.Database;

/**
 * The store manages the block-based persistent store file.  Each table
 * will have its own store file, table.db.
 *
 * The store is block-based, where each block is 64k.  Block allocation
 * is tracked by a free block, block 0.  Each block is represented as a
 * two-bit value: free, row, or used.  Since 64k stores 256k entries, the
 * free block can handle a 64G database size.  If the database is larger,
 * another free block occurs at block 256k handling another 64G.
 *
 * The blocks are marked as free (00), row (01), used (10) or fragment(11).
 * Row-blocks are table rows, so a table iterator will only look at
 * the row blocks.   Used blocks are for special blocks like the
 * free list.  Fragments are for blobs.
 *
 * Each store has a unique id in the database.  The store id is merged with
 * the block number in the store to create a unique block id.  There are
 * 64k allowed stores (and therefore 64k tables), leaving 64 - 16 = 48 bits
 * for the blocks in a table, i.e. 2 ^ 48 blocks = 256T blocks.
 *
 * block index: the block number in the file.
 *
 * address: the address of a byte within the store, treating the file as a
 * flat file.
 *
 * block id: the unique id of the block in the database.
 *
 * <h3>Blobs and fragments</h3>
 *
 * Fragments are stored continuously in the block.
 *
 * The first two bytes have the length taken up by all block's fragments.
 * The next two bytes have the number of fragments.
 * The next two bytes has the length of the first 8 fragments in the block.
 * The next two bytes has the length of the first fragment.
 *
 * The 2nd fragment only has a single two-byte header.
 *
 * The 8th fragment has the 8-fragment length and its own length.
 */
public class Store {
  private final static Logger log = Log.open(Store.class);
  private final static L10N L = new L10N(Store.class);
  
  public final static int BLOCK_BITS = 16;
  public final static int BLOCK_SIZE = 1 << BLOCK_BITS;
  public final static int BLOCK_INDEX_MASK = BLOCK_SIZE - 1;
  public final static long BLOCK_MASK = ~ BLOCK_INDEX_MASK;
  public final static long BLOCK_OFFSET_MASK = BLOCK_SIZE - 1;

  public final static int ALLOC_FREE     = 0x00;
  public final static int ALLOC_ROW      = 0x01;
  public final static int ALLOC_USED     = 0x02;
  public final static int ALLOC_FRAGMENT = 0x03;
  public final static int ALLOC_MASK     = 0x03;

  public final static int FRAGMENT_MAX_SIZE = BLOCK_SIZE / 2;

  public final static int FRAGMENT_MAX_COUNT = 128;

  // Need to use at least 4 fragment blocks before try recycling
  public final static long FRAGMENT_CLOCK_MIN   = 4 * BLOCK_SIZE;
  
  public final static int STORE_CREATE_END = 1024;

  public final static int ALLOC_CHUNK_SIZE = 1024;
  
  protected final Database _database;
  protected final BlockManager _blockManager;

  private final String _name;
  
  private int _id;

  private Path _path;

  private boolean _isBufferDirtyBlocks;

  private long _fileSize;
  private long _blockCount;

  private Object _allocationLock = new Object();
  private byte []_allocationTable;
  private long _clockAddr;

  // the current address of the fragment clock
  private long _fragmentClockAddr;
  private long _fragmentClockLastAddr;
  // the total bytes viewed of the clock (?)
  private long _fragmentClockTotal;
  // the total used bytes in the clock (s/b free?)
  private long _fragmentClockUsed;

  private SoftReference<RandomAccessWrapper> _cachedRowFile;
  
  private Lock _tableLock;
  private final Lifecycle _lifecycle = new Lifecycle();

  public Store(Database database, String name, Lock tableLock)
  {
    this(database, name, tableLock, database.getPath().lookup(name + ".db"));
  }

  /**
   * Creates a new store.
   *
   * @param database the owning database.
   * @param name the store name
   * @param lock the table lock
   * @param path the path to the files
   */
  public Store(Database database, String name, Lock tableLock, Path path)
  {
    _database = database;
    _blockManager = _database.getBlockManager();
    
    _name = name;
    _path = path;

    _id = _blockManager.allocateStoreId();

    if (tableLock == null)
      tableLock = new Lock(_id);

    _tableLock = tableLock;
  }

  /**
   * Creates an independent store.
   */
  public static Store create(Path path)
    throws IOException, SQLException
  {
    Database db = new Database();
    db.init();

    Store store = new Store(db, "temp", null, path);

    if (path.canRead())
      store.init();
    else
      store.create();

    return store;
  }

  /**
   * If true, dirty blocks can be buffered without writes.
   */
  public void setBufferDirtyBlocks(boolean bufferDirty)
  {
    _isBufferDirtyBlocks = bufferDirty;
  }

  /**
   * Returns the store's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the store's id.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Returns the table's lock.
   */
  public Lock getLock()
  {
    return _tableLock;
  }

  /**
   * Returns the block manager.
   */
  public BlockManager getBlockManager()
  {
    return _blockManager;
  }

  /**
   * Returns the file size.
   */
  public long getFileSize()
  {
    return _fileSize;
  }

  /**
   * Returns the block count.
   */
  public long getBlockCount()
  {
    return _blockCount;
  }

  /**
   * Converts from the block index to the address for database
   * storage.
   */
  static long blockIndexToAddr(long blockIndex)
  {
    return blockIndex << BLOCK_BITS;
  }

  /**
   * Converts from the block index to the unique block id.
   */
  public final long blockIndexToBlockId(long blockIndex)
  {
    return (blockIndex << BLOCK_BITS) + _id;
  }

  /**
   * Converts from the block index to the unique block id.
   */
  public final long addressToBlockId(long address)
  {
    return (address & BLOCK_MASK) + _id;
  }

  /**
   * Converts from the block index to the unique block id.
   */
  public static long blockIdToAddress(long blockId)
  {
    return (blockId & BLOCK_MASK);
  }

  /**
   * Converts from the block index to the unique block id.
   */
  public static long blockIdToAddress(long blockId, int offset)
  {
    return (blockId & BLOCK_MASK) + offset;
  }

  /**
   * Creates the store.
   */
  public void create()
    throws IOException, SQLException
  {
    if (! _lifecycle.toActive())
      return;
    
    log.finer(this + " create");

    _path.getParent().mkdirs();

    if (_path.exists())
      throw new SQLException(L.l("Table `{0}' already exists.  CREATE can not override an existing table.", _name));

    _allocationTable = new byte[ALLOC_CHUNK_SIZE];

    // allocates the allocation table itself
    setAllocation(0, ALLOC_USED);
    // allocates the header information
    setAllocation(1, ALLOC_USED);

    byte []buffer = new byte[BLOCK_SIZE];
    writeBlock(0, buffer, 0, BLOCK_SIZE);
    writeBlock(BLOCK_SIZE, buffer, 0, BLOCK_SIZE);
    
    writeBlock(0, _allocationTable, 0, _allocationTable.length);

    _blockCount = 2;
  }
  
  public void init()
    throws IOException
  {
    if (! _lifecycle.toActive())
      return;
    
    log.finer(this + " init");

    RandomAccessWrapper wrapper = openRowFile();

    try {
      RandomAccessFile file = wrapper.getFile();
      
      _fileSize = file.length();
      _blockCount = ((_fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE);

      int allocCount = (int) (_blockCount / 4) + ALLOC_CHUNK_SIZE;

      allocCount -= allocCount % ALLOC_CHUNK_SIZE;

      _allocationTable = new byte[allocCount];

      for (int i = 0; i < allocCount; i += BLOCK_SIZE) {
	int len = allocCount - i;
	
	if (BLOCK_SIZE < len)
	  len = BLOCK_SIZE;
	
	readBlock(i * 4L * BLOCK_SIZE, _allocationTable, i, len);
      }
    } finally {
      wrapper.close();
    }
  }

  public void remove()
    throws SQLException
  {
    try {
      _path.remove();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /*
  void lockRead(Transaction xa, long addr)
    throws SQLException
  {
    long blockId = addressToBlockId(addr);

    _database.lockRead(xa, blockId);
  }
  */

  /**
   * Returns the first block id which contains a row.
   *
   * @return the block id of the first row block
   */
  public long firstRow(long blockId)
    throws IOException
  {
    if (blockId <= BLOCK_SIZE)
      blockId = BLOCK_SIZE;
    
    long blockIndex = blockId >> BLOCK_BITS;

    synchronized (_allocationLock) {
      for (; blockIndex < _blockCount; blockIndex++) {
	if (getAllocation(blockIndex) == ALLOC_ROW)
	  return blockIndexToBlockId(blockIndex);
      }
    }

    return -1;
  }

  /**
   * Returns the first block id which contains a fragment.
   *
   * @return the block id of the first row block
   */
  public long firstFragment(long blockId)
    throws IOException
  {
    if (blockId <= BLOCK_SIZE)
      blockId = BLOCK_SIZE;
    
    long blockIndex = blockId >> BLOCK_BITS;

    synchronized (_allocationLock) {
      long end = _blockCount;
      
      for (; blockIndex < end; blockIndex++) {
	if (getAllocation(blockIndex) == ALLOC_FRAGMENT)
	  return blockIndexToBlockId(blockIndex);
      }
    }

    return -1;
  }

  /**
   * Returns the matching block.
   */
  public Block readBlock(long blockId)
    throws IOException
  {
    Block block = _blockManager.getBlock(this, blockId);

    try {
      block.read();
    
      return block;
    } catch (IOException e) {
      block.free();

      throw e;
    } catch (RuntimeException e) {
      block.free();

      throw e;
    }
  }

  /**
   * Returns the matching block.
   */
  public void writeBlock(Block block)
    throws IOException
  {
    block.setDirty();
    
    if (! _isBufferDirtyBlocks)
      block.write();
  }

  /**
   * Allocates a new block for a row.
   *
   * @return the block id of the allocated block.
   */
  public Block allocateRow()
    throws IOException
  {
    return allocateBlock(ALLOC_ROW);
  }

  /**
   * Allocates a new block for a non-row.
   *
   * @return the block id of the allocated block.
   */
  public Block allocate()
    throws IOException
  {
    return allocateBlock(ALLOC_USED);
  }

  /**
   * Allocates a new block for a fragment
   *
   * @return the block id of the allocated block.
   */
  public Block allocateFragment()
    throws IOException
  {
    return allocateBlock(ALLOC_FRAGMENT);
  }
  
  /**
   * Allocates a new block.
   *
   * @return the block id of the allocated block.
   */
  private Block allocateBlock(int code)
    throws IOException
  {
    long blockIndex;

    synchronized (_allocationLock) {
      long end = _blockCount;

      if (4 * _allocationTable.length < end)
	end = 4 * _allocationTable.length;
      
      for (blockIndex = 0; blockIndex < end; blockIndex++) {
	if (getAllocation(blockIndex) == ALLOC_FREE)
	  break;
      }

      if (4 * _allocationTable.length <= blockIndex) {
	// expand the allocation table
	byte []newTable = new byte[_allocationTable.length + ALLOC_CHUNK_SIZE];
	System.arraycopy(_allocationTable, 0,
			 newTable, 0,
			 _allocationTable.length);
	_allocationTable = newTable;

	// if the allocation table is over 64k, allocate the block for the
	// extension (each allocation block of 64k allocates 16G)
	if (blockIndex % (4 * BLOCK_SIZE) == 0) {
	  setAllocation(blockIndex, ALLOC_USED);
	  blockIndex++;
	}
      }

      // mark USED before actual code so it's properly initialized
      setAllocation(blockIndex, ALLOC_USED);

      if (log.isLoggable(Level.FINE))
	log.fine(this + " allocating block " + blockIndex);

      if (_blockCount <= blockIndex)
	_blockCount = blockIndex + 1;
    }

    long blockId = blockIndexToBlockId(blockIndex);

    Block block = _blockManager.getBlock(this, blockId);

    byte []buffer = block.getBuffer();

    for (int i = 0; i < BLOCK_SIZE; i++)
      buffer[i] = 0;

    block.setDirty();

    synchronized (_allocationLock) {
      setAllocation(blockIndex, code);
      saveAllocation();
    }

    block.write();

    return block;
  }
  
  /**
   * Frees a block.
   *
   * @return the block id of the allocated block.
   */
  void freeBlock(long blockId)
    throws IOException
  {
    if (blockId == 0)
      return;
    
    synchronized (_allocationLock) {
      setAllocation(blockId >> BLOCK_BITS, ALLOC_FREE);
    }

    saveAllocation();
  }

  /**
   * Sets the allocation for a block.
   */
  public final int getAllocation(long blockIndex)
  {
    int allocOffset = (int) (blockIndex >> 2);
    int allocBits = 2 * (int) (blockIndex & 0x3);

    return (_allocationTable[allocOffset] >> allocBits) & ALLOC_MASK;
  }

  /**
   * Sets the allocation for a block.
   */
  private void setAllocation(long blockIndex, int code)
  {
    int allocOffset = (int) (blockIndex >> 2);
    int allocBits = 2 * (int) (blockIndex & 0x3);

    int mask = ALLOC_MASK << allocBits;

    _allocationTable[allocOffset] =
      (byte) ((_allocationTable[allocOffset] & ~mask) | (code << allocBits));

  }

  /**
   * Sets the allocation for a block.
   */
  private void saveAllocation()
    throws IOException
  {
    int allocCount = _allocationTable.length;
    
    for (int i = 0; i < allocCount; i += BLOCK_SIZE) {
      int len = allocCount - i;
	
      if (BLOCK_SIZE < len)
	len = BLOCK_SIZE;
	
      writeBlock(i * 4L * BLOCK_SIZE, _allocationTable, i, len);
    }
  }
  
  /**
   * Reads a fragment.
   *
   * @return the fragment id
   */
  public int readFragment(long fragmentAddress, int fragmentOffset,
			  byte []buffer, int offset, int length)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

      byte []blockBuffer = block.getBuffer();
      
      synchronized (block) {
	return readFragmentImpl(id, fragmentOffset, blockBuffer,
				buffer, offset, length);
      }
    } finally {
      block.free();
    }
  }

  private int readFragmentImpl(int id, int fragmentOffset,
			       byte []blockBuffer,
			       byte []buffer, int offset, int length)
  {
    int blockOffset = getFragmentOffset(blockBuffer, id);

    int fragLen = readShort(blockBuffer, blockOffset - 2);

    if (fragLen - fragmentOffset < length)
      length = fragLen - fragmentOffset;

    if (length <= 0)
      return -1;

    System.arraycopy(blockBuffer, blockOffset + fragmentOffset,
		     buffer, offset, length);

    return length;
  }
			       
  
  /**
   * Reads a fragment for a clob.
   *
   * @return the fragment id
   */
  public int readFragment(long fragmentAddress, int fragmentOffset,
			  char []buffer, int offset, int length)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

      byte []blockBuffer = block.getBuffer();

      synchronized (block) {
	int blockOffset = getFragmentOffset(blockBuffer, id);

	int fragLen = readShort(blockBuffer, blockOffset - 2);

	if (fragLen - fragmentOffset < 2 * length)
	  length = (fragLen - fragmentOffset) >> 1;

	if (length <= 0)
	  return -1;

	blockOffset += fragmentOffset;
	
	for (int i = 0; i < length; i++) {
	  int ch1 = blockBuffer[blockOffset] & 0xff;
	  int ch2 = blockBuffer[blockOffset + 1] & 0xff;

	  buffer[offset + i] = (char) ((ch1 << 8) + ch2);

	  blockOffset += 2;
	}

	return length;
      }
    } finally {
      block.free();
    }
  }

  /**
   * Returns the fragment offset for an id.
   */
  private int getFragmentOffset(byte []blockBuffer, int id)
  {
    int fragOffset = 6;
    
    for (int i = 0; i < id && fragOffset < blockBuffer.length; i++) {
      if ((i & 0x7) == 0 && (i & ~0x7) != (id & ~0x7)) {
	// skip by 8
	int groupLen = readShort(blockBuffer, fragOffset - 2);

	fragOffset += 2 + groupLen;

	i += 7;
      }
      else {
	// single fragment
	int fragLen = readShort(blockBuffer, fragOffset);

	fragOffset += 2 + fragLen;
      }
    }

    return fragOffset + 2;
  }
  
  /**
   * Writes a fragment.
   *
   * @return the fragment address
   */
  public long writeFragment(byte []buffer, int offset, int length)
    throws IOException
  {
    if (buffer == null)
      throw new NullPointerException();
    if (FRAGMENT_MAX_SIZE < length)
      throw new IllegalArgumentException();
    if (offset < 0 || length <= 0 || buffer.length < offset + length) {
      System.err.println("bad fragment: " + offset + " " + length);
      throw new IllegalArgumentException();
    }

    boolean isLoop = false;
    while (true) {
      Block block = null;

      try {
	long freeBlockId = firstFragment(_fragmentClockAddr);
      
	if (freeBlockId >= 0) {
	  block = readBlock(freeBlockId);

	  if (freeBlockId != _fragmentClockLastAddr) {
	    _fragmentClockTotal += BLOCK_SIZE;
	    _fragmentClockUsed += readShort(block.getBuffer(), 0);
	  }
	}
	else if (! isLoop &&
		 FRAGMENT_CLOCK_MIN < _fragmentClockTotal &&
		 2 * _fragmentClockUsed < _fragmentClockTotal) {
	  _fragmentClockAddr = 0;
	  _fragmentClockLastAddr = 0;
	  _fragmentClockUsed = 0;
	  _fragmentClockTotal = 0;
	  isLoop = true;
	  continue;
	}
	else {
	  block = allocateFragment();
	  _fragmentClockTotal += BLOCK_SIZE;
	}
      
	_fragmentClockAddr = block.getBlockId();
	_fragmentClockLastAddr = _fragmentClockAddr;

	long fragAddr = writeFragmentBlock(block, buffer, offset, length);
	
	if (fragAddr != 0) {
	  writeBlock(block);
	
	  return fragAddr;
	}
	else {
	  // XXX: db/01g3?
	  _fragmentClockAddr += BLOCK_SIZE;
	}
      } finally {
	if (block != null)
	  block.free();
      }
    }
  }

  /**
   * Writes the fragment into the block.
   */
  private long writeFragmentBlock(Block block,
				  byte []buffer, int offset, int length)
  {
    byte []blockBuffer = block.getBuffer();

    synchronized (block) {
      int spaceUsed = readShort(blockBuffer, 0);

      if (spaceUsed == 0) {
	spaceUsed = 4;
	_fragmentClockUsed += spaceUsed;
	writeShort(blockBuffer, 0, spaceUsed);
	// no fragments used
	writeShort(blockBuffer, 2, 0);
      }

      int fragmentCount = readShort(blockBuffer, 2);
      if (FRAGMENT_MAX_COUNT <= fragmentCount)
	return 0;
	
      int spaceFree = blockBuffer.length - spaceUsed - 4;

      if (spaceFree < length)
	return 0;

      int groupOffset = 4;
      int fragmentOffset = 6;
      int i = 0;

      while (fragmentOffset < spaceUsed) {
	int fragLen = readShort(blockBuffer, fragmentOffset);

	if (fragLen == 0)
	  break;

	fragmentOffset += 2 + fragLen;
	i++;
	
	if ((i & 7) == 0) {
	  groupOffset = fragmentOffset;
	  fragmentOffset = groupOffset + 2;
	}
      }

      // if extending to the end of the block
      if (spaceUsed <= fragmentOffset) {
	int groupLength;
      
	if ((i & 7) == 0) {
	  groupLength = 0;

	  spaceUsed += 2;
	  _fragmentClockUsed += 2;
	}
	else {
	  groupLength = readShort(blockBuffer, groupOffset);
	}

	System.arraycopy(buffer, offset,
			 blockBuffer, fragmentOffset + 2,
			 length);

	writeShort(blockBuffer, fragmentOffset, length);
	writeShort(blockBuffer, groupOffset, groupLength + length + 2);
      
	spaceUsed += length + 2;
	_fragmentClockUsed += length + 2;
      }
      else {
	System.arraycopy(blockBuffer, fragmentOffset + 2,
			 blockBuffer, fragmentOffset + 2 + length,
			 spaceUsed - fragmentOffset - 2);

	System.arraycopy(buffer, offset,
			 blockBuffer, fragmentOffset + 2,
			 length);

	writeShort(blockBuffer, fragmentOffset, length);
	
	int groupLength = readShort(blockBuffer, groupOffset);
	writeShort(blockBuffer, groupOffset, groupLength + length);

	spaceUsed += length;
	_fragmentClockUsed += length;
      }
      
      writeShort(blockBuffer, 0, spaceUsed);
      
      // update the fragment count
      writeShort(blockBuffer, 2, fragmentCount + 1);

      return (block.getBlockId() & BLOCK_MASK) + i;
    }
  }
  
  /**
   * Deletes a fragment.
   */
  public void deleteFragment(long fragmentAddress)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

      int i = 0;
      int fragOffset = 6;
      int groupOffset = 4;

      byte []blockBuffer = block.getBuffer();

      synchronized (block) {
	int space = readShort(blockBuffer, 0);
	int fragmentCount = readShort(blockBuffer, 2);

	if (FRAGMENT_MAX_COUNT < fragmentCount) {
	  String msg = ("Corrupted database fragment count: " + fragmentCount +
			" at " + (block.getBlockId() >> 16) + ":" + (block.getBlockId() & 0xffff) +
			" for store " + _name + ":" + _id);
	  throw stateError(msg);
	}
	
	for (; i < id; i++) {
	  if ((i & 0x7) == 0 && (i & ~0x7) != (id & ~0x7)) {
	    // skip 8
	    int groupLen = readShort(blockBuffer, fragOffset - 2);

	    fragOffset += groupLen + 2;
	    groupOffset = fragOffset - 2;

	    i += 7;
	  }
	  else {
	    // single step
	    int fragLen = readShort(blockBuffer, fragOffset);

	    fragOffset += 2 + fragLen;
	  }
	}

	int fragLen = readShort(blockBuffer, fragOffset);

	fragOffset += 2;

	// debug
	if (blockBuffer.length < space) {
	  String msg = ("Space extended: " + space +
			" at " + (block.getBlockId() >> 16) + ":" + (block.getBlockId() & 0xffff) +
			" for store " + _name + ":" + _id);
	  throw stateError(msg);
	}
	else if (space < fragOffset + fragLen) {
	  String msg = ("Past end: offset:" + fragOffset +
			" len:" + fragLen + " space:" + space +
			" at " + (block.getBlockId() >> 16) + ":" + (block.getBlockId() & 0xffff) +
			" for store " + _name + ":" + _id);
	  throw stateError(msg);
	}
	else if (fragLen == 0) {
	  String msg = ("Fragment deleted: offset:" + fragOffset +
			" len:" + fragLen + " space:" + space +
			" at " + (block.getBlockId() >> 16) + ":" + (block.getBlockId() & 0xffff) +
			" for store " + _name + ":" + _id);
	  throw stateError(msg);
	}
	  
	System.arraycopy(blockBuffer, fragOffset + fragLen,
			 blockBuffer, fragOffset,
			 space - fragOffset - fragLen);

	writeShort(blockBuffer, 0, space - fragLen);
	// update fragment count
	writeShort(blockBuffer, 2, fragmentCount - 1);

	int groupLen = readShort(blockBuffer, groupOffset);
	writeShort(blockBuffer, groupOffset, groupLen - fragLen);
	writeShort(blockBuffer, fragOffset - 2, 0);

	if (fragmentAddress < _fragmentClockAddr + BLOCK_SIZE) {
	  _fragmentClockUsed -= fragLen;
	}
      }

      writeBlock(block);
    } finally {
      block.free();
    }
  }

  /**
   * Reads a block into the buffer.
   */
  public void readBlock(long blockId, byte []buffer, int offset, int length)
    throws IOException
  {
    RandomAccessWrapper wrapper = openRowFile();
    RandomAccessFile is = wrapper.getFile();

    long blockAddress = blockId & BLOCK_MASK;

    try {
      is.seek(blockAddress);

      int readLen = is.read(buffer, offset, length);

      if (readLen < 0) {
	for (int i = 0; i < BLOCK_SIZE; i++)
	  buffer[i] = 0;
      }
      
      freeRowFile(wrapper);
      wrapper = null;
    } finally {
      if (wrapper != null)
	wrapper.close();
    }
  }

  /**
   * Saves the buffer to the database.
   */
  public void writeBlock(long blockAddress,
			 byte []buffer, int offset, int length)
    throws IOException
  {
    RandomAccessWrapper wrapper = openRowFile();
    RandomAccessFile os = wrapper.getFile();
    
    try {
      os.seek(blockAddress);
      os.write(buffer, offset, length);
      
      freeRowFile(wrapper);
      wrapper = null;
      
      if (_fileSize < blockAddress + length) {
	_fileSize = blockAddress + length;
      }
      
    } finally {
      if (wrapper != null)
	wrapper.close();
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFile()
    throws IOException
  {
    RandomAccessFile file = null;
    RandomAccessWrapper wrapper = null;
    
    synchronized (this) {
      SoftReference<RandomAccessWrapper> ref = _cachedRowFile;
      _cachedRowFile = null;
      
      if (ref != null) {
	wrapper = ref.get();
      }
    }

    if (wrapper != null)
      file = wrapper.getFile();

    if (file == null) {
      file = new RandomAccessFile(_path.getNativePath(), "rw");

      wrapper = new RandomAccessWrapper(file);
    }

    return wrapper;
  }

  private void freeRowFile(RandomAccessWrapper wrapper)
    throws IOException
  {
    synchronized (this) {
      if (_cachedRowFile == null) {
	_cachedRowFile = new SoftReference<RandomAccessWrapper>(wrapper);
	return;
      }
    }

    wrapper.close();
  }

  /**
   * Writes the short.
   */
  private static void writeShort(byte []buffer, int offset, int v)
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

  /**
   * Flush the store.
   */
  public void flush()
  {
    if (_lifecycle.isActive()) {
      if (_blockManager != null) {
	_blockManager.flush(this);
      }
    }
  }

  /**
   * True if destroyed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isDestroyed();
  }
  
  /**
   * Closes the store.
   */
  public void close()
  {
    if (! _lifecycle.toDestroy())
      return;

    log.finer(this + " closing");

    if (_blockManager != null) {
      _blockManager.freeStore(this);
      _blockManager.freeStoreId(_id);
    }

    long id = _id;
    _id = 0;

    _path = null;
    
    RandomAccessWrapper wrapper = null;
    
    SoftReference<RandomAccessWrapper> ref = _cachedRowFile;
    _cachedRowFile = null;
      
    if (ref != null)
      wrapper = ref.get();

    if (wrapper != null) {
      try {
	wrapper.close();
      } catch (Throwable e) {
      }
    }
  }

  // debugging stuff.
  /**
   * Returns a copy of the allocation table.
   */
  public byte []getAllocationTable()
  {
    byte []table = new byte[_allocationTable.length];

    System.arraycopy(_allocationTable, 0, table, 0, table.length);

    return table;
  }

  private static IllegalStateException stateError(String msg)
  {
    IllegalStateException e = new IllegalStateException(msg);
    e.fillInStackTrace();
    log.log(Level.WARNING, e.toString(), e);
    return e;
  }
  
  public String toString()
  {
    return "Store[" + _id + "]";
  }

  static class RandomAccessWrapper {
    private RandomAccessFile _file;

    RandomAccessWrapper(RandomAccessFile file)
    {
      _file = file;
    }

    RandomAccessFile getFile()
    {
      return _file;
    }

    void close()
      throws IOException
    {
      RandomAccessFile file = _file;
      _file = null;

      if (file != null)
	file.close();
    }

    protected void finalize()
      throws Throwable
    {
      super.finalize();
      
      close();
    }
  }
}
