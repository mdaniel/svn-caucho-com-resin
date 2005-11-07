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

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.RandomAccessStream;
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
 * Fragments are stored in 1k chunks with a single byte prefix indicating
 * its use.
 *
 * <h3>Transactions</h3>
 *
 * Fragments are not associated with transactions.  The rollback is
 * associated with a transaction.
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
  public final static int ALLOC_INDEX    = 0x04;
  public final static int ALLOC_MASK     = 0x0f;

  // Need to use at least 4 fragment blocks before try recycling
  public final static long FRAGMENT_CLOCK_MIN   = 4 * BLOCK_SIZE;
  
  public final static int FRAGMENT_SIZE = 8 * 1024;
  public final static int FRAGMENT_PER_BLOCK
    = BLOCK_SIZE / (FRAGMENT_SIZE + 1);
  
  public final static int STORE_CREATE_END = 1024;

  public final static int ALLOC_CHUNK_SIZE = 1024;
  
  protected final Database _database;
  protected final BlockManager _blockManager;

  private final String _name;
  
  private int _id;

  private Path _path;

  // If true, dirty blocks are written at the end of the transaction.
  // Otherwise, they are buffered
  private boolean _isFlushDirtyBlocksOnCommit = true;

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

  private final Object _statLock = new Object();
  // number of fragments currently used
  private long _fragmentUseCount;

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
   * If true, dirty blocks are written at commit time.
   */
  public void setFlushDirtyBlocksOnCommit(boolean flushOnCommit)
  {
    _isFlushDirtyBlocksOnCommit = flushOnCommit;
  }

  /**
   * If true, dirty blocks are written at commit time.
   */
  public boolean isFlushDirtyBlocksOnCommit()
  {
    return _isFlushDirtyBlocksOnCommit;
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
   * Returns the current number of fragments used.
   */
  public long getTotalFragmentSize()
  {
    return _fragmentUseCount * FRAGMENT_SIZE;
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
      RandomAccessStream file = wrapper.getFile();
      
      _fileSize = file.getLength();
      _blockCount = ((_fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE);

      int allocCount = (int) (_blockCount / 2) + ALLOC_CHUNK_SIZE;

      allocCount -= allocCount % ALLOC_CHUNK_SIZE;

      _allocationTable = new byte[allocCount];

      for (int i = 0; i < allocCount; i += BLOCK_SIZE) {
	int len = allocCount - i;
	
	if (BLOCK_SIZE < len)
	  len = BLOCK_SIZE;
	
	readBlock(i * 2L * BLOCK_SIZE, _allocationTable, i, len);
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
    return firstBlock(blockId, ALLOC_ROW);
  }

  /**
   * Returns the first block id which contains a fragment.
   *
   * @return the block id of the first row block
   */
  public long firstFragment(long blockId)
    throws IOException
  {
    return firstBlock(blockId, ALLOC_FRAGMENT);
  }

  /**
   * Returns the first block id which contains a row.
   *
   * @return the block id of the first row block
   */
  public long firstBlock(long blockId, int type)
    throws IOException
  {
    if (blockId <= BLOCK_SIZE)
      blockId = BLOCK_SIZE;
    
    long blockIndex = blockId >> BLOCK_BITS;

    synchronized (_allocationLock) {
      for (; blockIndex < _blockCount; blockIndex++) {
	if (getAllocation(blockIndex) == type)
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
   * Writes the data in a block.
   */
  /*
  public void commitBlock(Block block, int min, int max)
    throws IOException
  {
    block.setDirty(min, max);

    block.commit();
  }
  */

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
  public Block allocateBlock()
    throws IOException
  {
    return allocateBlock(ALLOC_USED);
  }

  /**
   * Allocates a new block for a fragment
   *
   * @return the block id of the allocated block.
   */
  private Block allocateFragmentBlock()
    throws IOException
  {
    return allocateBlock(ALLOC_FRAGMENT);
  }

  /**
   * Allocates a new block for an index
   *
   * @return the block id of the allocated block.
   */
  public Block allocateIndexBlock()
    throws IOException
  {
    return allocateBlock(ALLOC_INDEX);
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

      if (2 * _allocationTable.length < end)
	end = 2 * _allocationTable.length;

      for (blockIndex = 0; blockIndex < end; blockIndex++) {
	if (getAllocation(blockIndex) == ALLOC_FREE)
	  break;
      }

      if (2 * _allocationTable.length <= blockIndex) {
	// expand the allocation table
	byte []newTable = new byte[_allocationTable.length + ALLOC_CHUNK_SIZE];
	System.arraycopy(_allocationTable, 0,
			 newTable, 0,
			 _allocationTable.length);
	_allocationTable = newTable;

	// if the allocation table is over 64k, allocate the block for the
	// extension (each allocation block of 64k allocates 16G)
	if (blockIndex % (2 * BLOCK_SIZE) == 0) {
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

    for (int i = BLOCK_SIZE - 1; i >= 0; i--)
      buffer[i] = 0;

    block.setDirty(0, BLOCK_SIZE);

    synchronized (_allocationLock) {
      setAllocation(blockIndex, code);
      saveAllocation();
    }

    return block;
  }

  /**
   * Check that an allocated block is valid.
   */
  protected void validateBlockId(long blockId)
    throws IllegalArgumentException, IllegalStateException
  {
    RuntimeException e = null;
    
    if (isClosed())
      e = new IllegalStateException(L.l("store {0} is closing.", this));
    else if (getId() <= 0)
      e = new IllegalStateException(L.l("invalid store {0}.", this));
    else if (getId() != (blockId & BLOCK_INDEX_MASK)) {
      e = new IllegalArgumentException(L.l("block {0} must match store {1}.",
					     blockId & BLOCK_INDEX_MASK,
					     this));
    }

    if (e != null)
      throw e;
  }

  /**
   * Check that an allocated block is valid.
   */
  protected void assertStoreActive()
    throws IllegalStateException
  {
    RuntimeException e = null;
    
    if (isClosed())
      e = new IllegalStateException(L.l("store {0} is closing.", this));
    else if (getId() <= 0)
      e = new IllegalStateException(L.l("invalid store {0}.", this));

    if (e != null)
      throw e;
  }
  
  /**
   * Frees a block.
   *
   * @return the block id of the allocated block.
   */
  protected void freeBlock(long blockId)
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
    int allocOffset = (int) (blockIndex >> 1);
    int allocBits = 4 * (int) (blockIndex & 0x1);

    return (_allocationTable[allocOffset] >> allocBits) & ALLOC_MASK;
  }

  /**
   * Sets the allocation for a block.
   */
  private void setAllocation(long blockIndex, int code)
  {
    int allocOffset = (int) (blockIndex >> 1);
    int allocBits = 4 * (int) (blockIndex & 0x1);

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
	
      writeBlock(i * 2L * BLOCK_SIZE, _allocationTable, i, len);
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
    if (FRAGMENT_SIZE - fragmentOffset < length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
					     fragmentOffset, length));
    }

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int blockOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();

      synchronized (blockBuffer) {
	System.arraycopy(blockBuffer, blockOffset + 1 + fragmentOffset,
			 buffer, offset, length);
      }

      return length;
    } finally {
      block.free();
    }
  }
  
  /**
   * Reads a long value from a fragment.
   *
   * @return the long value
   */
  public long readFragmentLong(long fragmentAddress, int fragmentOffset)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int blockOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();

      synchronized (blockBuffer) {
	return readLong(blockBuffer, fragmentOffset);
      }
    } finally {
      block.free();
    }
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
      int blockOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();

      if (FRAGMENT_SIZE < 2 * length)
	length = FRAGMENT_SIZE / 2;
      
      synchronized (blockBuffer) {
	for (int i = 0; i < length; i++) {
	  int ch1 = blockBuffer[blockOffset] & 0xff;
	  int ch2 = blockBuffer[blockOffset + 1] & 0xff;

	  buffer[offset + i] = (char) ((ch1 << 8) + ch2);

	  blockOffset += 2;
	}
      }

      return length;
    } finally {
      block.free();
    }
  }
  
  /**
   * Allocates a new fragment.
   *
   * @return the fragment address
   */
  public long allocateFragment()
    throws IOException
  {
    boolean isLoop = false;
    while (true) {
      Block block = null;

      try {
	long freeBlockId = firstFragment(_fragmentClockAddr);
      
	if (freeBlockId >= 0) {
	  block = readBlock(freeBlockId);
	}
	else if (! isLoop &&
		 FRAGMENT_CLOCK_MIN < _fragmentClockTotal &&
		 2 * _fragmentClockUsed < _fragmentClockTotal) {
	  if (log.isLoggable(Level.FINE)) {
	    log.fine(this + " fragment loop total:" +
		     _fragmentClockTotal + " used:" + _fragmentClockUsed);
	  }
	  
	  _fragmentClockAddr = 0;
	  _fragmentClockLastAddr = 0;
	  _fragmentClockUsed = 0;
	  _fragmentClockTotal = 0;
	  isLoop = true;
	  continue;
	}
	else {
	  block = allocateFragmentBlock();
	  
	  _fragmentClockTotal += FRAGMENT_PER_BLOCK * FRAGMENT_SIZE;
	}
      
	_fragmentClockAddr = block.getBlockId();
	_fragmentClockLastAddr = _fragmentClockAddr;

	long fragAddr = allocateFragment(block);

	if (fragAddr != 0)
	  return fragAddr;
      } finally {
	if (block != null)
	  block.free();
      }
	
      // XXX: db/01g3?
      _fragmentClockAddr += BLOCK_SIZE;
	
      long nextBlockId = firstFragment(_fragmentClockAddr);

      if (nextBlockId > 0) {
	updateClockUse(nextBlockId);
      }
    }
  }
  
  
  /**
   * Allocates a new fragment.
   *
   * @return the fragment address
   */
  private long allocateFragment(Block block)
    throws IOException
  {
    byte []buffer = block.getBuffer();

    synchronized (buffer) {
      for (int i = 0; i < FRAGMENT_PER_BLOCK; i++) {
	int offset = i * (1 + FRAGMENT_SIZE);
	    
	if (buffer[offset] == 0) {
	  buffer[offset] = 1;

	  block.setDirty(offset, offset + 1);
	      
	  synchronized (_statLock) {
	    _fragmentUseCount++;
	  }

	  return blockIdToAddress(block.getBlockId()) + i;
	}
      }
    }

    return 0;
  }
  
  /**
   * Updates the clock use count.
   */
  private void updateClockUse(long blockAddr)
    throws IOException
  {
    Block block = readBlock(blockAddr);

    try {
      byte []buffer = block.getBuffer();

      synchronized (buffer) {
	for (int i = 0; i < FRAGMENT_PER_BLOCK; i++) {
	  int offset = i * (1 + FRAGMENT_SIZE);
	    
	  if (buffer[offset] != 0)
	    _fragmentClockUsed += FRAGMENT_SIZE;
	}
      }
    } finally {
      block.free();
    }
  }
  
  /**
   * Writes a fragment.
   *
   * @param fragmentAddress the fragment to write
   * @param fragmentOffset the offset into the fragment
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   *
   * @return the fragment id
   */
  public void writeFragment(long fragmentAddress, int fragmentOffset,
			    byte []buffer, int offset, int length)
    throws IOException
  {
    if (FRAGMENT_SIZE - fragmentOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
					     fragmentOffset, length));
    
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int blockOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();

      blockOffset += 1 + fragmentOffset;

      synchronized (blockBuffer) {
	System.arraycopy(buffer, offset,
			 blockBuffer, blockOffset,
			 length);

	block.setDirty(blockOffset, blockOffset + length);
      }
    } finally {
      block.free();
    }
  }
  
  /**
   * Writes a long value to a fragment.
   *
   * @return the long value
   */
  public void writeFragmentLong(long fragmentAddress, int fragmentOffset,
				long value)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      int blockOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();
      int offset = blockOffset + 1 + fragmentOffset;

      synchronized (blockBuffer) {
	writeLong(blockBuffer, offset, value);

	block.setDirty(offset, offset + 8);
      }
    } finally {
      block.free();
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
      int fragOffset = getFragmentOffset(fragmentAddress);

      byte []blockBuffer = block.getBuffer();

      synchronized (blockBuffer) {
	blockBuffer[fragOffset] = 0;
      }

      block.setDirty(fragOffset, fragOffset + 1);

      block.commit(); // XXX: shouldn't commit here
    } finally {
      block.free();
    }

    synchronized (_statLock) {
      // XXX: issue with transaction and rollback
      _fragmentUseCount--;
    }
  }

  /**
   * Returns the fragment offset for an id.
   */
  private int getFragmentOffset(long fragmentAddress)
  {
    int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

    return (int) ((FRAGMENT_SIZE + 1) * id);
  }

  /**
   * Reads a block into the buffer.
   */
  public void readBlock(long blockId, byte []buffer, int offset, int length)
    throws IOException
  {
    RandomAccessWrapper wrapper = openRowFile();
    RandomAccessStream is = wrapper.getFile();

    long blockAddress = blockId & BLOCK_MASK;

    try {
      int readLen = is.read(blockAddress, buffer, offset, length);

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
    RandomAccessStream os = wrapper.getFile();
    
    try {
      os.write(blockAddress, buffer, offset, length);
      
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
    RandomAccessStream file = null;
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
      file = _path.openRandomAccess();

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
  
  public String toString()
  {
    return "Store[" + _id + "]";
  }

  static class RandomAccessWrapper {
    private RandomAccessStream _file;

    RandomAccessWrapper(RandomAccessStream file)
    {
      _file = file;
    }

    RandomAccessStream getFile()
    {
      return _file;
    }

    void close()
      throws IOException
    {
      RandomAccessStream file = _file;
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
