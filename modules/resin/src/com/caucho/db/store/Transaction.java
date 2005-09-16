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

import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.FreeList;
import com.caucho.util.LongKeyHashMap;

import com.caucho.log.Log;

import com.caucho.sql.SQLExceptionWrapper;

import com.caucho.db.store.Inode;

import com.caucho.db.table.Table;

import com.caucho.db.jdbc.ConnectionImpl;
/**
 * Represents a single transaction.
 */
public class Transaction {
  private static final Logger log = Log.open(Transaction.class);
  private static final L10N L = new L10N(Transaction.class);

  private static long AUTO_COMMIT_TIMEOUT = 30000L;

  private static final FreeList<Transaction> _freeList =
    new FreeList<Transaction>(32);

  private boolean _isAutoCommit = true;
  private ConnectionImpl _conn;
  
  private ArrayList<Lock> _readLocks = new ArrayList<Lock>();
  private ArrayList<Lock> _reqWriteLocks = new ArrayList<Lock>();
  private ArrayList<Lock> _writeLocks = new ArrayList<Lock>();
  
  private LongKeyHashMap<WriteBlock> _writeBlocks;

  // inodes that need to be deleted on a commit
  private ArrayList<Inode> _deleteInodes = new ArrayList<Inode>();
  
  // inodes that need to be deleted on a rollback
  private ArrayList<Inode> _addInodes = new ArrayList<Inode>();
  
  // blocks that need deallocating on a commit
  private ArrayList<Block> _deallocateBlocks = new ArrayList<Block>();

  private boolean _isRollbackOnly;

  private long _timeout = AUTO_COMMIT_TIMEOUT;

  private Transaction()
  {
  }

  public static Transaction create(ConnectionImpl conn)
  {
    Transaction xa = _freeList.allocate();

    if (xa == null) {
      xa = new Transaction();
    }
    
    xa.init(conn);

    return xa;
  }

  public static Transaction create()
  {
    Transaction xa = _freeList.allocate();

    if (xa == null)
      xa = new Transaction();

    return xa;
  }

  private void init(ConnectionImpl conn)
  {
    _conn = conn;
    _timeout = AUTO_COMMIT_TIMEOUT;
  }

  /**
   * Sets the transaction timeout.
   */
  public void setTimeout(long timeout)
  {
    _timeout = timeout;
  }
  
  /**
   * Acquires a new read lock.
   */
  public void addReadLock(Lock lock)
  {
    _readLocks.add(lock);
  }
  
  /**
   * Acquires a new read lock.
   */
  public boolean hasReadLock(Lock lock)
  {
    return _readLocks.contains(lock);
  }

  /**
   * Returns true for an auto-commit transaction.
   */
  public boolean isAutoCommit()
  {
    return _isAutoCommit;
  }

  /**
   * Returns true for an auto-commit transaction.
   */
  public void setAutoCommit(boolean autoCommit)
  {
    _isAutoCommit = autoCommit;
  }
  
  /**
   * Acquires a new write lock.
   */
  public void lockRead(Lock lock)
    throws SQLException
  {
    if (_isRollbackOnly)
      throw new SQLException(L.l("can't get lock with rollback transaction"));

    try {
      if (! _readLocks.contains(lock)) {
	lock.lockRead(this, _timeout);
	_readLocks.add(lock);
      }
    } catch (SQLException e) {
      setRollbackOnly();
      
      throw e;
    }
  }

  
  /**
   * Acquires a new write lock.
   */
  public void lockWrite(Lock lock)
    throws SQLException
  {
    if (_isRollbackOnly)
      throw new SQLException(L.l("can't get lock with rollback transaction"));

    try {
      // the actual locking happens at the commit
      // this should probably be an upgrade lock
      
      if (_reqWriteLocks.contains(lock)) {
	return;
      }
      else if (_readLocks.contains(lock)) {
	lock.lockUpgrade(this, _timeout);
	_reqWriteLocks.add(lock);
      }
      else {
	lock.lockReadAndUpgrade(this, _timeout);
	_readLocks.add(lock);
	_reqWriteLocks.add(lock);
      }
    } catch (SQLException e) {
      setRollbackOnly();
      
      throw e;
    }
  }
  
  /**
   * Acquires a new write lock.
   */
  public void lockAutoCommitRead(Lock lock)
    throws SQLException
  {
    lock.lockRead(this, _timeout);
  }
  
  /**
   * Acquires a new write lock.
   */
  public void unlockAutoCommitRead(Lock lock)
    throws SQLException
  {
    lock.unlockRead();
  }
  
  /**
   * Acquires a new write lock.
   */
  public void lockAutoCommitWrite(Lock lock)
    throws SQLException
  {
    lock.lockReadAndUpgrade(this, _timeout);
    try {
      lock.lockWrite(this, _timeout);
    } catch (SQLException e) {
      lock.unlockUpgrade();

      throw e;
    }
  }
  
  /**
   * Acquires a new write lock.
   */
  public void commitAutoCommitWrite(Lock lock)
    throws SQLException
  {
    lock.unlockWrite();
    lock.unlockUpgrade();
    lock.unlockRead();
  }


  /**
   * Returns a read block.
   */
  public Block readBlock(Store store, long blockAddress)
    throws IOException
  {
    long blockId = store.addressToBlockId(blockAddress);
      
    Block block;
    
    if (_writeBlocks != null)
      block = _writeBlocks.get(blockId);
    else
      block = null;

    if (block != null)
      block.allocate();
    else
      block = store.readBlock(blockId);

    return block;
  }

  /**
   * Returns a modified block.
   */
  public WriteBlock getWriteBlock(long blockId)
  {
    if (_writeBlocks == null)
      return null;

    return _writeBlocks.get(blockId);
  }

  /**
   * Returns a modified block.
   */
  public WriteBlock createWriteBlock(Block block)
    throws IOException
  {
    if (block instanceof WriteBlock)
      return (WriteBlock) block;

    WriteBlock writeBlock;
    
    if (isAutoCommit())
      writeBlock = new AutoCommitWriteBlock(block);
    else {
      // XXX: locking
      writeBlock = new XAWriteBlock(block);
    }

    setBlock(writeBlock);

    return writeBlock;
  }

  /**
   * Returns a modified block.
   */
  public WriteBlock createAutoCommitWriteBlock(Block block)
    throws IOException
  {
    if (block instanceof WriteBlock) {
      block.allocate();
      return (WriteBlock) block;
    }
    else {
      WriteBlock writeBlock = new AutoCommitWriteBlock(block);
      
      setBlock(writeBlock);

      return writeBlock;
    }
  }

  /**
   * Returns a modified block.
   */
  public WriteBlock allocateRow(Store store)
    throws IOException
  {
    Block block = store.allocateRow();

    WriteBlock writeBlock;

    if (isAutoCommit())
      writeBlock = new AutoCommitWriteBlock(block);
    else
      writeBlock = new XAWriteBlock(block);

    setBlock(writeBlock);

    return writeBlock;
  }

  /**
   * Returns a modified block.
   */
  public void deallocateBlock(Block block)
    throws IOException
  {
    if (isAutoCommit())
      block.getStore().freeBlock(block.getBlockId());
    else
      _deallocateBlocks.add(block);
  }

  /**
   * Returns a modified block.
   */
  public WriteBlock createWriteBlock(Store store, long blockAddress)
    throws IOException
  {
    Block block = readBlock(store, blockAddress);
    
    if (block instanceof WriteBlock)
      return (WriteBlock) block;

    WriteBlock writeBlock;
    
    if (isAutoCommit())
      writeBlock = new AutoCommitWriteBlock(block);
    else {
      // XXX: locking too
      writeBlock = new XAWriteBlock(block);
    }

    setBlock(writeBlock);

    return writeBlock;
  }

  /**
   * Returns a modified block.
   */
  private void setBlock(WriteBlock block)
  {
    // block.setDirty();

    if (_writeBlocks == null)
      _writeBlocks = new LongKeyHashMap<WriteBlock>(8);

    _writeBlocks.put(block.getBlockId(), block);
  }

  /**
   * Adds inode which should be deleted on a commit.
   */
  public void addDeleteInode(Inode inode)
  {
    _deleteInodes.add(inode);
  }

  /**
   * Adds inode which should be deleted on a rollback.
   */
  public void addAddInode(Inode inode)
  {
    _addInodes.add(inode);
  }

  public void autoCommit()
    throws SQLException
  {
    if (_isAutoCommit) {
      ConnectionImpl conn = _conn;
      _conn = null;
      
      if (conn != null) {
	conn.setTransaction(null);
      }
    }
  }

  public void setRollbackOnly()
  {
    _isRollbackOnly = true;

    releaseLocks();

    // XXX: release write blocks
    _writeBlocks = null;
  }

  public void setRollbackOnly(SQLException e)
  {
    setRollbackOnly();
  }

  public void commit()
    throws SQLException
  {
    try {
      LongKeyHashMap<WriteBlock> writeBlocks = _writeBlocks;

      if (_reqWriteLocks != null) {
	for (int i = 0; i < _reqWriteLocks.size(); i++) {
	  Lock lock = _reqWriteLocks.get(i);
	  lock.lockWrite(this, _timeout);

	  _writeLocks.add(lock);
	}
      }

      for (int i = 0; i < _deleteInodes.size(); i++) {
	Inode inode = _deleteInodes.get(i);

	// XXX: should be allocating based on auto-commit
	inode.remove();
      }

      if (writeBlocks != null) {
	try {
	  Iterator<WriteBlock> blockIter = writeBlocks.valueIterator();

	  while (blockIter.hasNext()) {
	    WriteBlock block = blockIter.next();

	    block.commit();
	  }
	} catch (IOException e) {
	  throw new SQLExceptionWrapper(e);
	}
      }

      for (int i = 0; i < _deallocateBlocks.size(); i++) {
	Block block = _deallocateBlocks.get(i);

	try {
	  block.getStore().freeBlock(block.getBlockId());
	} catch (IOException e) {
	  throw new SQLExceptionWrapper(e);
	}
      }
    } finally {
      releaseLocks();

      close();
    }
  }

  public void rollback()
    throws SQLException
  {
    releaseLocks();

    close();
  }

  private void releaseLocks()
  {
    // need to unlock write before upgrade to block other threads
    if (_writeLocks != null) {
      for (int i = 0; i < _writeLocks.size(); i++) {
	Lock lock = _writeLocks.get(i);

	try {
	  lock.unlockWrite();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      _writeLocks.clear();
    }
    
    if (_reqWriteLocks != null) {
      for (int i = 0; i < _reqWriteLocks.size(); i++) {
	Lock lock = _reqWriteLocks.get(i);

	try {
	  // if (! _writeLocks.contains(lock))
	  lock.unlockUpgrade();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      _reqWriteLocks.clear();
    }
    
    if (_readLocks != null) {
      for (int i = 0; i < _readLocks.size(); i++) {
	Lock lock = _readLocks.get(i);

	try {
	  lock.unlockRead();
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }

      _readLocks.clear();
    }

    _deleteInodes.clear();
    _addInodes.clear();
  }

  void close()
  {
    if (_readLocks != null)
      _readLocks.clear();
    
    if (_reqWriteLocks != null)
      _reqWriteLocks.clear();
    
    if (_writeLocks != null)
      _writeLocks.clear();

    LongKeyHashMap<WriteBlock> writeBlocks = _writeBlocks;
    _writeBlocks = null;

    if (writeBlocks != null) {
      Iterator<WriteBlock> blockIter = writeBlocks.valueIterator();

      while (blockIter.hasNext()) {
	WriteBlock block = blockIter.next();

	block.destroy();
      }
      
      // writeBlocks.clear();
    }

    _deallocateBlocks.clear();
    _deleteInodes.clear();
    _addInodes.clear();
    _isRollbackOnly = false;

    _freeList.free(this);
  }
}
