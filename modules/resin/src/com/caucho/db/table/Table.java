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

package com.caucho.db.table;

import com.caucho.db.Database;
import com.caucho.db.index.BTree;
import com.caucho.db.index.KeyCompare;
import com.caucho.db.lock.Lock;
import com.caucho.db.sql.CreateQuery;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.Parser;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.store.Block;
import com.caucho.db.store.BlockStore;
import com.caucho.db.xa.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;
import com.caucho.util.TaskWorker;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Table format:
 *
 * <pre>
 * Block 0: allocation table
 * Block 1: fragment table
 * Block 2: table definition
 *   0    - store data
 *   1024 - table data
 *    1024 - index pointers
 *   2048 - CREATE text
 * Block 3: first data
 * </pre>
 */
public class Table extends BlockStore {
  private final static Logger log
    = Logger.getLogger(Table.class.getName());
  private final static L10N L = new L10N(Table.class);

  private final static int ROOT_DATA_OFFSET = STORE_CREATE_END;
  private final static int INDEX_ROOT_OFFSET = ROOT_DATA_OFFSET + 32;

  private final static int ROOT_DATA_END = ROOT_DATA_OFFSET + 1024;

  public final static int INLINE_BLOB_SIZE = 120;

  public final static long ROW_CLOCK_MIN = 1024;

  public final static byte ROW_VALID = 0x1;
  public final static byte ROW_ALLOC = 0x2;
  public final static byte ROW_MASK = 0x3;

  private final static String DB_VERSION = "Resin-DB 4.0.2";
  private final static String MIN_VERSION = "Resin-DB 4.0.2";

  private final Row _row;

  private final int _rowLength;
  private final int _rowsPerBlock;
  private final int _rowEnd;

  private final Constraint[]_constraints;

  private final Column _autoIncrementColumn;

  private long _entries;

  private static final int FREE_ROW_SIZE = 256;
  private final AtomicLongArray _insertFreeRowArray
    = new AtomicLongArray(FREE_ROW_SIZE);
  private final AtomicInteger _insertFreeRowTop = new AtomicInteger();

  // top of file counters for row insert allocation
  private final Object _rowTailLock = new Object();
  private long _rowTailTop = BlockStore.BLOCK_SIZE * 256;
  private final AtomicLong _rowTailOffset = new AtomicLong();

  private final Object _rowClockLock = new Object();

  private final RowAllocator _rowAllocator = new RowAllocator();

  // clock counters for row insert allocation
  private long _rowClockTop;
  private long _rowClockOffset;

  private long _rowClockFree;
  private long _rowClockUsed;

  private long _autoIncrementValue = -1;

  private Lock _allocLock;
  private Lock _insertLock;

  Table(Database database, String name, Row row, Constraint constraints[])
  {
    super(database, name, null);

    _row = row;
    _constraints = constraints;

    _rowLength = _row.getLength();
    _rowsPerBlock = BLOCK_SIZE / _rowLength;
    _rowEnd = _rowLength * _rowsPerBlock;

    Column []columns = _row.getColumns();
    Column autoIncrementColumn = null;
    for (int i = 0; i < columns.length; i++) {
      columns[i].setTable(this);

      if (columns[i].getAutoIncrement() >= 0)
        autoIncrementColumn = columns[i];
    }
    _autoIncrementColumn = autoIncrementColumn;

    _insertLock = new Lock("table-insert:" + name);
    _allocLock = new Lock("table-alloc:" + name);
  }

  Row getRow()
  {
    return _row;
  }

  /**
   * Returns the length of a row.
   */
  int getRowLength()
  {
    return _rowLength;
  }

  /**
   * Returns the end of the row
   */
  int getRowEnd()
  {
    return _rowEnd;
  }

  public final Column []getColumns()
  {
    return _row.getColumns();
  }

  /**
   * Returns the table's constraints.
   */
  public final Constraint []getConstraints()
  {
    return _constraints;
  }

  /**
   * Returns the auto-increment column.
   */
  public Column getAutoIncrementColumn()
  {
    return _autoIncrementColumn;
  }

  /**
   * Returns the column for the given column name.
   *
   * @param name the column name
   *
   * @return the column
   */
  public Column getColumn(String name)
  {
    Column []columns = getColumns();

    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getName().equals(name))
        return columns[i];
    }

    return null;
  }

  /**
   * Returns the column index for the given column name.
   *
   * @param name the column name
   *
   * @return the column index.
   */
  public int getColumnIndex(String name)
    throws SQLException
  {
    Column []columns = getColumns();

    for (int i = 0; i < columns.length; i++) {
      if (columns[i].getName().equals(name))
        return i;
    }

    return -1;
  }

  //
  // initialization
  //

  /**
   * Loads the table from the file.
   */
  public static Table loadFromFile(Database db, String name)
    throws IOException, SQLException
  {
    Path path = db.getPath().lookup(name + ".db");

    if (! path.exists()) {
      if (log.isLoggable(Level.FINE))
        log.fine(db + " '" + path.getNativePath() + "' is an unknown table");

      return null; //throw new SQLException(L.l("table {0} does not exist", name));
    }

    String version = null;

    ReadStream is = path.openRead();
    try {
      // skip allocation table and fragment table
      is.skip(DATA_START + ROOT_DATA_OFFSET);

      StringBuilder sb = new StringBuilder();
      int ch;

      while ((ch = is.read()) > 0) {
        sb.append((char) ch);
      }

      version = sb.toString();

      if (! version.startsWith("Resin-DB")) {
        throw new SQLException(L.l("table {0} is not a Resin DB.  Version '{1}'",
                                   name, version));
      }
      else if (version.compareTo(MIN_VERSION) < 0 ||
               DB_VERSION.compareTo(version) < 0) {
        throw new SQLException(L.l("table {0} is out of date.  Old version {1}.",
                                   name, version));
      }
    } finally {
      is.close();
    }

    is = path.openRead();
    try {
      // skip allocation table and fragment table
      is.skip(DATA_START + ROOT_DATA_END);

      StringBuilder cb = new StringBuilder();

      int ch;
      while ((ch = is.read()) > 0) {
        cb.append((char) ch);
      }

      String sql = cb.toString();

      if (log.isLoggable(Level.FINER))
        log.finer("Table[" + name + "] " + version + " loading\n" + sql);

      try {
        CreateQuery query = (CreateQuery) Parser.parse(db, sql);

        TableFactory factory = query.getFactory();

        if (! factory.getName().equalsIgnoreCase(name))
          throw new IOException(L.l("factory {0} does not match", name));

        Table table = new Table(db, factory.getName(), factory.getRow(),
                                factory.getConstraints());

        table.init();

        table.clearIndexes();
        table.initIndexes();
        table.rebuildIndexes();

        return table;
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);

        throw new SQLException(L.l("can't load table {0} in {1}.\n{2}",
                                   name, path.getNativePath(), e.toString()));
      }
    } finally {
      is.close();
    }
  }

  /**
   * Creates the table.
   */
  public void create()
    throws IOException, SQLException
  {
    super.create();

    initIndexes();

    byte []tempBuffer = new byte[BLOCK_SIZE];

    readBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE);

    TempStream ts = new TempStream();

    WriteStream os = new WriteStream(ts);

    try {
      for (int i = 0; i < ROOT_DATA_OFFSET; i++)
        os.write(tempBuffer[i]);

      writeTableHeader(os);
    } finally {
      os.close();
    }

    TempBuffer head = ts.getHead();
    int offset = 0;
    for (; head != null; head = head.getNext()) {
      byte []buffer = head.getBuffer();

      int length = head.getLength();

      System.arraycopy(buffer, 0, tempBuffer, offset, length);

      for (; length < buffer.length; length++) {
        tempBuffer[offset + length] = 0;
      }

      offset += buffer.length;
    }

    for (; offset < BLOCK_SIZE; offset++)
      tempBuffer[offset] = 0;

    boolean isPriority = false;
    writeBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE, isPriority);

    _database.addTable(this);
  }

  /**
   * Initialize the indexes
   */
  private void initIndexes()
    throws IOException, SQLException
  {
    Column []columns = _row.getColumns();
    for (int i = 0; i < columns.length; i++) {
      Column column = columns[i];

      if (! column.isUnique())
        continue;

      KeyCompare keyCompare = column.getIndexKeyCompare();

      if (keyCompare == null)
        continue;

      Block rootBlock = allocateIndexBlock();
      long rootBlockId = rootBlock.getBlockId();
      rootBlock.free();

      BTree btree = new BTree(this, rootBlockId, column.getLength(),
                              keyCompare);

      column.setIndex(btree);
    }
  }

  /**
   * Clears the indexes
   */
  private void clearIndexes()
    throws IOException
  {
    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      BTree index = columns[i].getIndex();

      if (index == null)
        continue;

      long rootAddr = index.getIndexRoot();

      Block block = readBlock(addressToBlockId(rootAddr));

      try {
        byte []blockBuffer = block.getBuffer();

        synchronized (blockBuffer) {
          for (int j = 0; j < blockBuffer.length; j++) {
            blockBuffer[j] = 0;
          }

          block.setDirty(0, BLOCK_SIZE);
        }
      } finally {
        block.free();
      }
    }

    long blockAddr = 0;

    while ((blockAddr = firstBlock(blockAddr + BLOCK_SIZE, ALLOC_INDEX)) > 0) {
      freeBlock(blockAddr);
    }
  }

  /**
   * Rebuilds the indexes
   */
  private void rebuildIndexes()
    throws IOException, SQLException
  {
    Transaction xa = Transaction.create();
    xa.setAutoCommit(true);

    try {
      TableIterator iter = createTableIterator();

      iter.init(xa);

      Column []columns = _row.getColumns();

      while (iter.nextBlock()) {
        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          try {
            long rowAddress = iter.getRowAddress();
            int rowOffset = iter.getRowOffset();

            for (int i = 0; i < columns.length; i++) {
              Column column = columns[i];

              /*
              if (column.getIndex() != null)
                System.out.println(Long.toHexString(iter.getBlock().getBlockId()) + ":" + Long.toHexString(rowAddress) + ":" + Long.toHexString(rowOffset) + ": " + column.getIndexKeyCompare().toString(blockBuffer, rowOffset + column.getColumnOffset(), column.getLength()));
              */

              column.setIndex(xa, blockBuffer, rowOffset, rowAddress, null);
            }
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    } finally {
      xa.commit();
    }
  }

  /**
   * Rebuilds the indexes
   */
  public void validate()
    throws SQLException
  {
    try {
      validateIndexes();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Rebuilds the indexes
   */
  public void validateIndexes()
    throws IOException, SQLException
  {
    Transaction xa = Transaction.create();
    xa.setAutoCommit(true);

    try {
      TableIterator iter = createTableIterator();

      iter.init(xa);

      Column []columns = _row.getColumns();

      while (iter.nextBlock()) {
        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          try {
            long rowAddress = iter.getRowAddress();
            int rowOffset = iter.getRowOffset();

            for (int i = 0; i < columns.length; i++) {
              Column column = columns[i];

              column.validateIndex(xa, blockBuffer, rowOffset, rowAddress);
            }
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    } finally {
      xa.commit();
    }
  }

  private void writeTableHeader(WriteStream os)
    throws IOException
  {
    os.print(DB_VERSION);
    os.write(0);

    while (os.getPosition() < INDEX_ROOT_OFFSET) {
      os.write(0);
    }

    Column []columns = _row.getColumns();
    for (int i = 0; i < columns.length; i++) {
      if (! columns[i].isUnique())
        continue;

      BTree index = columns[i].getIndex();

      if (index != null) {
        writeLong(os, index.getIndexRoot());
      }
      else {
        writeLong(os, 0);
      }
    }

    while (os.getPosition() < ROOT_DATA_END) {
      os.write(0);
    }

    os.print("CREATE TABLE " + getName() + "(");
    for (int i = 0; i < _row.getColumns().length; i++) {
      Column column = _row.getColumns()[i];

      if (i != 0)
        os.print(",");

      os.print(column.getName());
      os.print(" ");

      switch (column.getTypeCode()) {
      case Column.VARCHAR:
        os.print("VARCHAR(" + column.getDeclarationSize() + ")");
        break;
      case Column.VARBINARY:
        os.print("VARBINARY(" + column.getDeclarationSize() + ")");
        break;
      case Column.BINARY:
        os.print("BINARY(" + column.getDeclarationSize() + ")");
        break;
      case Column.SHORT:
        os.print("SMALLINT");
        break;
      case Column.INT:
        os.print("INTEGER");
        break;
      case Column.LONG:
        os.print("BIGINT");
        break;
      case Column.DOUBLE:
        os.print("DOUBLE");
        break;
      case Column.DATE:
        os.print("TIMESTAMP");
        break;
      case Column.BLOB:
        os.print("BLOB");
        break;
      case Column.NUMERIC:
        {
          NumericColumn numeric = (NumericColumn) column;

          os.print("NUMERIC(" + numeric.getPrecision() + "," + numeric.getScale() + ")");
          break;
        }
      default:
        throw new UnsupportedOperationException();
      }

      if (column.isPrimaryKey())
        os.print(" PRIMARY KEY");
      else if (column.isUnique())
        os.print(" UNIQUE");

      if (column.isNotNull())
        os.print(" NOT NULL");

      Expr defaultExpr = column.getDefault();

      if (defaultExpr != null) {
        os.print(" DEFAULT (");
        os.print(defaultExpr);
        os.print(")");
      }

      if (column.getAutoIncrement() >= 0)
        os.print(" auto_increment");
    }
    os.print(")");

    /*
    writeLong(os, _blockMax);
    writeLong(os, _entries);
    writeLong(os, _clockAddr);
    */
  }

  public TableIterator createTableIterator()
  {
    assertStoreActive();

    return new TableIterator(this);
  }

  /**
   * Returns the next auto-increment value.
   */
  public long nextAutoIncrement(QueryContext context)
    throws SQLException
  {
    synchronized (this) {
      if (_autoIncrementValue >= 0)
        return ++_autoIncrementValue;
    }

    long max = 0;

    try {
      TableIterator iter = createTableIterator();
      iter.init(context);
      while (iter.next()) {
        byte []buffer = iter.getBuffer();

        long value = _autoIncrementColumn.getLong(buffer, iter.getRowOffset());

        if (max < value)
          max = value;
      }
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }

    synchronized (this) {
      if (_autoIncrementValue < max)
        _autoIncrementValue = max;

      return ++_autoIncrementValue;
    }
  }

  //
  // insert code
  //

  /**
   * Inserts a new row, returning the row address.
   */
  public long insert(QueryContext queryContext, Transaction xa,
                     ArrayList<Column> columns,
                     ArrayList<Expr> values)
    throws IOException, SQLException
  {
    if (log.isLoggable(Level.FINEST))
      log.finest("db table " + getName() + " insert row xa:" + xa);

    Block block = null;

    try {
      while (true) {
        long blockId = allocateInsertRow();

        block = xa.loadBlock(this, blockId);

        int rowOffset = allocateRow(block, xa);

        if (rowOffset >= 0) {
          insertRow(queryContext, xa, columns, values,
                    block, rowOffset);

          block.saveAllocation();

          freeRow(blockId);

          return blockIdToAddress(blockId, rowOffset);
        }

        Block freeBlock = block;
        block = null;
        freeBlock.free();
      }
    } finally {
      if (block != null)
        block.free();
    }
  }

  private int allocateRow(Block block, Transaction xa)
    throws IOException, SQLException
  {
    Lock blockLock = block.getLock();

    blockLock.lockReadAndWrite(xa.getTimeout());
    try {
      block.read();

      byte []buffer = block.getBuffer();

      int rowOffset = 0;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          buffer[rowOffset] = ROW_ALLOC;

          block.setDirty(rowOffset, rowOffset + 1);

          return rowOffset;
        }
      }
    } finally {
      blockLock.unlockReadAndWrite();
    }

    return -1;
  }

  public void insertRow(QueryContext queryContext, Transaction xa,
                        ArrayList<Column> columns,
                        ArrayList<Expr> values,
                        Block block, int rowOffset)
    throws SQLException
  {
    byte []buffer = block.getBuffer();

    long rowAddr = blockIdToAddress(block.getBlockId(), rowOffset);
    //System.out.println("ADDR:" + rowAddr + " " + rowOffset + " " + block);

    TableIterator iter = createTableIterator();
    TableIterator []iterSet = new TableIterator[] { iter };
    // QueryContext context = QueryContext.allocate();
    boolean isReadOnly = false;
    queryContext.init(xa, iterSet, isReadOnly);
    iter.init(queryContext);

    boolean isOkay = false;
    queryContext.lock();
    try {
      iter.setRow(block, rowOffset);

      if (buffer[rowOffset] != ROW_ALLOC)
        throw new IllegalStateException(L.l("Expected ROW_ALLOC at '{0}'",
                                            buffer[rowOffset]));

      for (int i = rowOffset + _rowLength - 1; rowOffset < i; i--)
        buffer[i] = 0;

      for (int i = 0; i < columns.size(); i++) {
        Column column = columns.get(i);
        Expr value = values.get(i);

        column.setExpr(xa, buffer, rowOffset, value, queryContext);
      }

      // lock for insert, i.e. entries, indices, and validation
      // XXX: the set index needs to handle the validation
      //xa.lockWrite(_insertLock);
      try {
        validate(block, rowOffset, queryContext, xa);

        for (int i = 0; i < columns.size(); i++) {
          Column column = columns.get(i);
          Expr value = values.get(i);

          column.setIndex(xa, buffer, rowOffset, rowAddr, queryContext);
        }

        buffer[rowOffset] = (byte) ((buffer[rowOffset] & ~ROW_MASK) | ROW_VALID);

        xa.addUpdateBlock(block);

        if (_autoIncrementColumn != null) {
          long value = _autoIncrementColumn.getLong(buffer, rowOffset);

          synchronized (this) {
            if (_autoIncrementValue < value)
              _autoIncrementValue = value;
          }
        }

        block.setDirty(rowOffset, rowOffset + _rowLength);
        _entries++;

        isOkay = true;
      } catch (SQLException e) {
        // e.printStackTrace();
        throw e;
      } finally {
        // xa.unlockWrite(_insertLock);

        if (! isOkay) {
          delete(xa, block, buffer, rowOffset, false);
          block.setDirty(rowOffset, rowOffset + _rowLength);
        }
      }
    } finally {
      queryContext.unlock();
    }
  }

  //
  // row allocation
  //

  private long allocateInsertRow()
    throws IOException
  {
    long blockId = allocateFreeRow();

    if (blockId != 0) {
      return blockId;
    }

    long rowTailOffset = _rowTailOffset.get();

    blockId = firstRow(rowTailOffset);

    if (blockId <= 0) {
      Block block = allocateRow();

      blockId = block.getBlockId();

      block.free();
    }

    _rowTailOffset.compareAndSet(rowTailOffset, blockId + BLOCK_SIZE);

    return blockId;
  }

  private void popRow(long blockId)
  {
    popFreeRow(blockId);

    _rowTailOffset.compareAndSet(blockId, blockId + BLOCK_SIZE);
  }

  private long peekFreeRow()
  {
    int top = _insertFreeRowTop.get();
    long blockId = 0;

    if (top > 0) {
      blockId = _insertFreeRowArray.get(top - 1);
    }

    if (2 * top < FREE_ROW_SIZE && _rowTailTop < _rowTailOffset.get()) {
      _rowAllocator.wake();
    }

    return blockId;
  }

  private void popFreeRow(long blockId)
  {
    int top = _insertFreeRowTop.get();

    if (top > 0 && _insertFreeRowArray.compareAndSet(top - 1, blockId, 0)) {
      _insertFreeRowTop.compareAndSet(top, top - 1);
    }
  }

  private long allocateFreeRow()
  {
    int top = _insertFreeRowTop.get();
    long blockId = 0;

    if (top > 0 && _insertFreeRowTop.compareAndSet(top, top - 1)) {
      blockId = _insertFreeRowArray.getAndSet(top - 1, 0);
    }

    if (2 * top < FREE_ROW_SIZE && _rowTailTop <= _rowTailOffset.get()) {
      _rowAllocator.wake();
    }

    return blockId;
  }

  //
  // allocator
  //

  private void fillFreeRows()
  {
    if (_rowClockTop <= _rowClockOffset) {
      // force 50% free rows before clock starts again
      long count = (_rowClockUsed - _rowClockFree) / _rowsPerBlock;

      // minimum 256 blocks of free rows
      if (_rowClockFree < 256 && _rowClockOffset > 0)
        count = 256;

      if (count > 0) {
        _rowTailTop = _rowTailOffset.get() + count * BlockStore.BLOCK_SIZE;
      }

      _rowClockOffset = 0;
      _rowClockTop = _rowTailOffset.get();
      _rowClockUsed = 0;
      _rowClockFree = 0;

      if (count > 0)
        return;
    }

    while (isFreeRowAvailable()) {
      long rowClockOffset = _rowClockOffset;

      try {
        rowClockOffset = firstRow(rowClockOffset);

        if (rowClockOffset < 0) {
          rowClockOffset = _rowClockTop;
          return;
        }

        if (isRowFree(rowClockOffset)) {
          freeRow(rowClockOffset);

          _rowClockFree++;
        }
        else {
          _rowClockUsed++;
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);

        rowClockOffset = _rowClockTop;
      } finally {
        _rowClockOffset = rowClockOffset + BlockStore.BLOCK_SIZE;
      }
    }
  }

  /**
   * Test if any row in the block is free
   */
  private boolean isRowFree(long blockId)
    throws IOException
  {
    Block block = readBlock(blockId);

    try {
      int rowOffset = 0;

      byte []buffer = block.getBuffer();
      boolean isFree = false;

      for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
        if (buffer[rowOffset] == 0) {
          isFree = true;
          _rowClockFree++;
        }
        else
          _rowClockUsed++;
      }

      return isFree;
    } finally {
      block.free();
    }
  }

  private boolean isFreeRowAvailable()
  {
    return _insertFreeRowTop.get() < FREE_ROW_SIZE;
  }

  private void freeRow(long blockId)
  {
    int top;

    do {
      top = _insertFreeRowTop.get();

      if (top >= FREE_ROW_SIZE)
        return;

      _insertFreeRowArray.set(top, blockId);
    } while (! _insertFreeRowTop.compareAndSet(top, top + 1));
  }

  //
  // insert
  //

  /**
   * Validates the given row.
   */
  private void validate(Block block, int rowOffset,
                        QueryContext queryContext, Transaction xa)
    throws SQLException
  {
    TableIterator row = createTableIterator();
    TableIterator []rows = new TableIterator[] { row };

    row.setRow(block, rowOffset);

    for (int i = 0; i < _constraints.length; i++) {
      _constraints[i].validate(rows, queryContext, xa);
    }
  }

  void delete(Transaction xa, Block block,
              byte []buffer, int rowOffset,
              boolean isDeleteIndex)
    throws SQLException
  {
    byte rowState = buffer[rowOffset];

    /*
    if ((rowState & ROW_MASK) == 0)
      return;
    */

    buffer[rowOffset] = (byte) ((rowState & ~ROW_MASK) | ROW_ALLOC);

    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      columns[i].deleteData(xa, buffer, rowOffset);
    }

    if (isDeleteIndex) {
      for (int i = 0; i < columns.length; i++) {
        try {
          columns[i].deleteIndex(xa, buffer, rowOffset);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    buffer[rowOffset] = 0;
  }

  @Override
  public void close()
  {

    _row.close();

    super.close();

    _rowAllocator.destroy();
  }

  private void writeLong(WriteStream os, long value)
    throws IOException
  {
    os.write((int) (value >> 56));
    os.write((int) (value >> 48));
    os.write((int) (value >> 40));
    os.write((int) (value >> 32));
    os.write((int) (value >> 24));
    os.write((int) (value >> 16));
    os.write((int) (value >> 8));
    os.write((int) value);
  }

  private void setLong(byte []buffer, int offset, long value)
    throws IOException
  {
    buffer[offset + 0] = (byte) (value >> 56);
    buffer[offset + 1] = (byte) (value >> 48);
    buffer[offset + 2] = (byte) (value >> 40);
    buffer[offset + 3] = (byte) (value >> 32);
    buffer[offset + 4] = (byte) (value >> 24);
    buffer[offset + 5] = (byte) (value >> 16);
    buffer[offset + 6] = (byte) (value >> 8);
    buffer[offset + 7] = (byte) (value);
  }

  private long getLong(byte []buffer, int offset)
    throws IOException
  {
    long value = (((buffer[offset + 0] & 0xffL) << 56)
                  + ((buffer[offset + 1] & 0xffL) << 48)
                  + ((buffer[offset + 2] & 0xffL) << 40)
                  + ((buffer[offset + 3] & 0xffL) << 32)

                  + ((buffer[offset + 4] & 0xffL) << 24)
                  + ((buffer[offset + 5] & 0xffL) << 16)
                  + ((buffer[offset + 6] & 0xffL) << 8)
                  + ((buffer[offset + 7] & 0xffL)));

    return value;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + ":" + getId() + "]";
  }

  class RowAllocator extends TaskWorker {
    public void runTask()
    {
      fillFreeRows();
    }
  }
}
