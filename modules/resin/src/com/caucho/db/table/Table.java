/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
import com.caucho.db.sql.CreateQuery;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.Parser;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.store.Block;
import com.caucho.db.store.Lock;
import com.caucho.db.store.Store;
import com.caucho.db.store.Transaction;
import com.caucho.sql.SQLExceptionWrapper;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
public class Table extends Store {
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

  private final Object _rowClockLock = new Object();
  private long _rowClockAddr;
  private long _rowClockTotal;
  private long _rowClockUsed;
  private int _rowClockCount;
  private int _rowAllocCount;

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

    _rowClockAddr = 0;

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

    writeBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE);

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
          long rowAddress = iter.getRowAddress();
          int rowOffset = iter.getRowOffset();

          for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];

            column.setIndex(xa, blockBuffer, rowOffset, rowAddress, null);
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
      long addr;
      int rowOffset = 0;

      boolean isLoop = false;
      boolean hasRow = false;

      int rowClockCount = 0;
      long rowClockAddr = 0;
      long rowClockUsed = 0;
      long rowClockTotal = 0;

      do {
        long blockId = 0;

        if (block != null) {
          block.free();
          block = null;
        }

        synchronized (_rowClockLock) {
          blockId = firstRow(_rowClockAddr);

          if (blockId >= 0) {
          }
          else if (! isLoop
                   && (ROW_CLOCK_MIN < _rowClockTotal
                       && 4 * _rowClockUsed < 3 * _rowClockTotal
                       || _rowAllocCount > 8)) {
            // System.out.println("LOOP: used:" + _rowClockUsed + " total:" + _rowClockTotal + " frac:" + (double) _rowClockUsed / (double) (_rowClockTotal + 0.01));
            // go around loop if there are sufficient entries, i.e. over
            // ROW_CLOCK_MIN and at least 1/4 free entries.
            isLoop = true;
            _rowClockCount = 0;
            _rowClockAddr = 0;
            _rowClockUsed = 0;
            _rowClockTotal = 0;
            _rowAllocCount = 0;
            continue;
          }
          else {
            //System.out.println("ROW: used:" + _rowClockUsed + " total:" + _rowClockTotal + " frac:" + (double) _rowClockUsed / (double) (_rowClockTotal + 0.01));

            _rowAllocCount++;

            // if no free row is available, allocate a new one
            block = xa.allocateRow(this);
            //System.out.println("ALLOC: " + block);

            blockId = block.getBlockId();
          }

          rowClockCount = _rowClockCount;
          rowClockAddr = blockIdToAddress(blockId);
          rowClockUsed = _rowClockUsed;
          rowClockTotal = _rowClockTotal;

          // the next insert will try the following block
          _rowClockCount++;
          _rowClockAddr = rowClockAddr + BLOCK_SIZE;
          _rowClockUsed = rowClockUsed + _rowsPerBlock;
          _rowClockTotal = rowClockTotal + _rowsPerBlock;
        }

        if (block == null)
          block = xa.readBlock(this, blockId);

        Lock blockLock = block.getLock();

        if (xa.lockReadAndWriteNoWait(blockLock)) {
          try {
            rowOffset = 0;

            byte []buffer = block.getBuffer();

            for (; rowOffset < _rowEnd; rowOffset += _rowLength) {
              if (buffer[rowOffset] == 0) {
                block.setDirty(rowOffset, rowOffset + 1);

                hasRow = true;
                buffer[rowOffset] = ROW_ALLOC;
                break;
              }
            }
          } finally {
            xa.unlockReadAndWrite(blockLock);
          }
        }
      } while (! hasRow);

      insertRow(queryContext, xa, columns, values,
                block, rowOffset);

      synchronized (_rowClockLock) {
        if (rowClockCount < _rowClockCount) {
          // the next insert will retry this block
          int blocks = _rowClockCount - rowClockCount;

          _rowClockCount = rowClockCount;
          _rowClockAddr = rowClockAddr;
          _rowClockUsed -= blocks * _rowsPerBlock;
          _rowClockTotal -= blocks * _rowsPerBlock;
        }
      }

      return blockIdToAddress(block.getBlockId(), rowOffset);
    } finally {
      if (block != null)
        block.free();
    }
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
    queryContext.init(xa, iterSet, true);
    iter.init(queryContext);

    boolean isOkay = false;
    queryContext.lock();
    try {
      iter.setRow(block, rowOffset);

      block.setDirty(rowOffset, rowOffset + _rowLength);

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

        buffer[rowOffset] = (byte) ((buffer[rowOffset] & ~ROW_MASK) | ROW_VALID);

        for (int i = 0; i < columns.size(); i++) {
          Column column = columns.get(i);
          Expr value = values.get(i);

          column.setIndex(xa, buffer, rowOffset, rowAddr, queryContext);
        }

        xa.addUpdateBlock(block);

        if (_autoIncrementColumn != null) {
          long value = _autoIncrementColumn.getLong(buffer, rowOffset);

          synchronized (this) {
            if (_autoIncrementValue < value)
              _autoIncrementValue = value;
          }
        }

        _entries++;

        isOkay = true;
      } finally {
        // xa.unlockWrite(_insertLock);

        if (! isOkay)
          delete(xa, block, buffer, rowOffset, false);
      }
    } finally {
      queryContext.unlock();
    }
  }

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

    if ((rowState & ROW_MASK) != ROW_VALID)
      return;

    buffer[rowOffset] = (byte) ((rowState & ~ROW_MASK) | ROW_ALLOC);

    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      columns[i].deleteData(xa, buffer, rowOffset);
    }

    if (isDeleteIndex) {
      for (int i = 0; i < columns.length; i++) {
        columns[i].deleteIndex(xa, buffer, rowOffset);
      }
    }

    buffer[rowOffset] = 0;

    synchronized (_rowClockLock) {
      long addr = blockIdToAddress(block.getBlockId());

      if (addr <= _rowClockAddr) {
        _rowClockUsed--;
      }
    }
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
}
