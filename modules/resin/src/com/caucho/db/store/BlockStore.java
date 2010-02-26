/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.db.Database;
import com.caucho.db.lock.Lock;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;
import com.caucho.util.FreeList;
import com.caucho.vfs.Path;
import com.caucho.vfs.RandomAccessStream;

import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The store manages the block-based persistent store file.  Each table
 * will have its own store file, table.db.
 *
 * The store is block-based, where each block is 64k.  Block allocation
 * is tracked by a free block, block 0.  Each block is represented as a
 * two-byte value.  The first byte is the allocation code: free, row,
 * or used.  The second byte is a fragment allocation mask.
 *
 * Since 64k stores 32k entries, the allocation block can handle
 * a 2G database size.  If the database is larger, another free block
 * occurs at block 32k handling another 2G.
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
 * Fragments are stored in 8k chunks with a single byte prefix indicating
 * its use.
 */
public class BlockStore {
  private final static Logger log
    = Logger.getLogger(BlockStore.class.getName());
  private final static L10N L = new L10N(BlockStore.class);

  // 8k block size
  public final static int BLOCK_BITS = 13;
  public final static int BLOCK_SIZE = 1 << BLOCK_BITS;
  public final static long BLOCK_INDEX_MASK = BLOCK_SIZE - 1;
  public final static long BLOCK_MASK = ~ BLOCK_INDEX_MASK;
  public final static long BLOCK_OFFSET_MASK = BLOCK_SIZE - 1;

  private final static int ALLOC_BYTES_PER_BLOCK = 2;

  //private final static int ALLOC_CHUNK_SIZE = 1024 * ALLOC_BYTES_PER_BLOCK;
  private final static int ALLOC_CHUNK_SIZE = BLOCK_SIZE;
  private final static int ALLOC_GROUP_COUNT
    = BLOCK_SIZE / ALLOC_BYTES_PER_BLOCK;

  // total size of an allocation group
  private final static long ALLOC_GROUP_SIZE
    = 1L * ALLOC_GROUP_COUNT * BLOCK_SIZE;

  public final static int ALLOC_FREE      = 0x00;
  public final static int ALLOC_ROW       = 0x01;
  public final static int ALLOC_USED      = 0x02;
  public final static int ALLOC_FRAGMENT  = 0x03;
  public final static int ALLOC_INDEX     = 0x04;
  public final static int ALLOC_MINI_FRAG = 0x05;
  public final static int ALLOC_MASK      = 0x0f;

  public final static int FRAGMENT_SIZE = BLOCK_SIZE / 4; // 16 * 1024;
  public final static int FRAGMENT_PER_BLOCK
    = (int) (BLOCK_SIZE / FRAGMENT_SIZE);

  public final static int MINI_FRAG_SIZE = 256;
  public final static int MINI_FRAG_PER_BLOCK
    = (int) ((BLOCK_SIZE - 64) / MINI_FRAG_SIZE);
  public final static int MINI_FRAG_ALLOC_OFFSET
    = MINI_FRAG_PER_BLOCK * MINI_FRAG_SIZE;

  public final static long DATA_START = BLOCK_SIZE;

  public final static int STORE_CREATE_END = 1024;

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

  private final Object _allocationLock = new Object();
  private byte []_allocationTable;

  private long _freeAllocIndex; // index for finding a free allocation
  private int _freeAllocCount;

  private int _freeMiniAllocIndex; // index for finding a free mini
  private int _freeMiniAllocCount;

  private final Object _allocationWriteLock = new Object();
  private final AtomicInteger _allocationWriteCount = new AtomicInteger();
  private int _allocDirtyMin = Integer.MAX_VALUE;
  private int _allocDirtyMax;

  // number of fragments currently used
  private long _fragmentUseCount;

  // number of minifragments currently used
  private long _miniFragmentUseCount;

  private Object _fileLock = new Object();
  private FreeList<SoftReference<RandomAccessWrapper>> _cachedRowFile
    = new FreeList<SoftReference<RandomAccessWrapper>>(4);

  private final Semaphore _rowFileSemaphore = new Semaphore(8);

  private Lock _rowLock;
  private long _blockLockTimeout = 120000;

  private boolean _isCorrupted;

  private final Lifecycle _lifecycle = new Lifecycle();
  
  public BlockStore(Database database, String name, Lock tableLock)
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
  public BlockStore(Database database, String name, Lock rowLock, Path path)
  {
    _database = database;
    _blockManager = _database.getBlockManager();

    _name = name;
    _path = path;

    if (path == null)
      throw new NullPointerException();

    _id = _blockManager.allocateStoreId();

    if (rowLock == null)
      rowLock = new Lock("row-lock:" + _name + ":" + _id);

    _rowLock = rowLock;
  }

  /**
   * Creates an independent store.
   */
  public static BlockStore create(Path path)
    throws IOException, SQLException
  {
    Database db = new Database();
    db.init();

    BlockStore store = new BlockStore(db, "temp", null, path);

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
    return _rowLock;
  }

  /**
   * Returns the block manager.
   */
  public BlockManager getBlockManager()
  {
    return _blockManager;
  }

  public void setCorrupted(boolean isCorrupted)
  {
    _isCorrupted = isCorrupted;
  }

  public boolean isCorrupted()
  {
    return _isCorrupted;
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
  private static long blockIndexToAddr(long blockIndex)
  {
    return blockIndex << BLOCK_BITS;
  }

  /**
   * Converts from the block index to the unique block id.
   */
  private final long blockIndexToBlockId(long blockIndex)
  {
    return (blockIndex << BLOCK_BITS) + _id;
  }

  /**
   * Converts from the block index to the address for database
   * storage.
   */
  public static long blockIdToIndex(long blockId)
  {
    return blockId >> BLOCK_BITS;
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

    if (_path.exists()) {
      throw new SQLException(L.l("CREATE '{0}' for path '{1}' failed, because the file already exists.  CREATE can not override an existing table.",
                                 _name, _path.getNativePath()));
    }

    _allocationTable = new byte[ALLOC_CHUNK_SIZE];

    // allocates the allocation table itself
    setAllocation(0, ALLOC_USED);
    // allocates the header information
    setAllocation(1, ALLOC_USED);

    boolean isPriority = true;

    byte []buffer = new byte[BLOCK_SIZE];
    writeBlock(0, buffer, 0, BLOCK_SIZE, isPriority);
    writeBlock(BLOCK_SIZE, buffer, 0, BLOCK_SIZE, isPriority);

    writeBlock(0, _allocationTable, 0, _allocationTable.length, isPriority);

    _blockCount = 2;
  }

  public void init()
    throws IOException
  {
    if (! _lifecycle.toActive())
      return;

    log.finer(this + " init");

    boolean isPriority = true;

    RandomAccessWrapper wrapper = openRowFile(true);

    try {
      RandomAccessStream file = wrapper.getFile();

      _fileSize = file.getLength();
    } finally {
      closeRowFile(wrapper, isPriority);
    }

    _blockCount = ((_fileSize + BLOCK_SIZE - 1) / BLOCK_SIZE);

    int allocCount = (int) _blockCount;

    allocCount += (ALLOC_GROUP_COUNT - 1);
    allocCount -= allocCount % ALLOC_GROUP_COUNT;

    int allocSize = allocCount * ALLOC_BYTES_PER_BLOCK;

    _allocationTable = new byte[allocSize];

    for (int i = 0; i < allocSize; i += BLOCK_SIZE) {
      int len = allocSize - i;

      long allocGroup = i / BLOCK_SIZE;

      if (BLOCK_SIZE < len)
        len = BLOCK_SIZE;

      /*
      System.out.println("READ: " + Long.toHexString(allocGroup * ALLOC_GROUP_SIZE) + " " + allocGroup * ALLOC_GROUP_SIZE);
      */

      readBlock((long) allocGroup * ALLOC_GROUP_SIZE,
                _allocationTable, i, len);
    }
  }

  public void remove()
    throws SQLException
  {
    try {
      Path path = _path;
      _path = null;

      close();

      if (path != null)
        path.remove();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

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
    long blockCount = _blockCount;

    for (; blockIndex < blockCount; blockIndex++) {
      if (getAllocation(blockIndex) == type)
        return blockIndexToBlockId(blockIndex);
    }

    return -1;
  }

  /**
   * Returns the matching block.
   */
  public final Block readBlock(long blockAddress)
    throws IOException
  {
    long blockId = addressToBlockId(blockAddress);

    Block block = _blockManager.getBlock(this, blockId);

    boolean isValid = false;

    try {
      block.read();

      isValid = true;

      return block;
    } finally {
      if (! isValid)
        block.free();
    }
  }

  /**
   * Returns the matching block.
   */
  public final Block loadBlock(long blockAddress)
    throws IOException
  {
    long blockId = addressToBlockId(blockAddress);

    Block block = _blockManager.getBlock(this, blockId);

    return block;
  }

  /**
   * Allocates a new block for a row.
   *
   * @return the block id of the allocated block.
   */
  public Block allocateRow()
    throws IOException
  {
    boolean isSave = true;
    Block block = allocateBlock(ALLOC_ROW, isSave);

    // System.out.println("ROW: " + Long.toHexString(block.getBlockId()));

    return block;
  }

  /**
   * Return true if the block is a row block.
   */
  public boolean isRowBlock(long blockAddress)
  {
    return getAllocation(blockAddress / BLOCK_SIZE) == ALLOC_ROW;
  }

  /**
   * Allocates a new block for a non-row.
   *
   * @return the block id of the allocated block.
   */
  public Block allocateBlock()
    throws IOException
  {
    boolean isSave = true;
    return allocateBlock(ALLOC_USED, isSave);
  }

  /**
   * Allocates a new block for a fragment
   *
   * @return the block id of the allocated block.
   */
  private Block allocateFragmentBlock()
    throws IOException
  {
    boolean isSave = true;
    return allocateBlock(ALLOC_FRAGMENT, isSave);
  }

  /**
   * Allocates a new block for a mini-fragment
   *
   * @return the block id of the allocated block.
   */
  private Block allocateBlockMiniFragment()
    throws IOException
  {
    boolean isSave = true;
    return allocateBlock(ALLOC_MINI_FRAG, isSave);
  }

  /**
   * Allocates a new block for an index
   *
   * @return the block id of the allocated block.
   */
  public Block allocateIndexBlock()
    throws IOException
  {
    boolean isSave = false;
    return allocateBlock(ALLOC_INDEX, isSave);
  }

  /**
   * Return true if the block is an index block.
   */
  public boolean isIndexBlock(long blockAddress)
  {
    return getAllocation(blockAddress / BLOCK_SIZE) == ALLOC_INDEX;
  }

  /**
   * Allocates a new block.
   *
   * @return the block id of the allocated block.
   */
  private Block allocateBlock(int code, boolean isSave)
    throws IOException
  {
    long blockIndex;

    while ((blockIndex = findFreeBlock()) == 0) {
      if (_freeAllocIndex == _blockCount && _freeAllocCount == 0) {
        extendFile();
      }
    }

    long blockId = blockIndexToBlockId(blockIndex);

    Block block = _blockManager.getBlock(this, blockId);

    byte []buffer = block.getBuffer();

    for (int i = BLOCK_SIZE - 1; i >= 0; i--)
      buffer[i] = 0;

    block.validate();
    block.setDirty(0, BLOCK_SIZE);

    synchronized (_allocationLock) {
      setAllocation(blockIndex, code);
    }

    /* XXX: requires more
    if (isSave)
      saveAllocation();
    */
    saveAllocation();

    return block;
  }

  private long findFreeBlock()
  {
    synchronized (_allocationLock) {
      long end = _blockCount;

      if (_allocationTable.length < ALLOC_BYTES_PER_BLOCK * end)
        end = _allocationTable.length / ALLOC_BYTES_PER_BLOCK;

      for (long blockIndex = _freeAllocIndex; blockIndex < end; blockIndex++) {
        if (getAllocation(blockIndex) == ALLOC_FREE) {
          _freeAllocIndex = blockIndex;
          _freeAllocCount++;

          // mark USED before actual code so it's properly initialized
          setAllocation(blockIndex, ALLOC_USED);

          return blockIndex;
        }
      }

      if (_freeAllocCount > 0) {
        _freeAllocIndex = 0;
        _freeAllocCount = 0;
      }
      else {
        _freeAllocIndex = _blockCount;
      }

      return 0;
    }
  }

  private void extendFile()
  {
    long newBlockCount;

    synchronized (_allocationLock) {
      if (_freeAllocIndex < _blockCount)
        return;

      if (_blockCount < 256)
        newBlockCount = _blockCount + 1;
      else
        newBlockCount = _blockCount + 256;

      while (_allocationTable.length / ALLOC_BYTES_PER_BLOCK
             <= newBlockCount) {
        // expand the allocation table

        byte []newTable = new byte[_allocationTable.length + ALLOC_CHUNK_SIZE];
        System.arraycopy(_allocationTable, 0,
                         newTable, 0,
                         _allocationTable.length);
        _allocationTable = newTable;

        long superBlockMax = _allocationTable.length / ALLOC_BYTES_PER_BLOCK;
        for (long count = 0;
             count < superBlockMax;
             count += ALLOC_GROUP_COUNT) {
          // if the allocation table is over 8k, allocate the block for the
          // extension (each allocation block of 8k allocates 512m)
          setAllocation(count, ALLOC_USED);
          // System.out.println("SET_ALLOC: " + count);

          // avoid collision
          if (newBlockCount == count)
            newBlockCount++;
        }

        _allocDirtyMin = 0;
        _allocDirtyMax = newTable.length;
      }

      if (log.isLoggable(Level.FINER))
        log.finer(this + " extending file " + newBlockCount);

      _blockCount = newBlockCount;
      _freeAllocIndex = 0;

      if (getAllocation(newBlockCount) != ALLOC_FREE)
        System.out.println("BAD_BLOCK: " + newBlockCount);

      setAllocation(newBlockCount, ALLOC_USED);
    }

    long blockId = blockIndexToBlockId(newBlockCount);

    Block block = _blockManager.getBlock(this, blockId);

    byte []buffer = block.getBuffer();

    for (int i = BLOCK_SIZE - 1; i >= 0; i--)
      buffer[i] = 0;

    block.validate();
    block.setDirty(0, BLOCK_SIZE);

    // if extending file, write the contents now
    try {
      block.write();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    block.free();

    synchronized (_allocationLock) {
      setAllocation(newBlockCount, ALLOC_FREE);
    }
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
  public void freeBlock(long blockId)
    throws IOException
  {
    if (blockId == 0)
      return;

    synchronized (_allocationLock) {
      long index = blockIdToIndex(blockId);

      if (getAllocation(index) == ALLOC_FREE) {
        throw new IllegalStateException(L.l("{0} double free of {1}",
                                            this, Long.toHexString(blockId)));
      }

      setAllocation(index, ALLOC_FREE);
    }

    saveAllocation();
  }

  /**
   * Sets the allocation for a block.
   */
  public final int getAllocation(long blockIndex)
  {
    int allocOffset = (int) (ALLOC_BYTES_PER_BLOCK * blockIndex);

    return _allocationTable[allocOffset] & ALLOC_MASK;
  }

  /**
   * Sets the allocation for a block.
   */
  private void setAllocation(long blockIndex, int code)
  {
    int allocOffset = (int) (ALLOC_BYTES_PER_BLOCK * blockIndex);

    for (int i = 1; i < ALLOC_BYTES_PER_BLOCK; i++)
      _allocationTable[allocOffset + i] = 0;

    _allocationTable[allocOffset] = (byte) code;

    setAllocDirty(allocOffset, allocOffset + ALLOC_BYTES_PER_BLOCK);
  }

  /**
   * Sets the dirty range for the allocation table.
   */
  private void setAllocDirty(int min, int max)
  {
    if (min < _allocDirtyMin)
      _allocDirtyMin = min;

    if (_allocDirtyMax < max)
      _allocDirtyMax = max;
  }

  /**
   * Sets the allocation for a block.
   */
  public void saveAllocation()
    throws IOException
  {
    // cache doesn't actually need to write this data
    if (! _isFlushDirtyBlocksOnCommit)
      return;

    if (_allocDirtyMax <= _allocDirtyMin)
      return;

    try {
      // only two threads should try saving at once.  The second thread
      // is necessary if the dirty range is set after the write
      if (_allocationWriteCount.incrementAndGet() < 2) {
        synchronized (_allocationWriteLock) {
          int dirtyMin;
          int dirtyMax;

          synchronized (_allocationLock) {
            dirtyMin = _allocDirtyMin;
            _allocDirtyMin = Integer.MAX_VALUE;

            dirtyMax = _allocDirtyMax;
            _allocDirtyMax = 0;
          }

          saveAllocation(dirtyMin, dirtyMax);
        }
      }
    } finally {
      _allocationWriteCount.decrementAndGet();
    }
  }

  private void saveAllocation(int dirtyMin, int dirtyMax)
    throws IOException
  {
    // Write each dirty block to disk.  The physical blocks are
    // broken up each BLOCK_SIZE / ALLOC_BYTES_PER_BLOCK.
    for (;
         dirtyMin < dirtyMax;
         dirtyMin = (dirtyMin + BLOCK_SIZE) - dirtyMin % BLOCK_SIZE) {
      int allocGroup = dirtyMin / BLOCK_SIZE;

      int offset = dirtyMin % BLOCK_SIZE;
      int length;

      if (dirtyMin / BLOCK_SIZE != dirtyMax / BLOCK_SIZE)
        length = BLOCK_SIZE - offset;
      else
        length = dirtyMax - dirtyMin;

      boolean isPriority = true;
      writeBlock((long) allocGroup * ALLOC_GROUP_SIZE + offset,
                 _allocationTable, dirtyMin, length, isPriority);
    }
  }

  /**
   * Reads a fragment.
   *
   * @param fragmentAddress the address of the fragment
   * @param fragmentOffset the offset inside the fragment to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the number of bytes to read
   *
   * @return the number of bytes read
   */
  public int readFragment(long fragmentAddress, int fragmentOffset,
                          byte []buffer, int offset, int length)
    throws IOException
  {
    if (fragmentAddress <= 0) {
      log.warning(this + " illegal fragment read with fragment-address=0");
      return 0;
    }

    if (FRAGMENT_SIZE - fragmentOffset < length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             fragmentOffset, length));
    }

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        System.arraycopy(blockBuffer, blockOffset + fragmentOffset,
                         buffer, offset, length);

        return length;
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a block to an output stream.
   *
   * @param blockAddress the address of the block
   * @param blockOffset the offset inside the block to start reading
   * @param os the result output stream
   * @param length the number of bytes to read
   *
   * @return the number of bytes read
   */
  public void readBlock(long blockId, int blockOffset,
                           OutputStream os, int length)
    throws IOException
  {
    if (blockId <= 0) {
      log.warning(this + " illegal block read with block-id=0");
      return;
    }

    if (BLOCK_SIZE - blockOffset < length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             blockOffset, length));
    }

    Block block = readBlock(blockId);

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        os.write(blockBuffer, blockOffset, length);
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a fragment for a clob.
   *
   * @param fragmentAddress the address of the fragment
   * @param fragmentOffset the offset inside the fragment to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the length of the fragment in characters
   *
   * @return the number of characters read
   */
  public int readFragment(long fragmentAddress, int fragmentOffset,
                          char []buffer, int offset, int length)
    throws IOException
  {
    if (FRAGMENT_SIZE - fragmentOffset < 2 * length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             fragmentOffset, length));
    }

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getFragmentOffset(fragmentAddress);
        blockOffset += fragmentOffset;

        byte []blockBuffer = block.getBuffer();

        for (int i = 0; i < length; i++) {
          int ch1 = blockBuffer[blockOffset] & 0xff;
          int ch2 = blockBuffer[blockOffset + 1] & 0xff;

          buffer[offset + i] = (char) ((ch1 << 8) + ch2);

          blockOffset += 2;
        }
      } finally {
        lock.unlockRead();
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
  public long readFragmentLong(long fragmentAddress,
                               int fragmentOffset)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        return readLong(blockBuffer, blockOffset + fragmentOffset);
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a block.
   *
   * @param blockAddress the address of the block
   * @param blockOffset the offset inside the block to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the number of bytes to read
   *
   * @return the number of bytes read
   */
  public int readBlock(long blockAddress, int blockOffset,
                       byte []buffer, int offset, int length)
    throws IOException
  {
    if (BLOCK_SIZE - blockOffset < length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             blockOffset, length));
    }

    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        System.arraycopy(blockBuffer, blockOffset,
                         buffer, offset, length);

        return length;
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a block for a clob.
   *
   * @param blockAddress the address of the block
   * @param blockOffset the offset inside the block to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the length of the block in characters
   *
   * @return the number of characters read
   */
  public int readBlock(long blockAddress, int blockOffset,
                       char []buffer, int offset, int length)
    throws IOException
  {
    if (BLOCK_SIZE - blockOffset < 2 * length) {
      // server/13df
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             blockOffset, length));
    }

    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        for (int i = 0; i < length; i++) {
          int ch1 = blockBuffer[blockOffset] & 0xff;
          int ch2 = blockBuffer[blockOffset + 1] & 0xff;

          buffer[offset + i] = (char) ((ch1 << 8) + ch2);

          blockOffset += 2;
        }

        return length;
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a long value from a block.
   *
   * @return the long value
   */
  public long readBlockLong(long blockAddress,
                            int offset)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        return readLong(blockBuffer, offset);
      } finally {
        lock.unlockRead();
      }
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
    while (true) {
      synchronized (_allocationLock) {
        byte []allocationTable = _allocationTable;

        for (int i = 0;
             i < allocationTable.length;
             i += ALLOC_BYTES_PER_BLOCK) {
          int fragMask = allocationTable[i + 1] & 0xff;

          if (allocationTable[i] == ALLOC_FRAGMENT && fragMask != 0xff) {
            for (int j = 0; j < FRAGMENT_PER_BLOCK; j++) {
              if ((fragMask & (1 << j)) == 0) {
                allocationTable[i + 1] = (byte) (fragMask | (1 << j));

                setAllocDirty(i + 1, i + 2);

                _fragmentUseCount++;

                long fragmentAddress
                  = BLOCK_SIZE * ((long) i / ALLOC_BYTES_PER_BLOCK) + j;

                //System.out.println((fragmentAddress / BLOCK_SIZE) + ":" + j + " ALLOCATE");
                return fragmentAddress;
              }
            }
          }
        }
      }

      // if no fragment, allocate a new one.

      Block block = allocateFragmentBlock();
      block.free();
    }
  }

  /**
   * Deletes a fragment.
   */
  public void deleteFragment(long fragmentAddress)
    throws IOException
  {
    synchronized (_allocationLock) {
      int i = (int) (ALLOC_BYTES_PER_BLOCK * (fragmentAddress / BLOCK_SIZE));
      int j = (int) (fragmentAddress & 0xff);

      int fragMask = _allocationTable[i + 1] & 0xff;
      //System.out.println((fragmentAddress / BLOCK_SIZE) + ":" + j + " DELETE");

      if (_allocationTable[i] != ALLOC_FRAGMENT)
        System.out.println("BAD ENTRY: " + fragMask);

      if (j >= 8)
        System.out.println("BAD J: " + fragMask);

      if ((fragMask & (1 << j)) == 0) {
        log.fine("BAD J-MASK: " + fragMask + " " + j);
      }

      _allocationTable[i + 1] = (byte) (fragMask & ~(1 << j));

      _fragmentUseCount--;

      setAllocDirty(i + 1, i + 2);
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
  public Block writeFragment(long fragmentAddress, int fragmentOffset,
                             byte []buffer, int offset, int length)
    throws IOException
  {
    if (FRAGMENT_SIZE - fragmentOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             fragmentOffset, length));

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        int blockOffset = getFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        blockOffset += fragmentOffset;

        System.arraycopy(buffer, offset,
                         blockBuffer, blockOffset,
                         length);

        block.setDirty(blockOffset, blockOffset + length);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a character based
   *
   * @param fragmentAddress the fragment to write
   * @param fragmentOffset the offset into the fragment
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   */
  public Block writeFragment(long fragmentAddress, int fragmentOffset,
                             char []buffer, int offset, int length)
    throws IOException
  {
    if (FRAGMENT_SIZE - fragmentOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             fragmentOffset, length));

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        int blockOffset = getFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        blockOffset += fragmentOffset;

        int blockTail = blockOffset;

        for (int i = 0; i < length; i++) {
          char ch = buffer[offset + i];

          blockBuffer[blockTail] = (byte) (ch >> 8);
          blockBuffer[blockTail + 1] = (byte) (ch);

          blockTail += 2;
        }

        block.setDirty(blockOffset, blockTail);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a blockfragment.
   *
   * @param blockAddress the block to write
   * @param blockOffset the offset into the block
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   *
   * @return the fragment id
   */
  public Block writeBlock(long blockAddress, int blockOffset,
                          byte []buffer, int offset, int length)
    throws IOException
  {
    if (BLOCK_SIZE - blockOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             blockOffset, length));

    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        System.arraycopy(buffer, offset,
                         blockBuffer, blockOffset,
                         length);

        block.setDirty(blockOffset, blockOffset + length);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a character based block
   *
   * @param blockAddress the fragment to write
   * @param blockOffset the offset into the fragment
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   */
  public Block writeBlock(long blockAddress, int blockOffset,
                          char []buffer, int offset, int charLength)
    throws IOException
  {
    int length = 2 * charLength;

    if (BLOCK_SIZE - blockOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             blockOffset, length));

    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        int blockTail = blockOffset;

        for (int i = 0; i < charLength; i++) {
          char ch = buffer[offset + i];

          blockBuffer[blockTail] = (byte) (ch >> 8);
          blockBuffer[blockTail + 1] = (byte) (ch);

          blockTail += 2;
        }

        block.setDirty(blockOffset, blockTail);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a long value to a block
   *
   * @return the long value
   */
  public Block writeBlockLong(long blockAddress, int offset,
                              long value)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(blockAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        byte []blockBuffer = block.getBuffer();

        writeLong(blockBuffer, offset, value);

        block.setDirty(offset, offset + 8);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Returns the fragment offset for an id.
   */
  private int getFragmentOffset(long fragmentAddress)
  {
    int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

    return (int) (FRAGMENT_SIZE * id);
  }

  /**
   * Reads a fragment.
   *
   * @param fragmentAddress the address of the fragment
   * @param fragmentOffset the offset inside the fragment to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the number of bytes to read
   *
   * @return the number of bytes read
   */
  public int readMiniFragment(long fragmentAddress, int fragmentOffset,
                              byte []buffer, int offset, int length)
    throws IOException
  {
    if (MINI_FRAG_SIZE - fragmentOffset < length) {
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             fragmentOffset, length));
    }

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getMiniFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        System.arraycopy(blockBuffer, blockOffset + fragmentOffset,
                         buffer, offset, length);

        return length;
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a miniFragment for a clob.
   *
   * @param fragmentAddress the address of the fragment
   * @param fragmentOffset the offset inside the fragment to start reading
   * @param buffer the result buffer
   * @param offset offset into the result buffer
   * @param length the length of the fragment in characters
   *
   * @return the number of characters read
   */
  public int readMiniFragment(long fragmentAddress, int fragmentOffset,
                              char []buffer, int offset, int length)
    throws IOException
  {
    if (MINI_FRAG_SIZE - fragmentOffset < 2 * length) {
      throw new IllegalArgumentException(L.l("read offset {0} length {1} too long",
                                             fragmentOffset, length));
    }

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getMiniFragmentOffset(fragmentAddress);
        blockOffset += fragmentOffset;

        byte []blockBuffer = block.getBuffer();

        for (int i = 0; i < length; i++) {
          int ch1 = blockBuffer[blockOffset] & 0xff;
          int ch2 = blockBuffer[blockOffset + 1] & 0xff;

          buffer[offset + i] = (char) ((ch1 << 8) + ch2);

          blockOffset += 2;
        }

        return length;
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Reads a long value from a miniFragment.
   *
   * @return the long value
   */
  public long readMiniFragmentLong(long fragmentAddress,
                                   int fragmentOffset)
    throws IOException
  {
    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockRead(_blockLockTimeout);

      try {
        int blockOffset = getMiniFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        return readLong(blockBuffer, blockOffset + fragmentOffset);
      } finally {
        lock.unlockRead();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Allocates a new miniFragment.
   *
   * @return the fragment address
   */
  public long allocateMiniFragment()
    throws IOException
  {
    while (true) {
      long blockAddr = allocateMiniFragmentBlock();

      Block block = readBlock(blockAddr);
      int fragOffset = -1;

      try {
        byte []blockBuffer = block.getBuffer();
        int freeOffset = -1;

        Lock lock = block.getLock();
        lock.lockReadAndWrite(_blockLockTimeout);

        try {
          for (int i = 0; i < MINI_FRAG_PER_BLOCK; i++) {
            int offset = i / 8 + MINI_FRAG_ALLOC_OFFSET;
            int mask = 1 << (i % 8);

            if ((blockBuffer[offset] & mask) == 0) {
              fragOffset = i;
              blockBuffer[offset] |= mask;
              block.setDirty(offset, offset + 1);
              break;
            }
          }

          // fragment allocated underneath us
          if (fragOffset < 0)
            continue;

          for (int i = 0; i < MINI_FRAG_PER_BLOCK; i++) {
            int offset = i / 8 + MINI_FRAG_ALLOC_OFFSET;
            int mask = 1 << (i % 8);

            if ((blockBuffer[offset] & mask) == 0) {
              freeOffset =
                (int) (ALLOC_BYTES_PER_BLOCK * (blockAddr / BLOCK_SIZE));
              break;
            }
          }
        } finally {
          lock.unlockReadAndWrite();
        }

        if (freeOffset >= 0) {
          synchronized (_allocationLock) {
            _allocationTable[freeOffset + 1] = 0;
            setAllocDirty(freeOffset + 1, freeOffset + 2);
          }
        }

        return blockAddr + fragOffset;
      } finally {
        block.free();
      }
    }
  }

  /**
   * Allocates a new miniFragment.
   *
   * @return the fragment address
   */
  private long allocateMiniFragmentBlock()
    throws IOException
  {
    while (true) {
      byte []allocationTable = _allocationTable;

      for (int i = _freeMiniAllocIndex;
           i < allocationTable.length;
           i += ALLOC_BYTES_PER_BLOCK) {
        int fragMask = allocationTable[i + 1] & 0xff;

        if (allocationTable[i] == ALLOC_MINI_FRAG && fragMask != 0xff) {
          _freeMiniAllocIndex = i;
          _freeMiniAllocCount++;

          synchronized (_allocationLock) {
            if (allocationTable[i] == ALLOC_MINI_FRAG && fragMask != 0xff) {
              allocationTable[i + 1] = (byte) 0xff;

              setAllocDirty(i + 1, i + 2);

              _miniFragmentUseCount++;

              long fragmentAddress
                = BLOCK_SIZE * ((long) i / ALLOC_BYTES_PER_BLOCK);

              return fragmentAddress;
            }
          }
        }
      }

      if (_freeMiniAllocCount == 0) {
        // if no fragment, allocate a new one.

        int count;
        if (_blockCount >= 256)
          count = 16;
        else
          count = 1;

        for (int i = 0; i < count; i++) {
          Block block = allocateBlockMiniFragment();
          block.free();
        }
      }

      _freeMiniAllocCount = 0;
      _freeMiniAllocIndex = 0;
    }
  }

  /**
   * Deletes a miniFragment.
   */
  public void deleteMiniFragment(long fragmentAddress)
    throws IOException
  {
    Block block = readBlock(fragmentAddress);

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        int fragIndex = (int) (fragmentAddress & BLOCK_OFFSET_MASK);
        int offset = fragIndex / 8 + MINI_FRAG_ALLOC_OFFSET;
        int mask = 1 << (fragIndex % 8);
        byte []blockBuffer = block.getBuffer();

        blockBuffer[offset] &= ~mask;
        block.setDirty(offset, offset + 1);

        int i = (int) (ALLOC_BYTES_PER_BLOCK * (fragmentAddress / BLOCK_SIZE));
        int j = (int) (fragmentAddress & 0xff);

        synchronized (_allocationLock) {
          int fragMask = _allocationTable[i + 1] & 0xff;
          //System.out.println((fragmentAddress / BLOCK_SIZE) + ":" + j + " DELETE");

          if (_allocationTable[i] != ALLOC_MINI_FRAG)
            System.out.println("BAD ENTRY: " + fragMask);

          _allocationTable[i + 1] = 0;

          _miniFragmentUseCount--;

          setAllocDirty(i + 1, i + 2);
        }
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a miniFragment.
   *
   * @param fragmentAddress the fragment to write
   * @param fragmentOffset the offset into the fragment
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   *
   * @return the fragment id
   */
  public Block writeMiniFragment(long fragmentAddress, int fragmentOffset,
                                 byte []buffer, int offset, int length)
    throws IOException
  {
    if (MINI_FRAG_SIZE - fragmentOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             fragmentOffset, length));

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        int blockOffset = getMiniFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        blockOffset += fragmentOffset;

        System.arraycopy(buffer, offset,
                         blockBuffer, blockOffset,
                         length);

        block.setDirty(blockOffset, blockOffset + length);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Writes a character based
   *
   * @param miniFragmentAddress the fragment to write
   * @param fragmentOffset the offset into the fragment
   * @param buffer the write buffer
   * @param offset offset into the write buffer
   * @param length the number of bytes to write
   */
  public Block writeMiniFragment(long fragmentAddress, int fragmentOffset,
                                 char []buffer, int offset, int length)
    throws IOException
  {
    if (MINI_FRAG_SIZE - fragmentOffset < length)
      throw new IllegalArgumentException(L.l("write offset {0} length {1} too long",
                                             fragmentOffset, length));

    Block block = readBlock(addressToBlockId(fragmentAddress));

    try {
      Lock lock = block.getLock();
      lock.lockReadAndWrite(_blockLockTimeout);

      try {
        int blockOffset = getMiniFragmentOffset(fragmentAddress);

        byte []blockBuffer = block.getBuffer();

        blockOffset += fragmentOffset;

        int blockTail = blockOffset;

        for (int i = 0; i < length; i++) {
          char ch = buffer[offset + i];

          blockBuffer[blockTail] = (byte) (ch >> 8);
          blockBuffer[blockTail + 1] = (byte) (ch);

          blockTail += 2;
        }

        block.setDirty(blockOffset, blockTail);
        
        return block;
      } finally {
        lock.unlockReadAndWrite();
      }
    } finally {
      block.free();
    }
  }

  /**
   * Returns the miniFragment offset for an id.
   */
  private int getMiniFragmentOffset(long fragmentAddress)
  {
    int id = (int) (fragmentAddress & BLOCK_OFFSET_MASK);

    return (int) (MINI_FRAG_SIZE * id);
  }

  /**
   * Reads a block into the buffer.
   */
  public void readBlock(long blockId, byte []buffer, int offset, int length)
    throws IOException
  {
    boolean isPriority = false;
    RandomAccessWrapper wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream is = wrapper.getFile();

      long blockAddress = blockId & BLOCK_MASK;

      if (blockAddress < 0 || _fileSize < blockAddress + length) {
        throw new IllegalStateException(L.l("block at 0x{0} is invalid for file {1} (length 0x{2})",
                                            Long.toHexString(blockAddress),
                                            _path,
                                            Long.toHexString(_fileSize)));
      }

      // System.out.println("READ: " + Long.toHexString(blockAddress));
      int readLen = is.read(blockAddress, buffer, offset, length);

      if (readLen < 0) {
        throw new IllegalStateException("Error reading " + is + " for block " + Long.toHexString(blockAddress) + " result=" + readLen);
      }

      if (readLen < length) {
        System.out.println("BAD-READ: " + Long.toHexString(blockAddress));
        if (readLen < 0)
          readLen = 0;

        for (int i = readLen; i < BLOCK_SIZE; i++)
          buffer[i] = 0;
      }

      _blockManager.addBlockRead();

      freeRowFile(wrapper, isPriority);
      wrapper = null;
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Saves the buffer to the database.
   */
  public void writeBlock(long blockAddress,
                         byte []buffer, int offset, int length,
                         boolean isPriority)
    throws IOException
  {
    RandomAccessWrapper wrapper;

    wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream os = wrapper.getFile();
      /*
      if (blockAddress > 2 * 0x2000000) {
      System.out.println("BLOCK: " + Long.toHexString(blockAddress) + " " + offset);
      Thread.dumpStack();
      }
      */
      os.write(blockAddress, buffer, offset, length);

      freeRowFile(wrapper, isPriority);
      wrapper = null;

      synchronized (_fileLock) {
        if (_fileSize < blockAddress + length) {
          _fileSize = blockAddress + length;
        }
      }

      _blockManager.addBlockWrite();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFile(boolean isPriority)
    throws IOException
  {
    // limit number of active row files

    if (! isPriority) {
      try {
        Thread.interrupted();
        _rowFileSemaphore.acquire();
      } catch (InterruptedException e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      }
    }

    RandomAccessWrapper wrapper = null;
    try {
      wrapper = openRowFileImpl();

      return wrapper;
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      if (wrapper == null)
        _rowFileSemaphore.release();
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFileImpl()
    throws IOException
  {
    RandomAccessStream file = null;
    RandomAccessWrapper wrapper = null;

    SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();

    if (ref != null) {
      wrapper = ref.get();
    }

    if (wrapper != null)
      file = wrapper.getFile();

    if (file == null) {
      Path path = _path;

      if (path != null) {
        file = path.openRandomAccess();

        wrapper = new RandomAccessWrapper(file);
      }
    }

    return wrapper;
  }

  private void freeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority)
      _rowFileSemaphore.release();

    SoftReference<RandomAccessWrapper> fileRef
      = new SoftReference<RandomAccessWrapper>(wrapper);

    if (_cachedRowFile.free(fileRef)) {
      return;
    }

    wrapper.close();
  }

  private void closeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority)
      _rowFileSemaphore.release();

    wrapper.close();
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
    }

    int id = _id;
    _id = 0;

    _path = null;

    RandomAccessWrapper wrapper = null;

    SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();

    if (ref != null)
      wrapper = ref.get();

    if (wrapper != null) {
      try {
        wrapper.close();
      } catch (Throwable e) {
      }
    }

    if (_blockManager != null) {
      _blockManager.freeStoreId(id);
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

  /**
   * Debug names for the allocation.
   */
  public static String codeToName(int code)
  {
    switch (code) {
    case ALLOC_FREE:
      return "free";
    case ALLOC_ROW:
      return "row";
    case ALLOC_USED:
      return "used";
    case ALLOC_FRAGMENT:
      return "fragment";
    case ALLOC_MINI_FRAG:
      return "mini-fragment";
    case ALLOC_INDEX:
      return "index";
    default:
      return String.valueOf(code);
    }
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
