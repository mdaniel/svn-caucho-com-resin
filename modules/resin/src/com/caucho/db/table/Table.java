/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.db.Database;
import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.db.index.BTree;
import com.caucho.db.index.KeyCompare;
import com.caucho.db.jdbc.GeneratedKeysResultSet;
import com.caucho.db.sql.CreateQuery;
import com.caucho.db.sql.Expr;
import com.caucho.db.sql.Parser;
import com.caucho.db.sql.QueryContext;
import com.caucho.db.xa.DbTransaction;
import com.caucho.inject.Module;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.BitsUtil;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.SQLExceptionWrapper;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

/**
 * Table format:
 *
 * <pre>
 * Block 0: allocation table
 * Block 1: table definition
 *   0    - store data
 *   1024 - table data
 *    1024 - index pointers
 *   2048 - CREATE text
 * Block 2: first data
 * </pre>
 */
@Module
public class Table extends BlockStore
{
  private final static Logger log
    = Logger.getLogger(Table.class.getName());
  private final static L10N L = new L10N(Table.class);

  private final static int TABLE_DATA_OFFSET = STORE_CREATE_END;

  private final static int STARTUP_TIMESTAMP_OFFSET = TABLE_DATA_OFFSET;
  private final static int SHUTDOWN_TIMESTAMP_OFFSET = TABLE_DATA_OFFSET + 8;
  private final static int TABLE_DATA_END = SHUTDOWN_TIMESTAMP_OFFSET + 8;

  private final static int ROOT_DATA_OFFSET = TABLE_DATA_END;

  private final static int INDEX_ROOT_OFFSET = ROOT_DATA_OFFSET + 768;

  private final static int ROOT_DATA_END = ROOT_DATA_OFFSET + 1024;

  public final static int INLINE_BLOB_SIZE = 120;

  public final static byte ROW_VALID = 0x1;
  public final static byte ROW_ALLOC = 0x2;
  public final static byte ROW_MASK = 0x3;

  private final static String DB_VERSION = "Resin-DB 4.0.28";
  private final static String MIN_VERSION = "Resin-DB 4.0.28";

  private final Row _row;

  private final int _rowLength;
  private final int _rowsPerBlock;
  private final int _rowEnd;

  private final Constraint[]_constraints;

  private final Column _identityColumn;
  private final Column _autoIncrementColumn;

  private long _startupTimestamp;

  private final TableRowAllocator _rowAllocator;

  private final AtomicLong _rowDeleteCount = new AtomicLong();

  private long _autoIncrementValue = -1;

  private final Lifecycle _lifecycle;

  Table(Database database, String name, Row row, Constraint constraints[])
  {
    super(database, name, null);

    _lifecycle = new Lifecycle(log, name);

    _row = row;
    _constraints = constraints;

    _rowLength = _row.getLength();
    _rowsPerBlock = BLOCK_SIZE / _rowLength;
    _rowEnd = _rowLength * _rowsPerBlock;

    Column []columns = _row.getColumns();
    Column autoIncrementColumn = null;
    Column identityColumn = null;

    for (int i = 0; i < columns.length; i++) {
      columns[i].setTable(this);

      if (columns[i].getAutoIncrement() >= 0)
        autoIncrementColumn = columns[i];

      if (columns[i] instanceof IdentityColumn) {
        identityColumn = columns[i];
      }
    }

    _autoIncrementColumn = autoIncrementColumn;
    _identityColumn = identityColumn;

    //new Lock("table-insert:" + name);
    //new Lock("table-alloc:" + name);
    _rowAllocator = new TableRowAllocator(this);
  }

  @Override
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  Row getRow()
  {
    return _row;
  }

  /**
   * Returns the length of a row.
   */
  public int getRowLength()
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

  /**
   * Returns the end of the row
   */
  int getRowsPerBlock()
  {
    return _rowsPerBlock;
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
      is.skip(METADATA_START + ROOT_DATA_OFFSET);

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
      is.skip(METADATA_START + ROOT_DATA_END);

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

        boolean isReadIndex = table.readIndexes();

        if (! table.isShutdownTimestampValid()) {
          log.info(L.l("{0} validating indexes due to unclean shutdown.",
                       table));

          if (! isReadIndex || ! table.validateIndexesSafe()) {
            log.warning(L.l("rebuilding indexes for '{0}' because they did not properly validate on startup"
                            + ", total blocks={1}",
                            table, table.getBlockCount()));

            table.clearIndexes();
            table.createIndexes();
            table.rebuildIndexes();

            log.warning(L.l("rebuilding indexes for '{0}' finished.",
                            table));
          }
        }
        
        if (table.getAllocation(0) != BlockStore.ALLOC_DATA) {
          throw new IllegalStateException("Invalid table load");
        }

        table.writeStartupTimestamp();

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
  @Override
  public void create()
    throws IOException, SQLException
  {
    super.create();

    createIndexes();

    byte []tempBuffer = new byte[BLOCK_SIZE];

    getReadWrite().readBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE);

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
    getReadWrite().writeBlock(BLOCK_SIZE, tempBuffer, 0, BLOCK_SIZE, isPriority);

    _database.addTable(this);

    writeStartupTimestamp();
    
    wakeWriter();
  }

  private void writeStartupTimestamp()
    throws IOException
  {
    _startupTimestamp = CurrentTime.getCurrentTime();
    int offset = STARTUP_TIMESTAMP_OFFSET;

    writeTimestamp(offset, _startupTimestamp);

    _lifecycle.toActive();
  }

  private void writeShutdownTimestamp()
    throws IOException
  {
    int offset = SHUTDOWN_TIMESTAMP_OFFSET;
    
    writeTimestamp(offset, _startupTimestamp);
  }

  private void writeTimestamp(int offset, long timestamp)
    throws IOException
  {
    long metaBlockAddress = METADATA_START;
    Block metaBlock = readBlock(metaBlockAddress);
    try {
      byte []buffer = metaBlock.getBuffer();

      _startupTimestamp = CurrentTime.getCurrentTime();

      BitsUtil.writeLong(buffer, offset, timestamp);

      metaBlock.setDirty(offset, offset + 8);

      metaBlock.commit();
    } finally {
      metaBlock.free();
    }
  }

  private boolean isShutdownTimestampValid()
    throws IOException
  {
    long metaBlockAddress = METADATA_START;
    Block metaBlock = readBlock(metaBlockAddress);

    try {
      byte []buffer = metaBlock.getBuffer();

      long startupTimestamp
        = BitsUtil.readLong(buffer, STARTUP_TIMESTAMP_OFFSET);

      long shutdownTimestamp
        = BitsUtil.readLong(buffer, SHUTDOWN_TIMESTAMP_OFFSET);
      
      return startupTimestamp == shutdownTimestamp && startupTimestamp != 0;
    } finally {
      metaBlock.free();
    }
  }


  /**
   * Creates the indexes
   */
  private void createIndexes()
    throws IOException, SQLException
  {
    int indexCount = 0;

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

      long metaBlockAddress = METADATA_START;
      Block metaBlock = readBlock(metaBlockAddress);
      try {
        byte []buffer = metaBlock.getBuffer();

        int newIndexCount = indexCount++;

        int offset = newIndexCount * 8 + INDEX_ROOT_OFFSET;

        BitsUtil.writeLong(buffer, offset, rootBlockId);
      } finally {
        metaBlock.free();
      }
    }
  }

  /**
   * Creates the indexes
   */
  private boolean readIndexes()
    throws IOException, SQLException
  {
    int indexCount = 0;

    Column []columns = _row.getColumns();
    for (int i = 0; i < columns.length; i++) {
      Column column = columns[i];

      if (! column.isUnique())
        continue;

      KeyCompare keyCompare = column.getIndexKeyCompare();

      if (keyCompare == null)
        continue;

      long rootBlockId = -1;

      long metaBlockAddress = METADATA_START;
      Block metaBlock = readBlock(metaBlockAddress);
      try {
        byte []buffer = metaBlock.getBuffer();

        int newIndexCount = indexCount++;

        int offset = newIndexCount * 8 + INDEX_ROOT_OFFSET;

        rootBlockId = BitsUtil.readLong(buffer, offset);

        if (! isIndexBlock(rootBlockId)) {
          return false;
        }
      } finally {
        metaBlock.free();
      }

      BTree btree = new BTree(this, rootBlockId, column.getLength(),
                              keyCompare);

      column.setIndex(btree);
    }

    return true;
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
      deallocateBlock(blockAddr);
    }
  }

  /**
   * Rebuilds the indexes
   */
  private void rebuildIndexes()
    throws IOException, SQLException
  {
    DbTransaction xa = DbTransaction.create();
    xa.setAutoCommit(true);

    try {
      TableIterator iter = createTableIterator();

      iter.init(xa);

      Column []columns = _row.getColumns();

      int blockCount = 0;
      int rowCount = 0;

      while (iter.nextBlock()) {
        if (blockCount++ > 0 && blockCount % 10000 == 0) {
          log.info(L.l("rebuilding indexes for '{0}': processed {1} blocks", this, blockCount));
        }

        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          if (rowCount++ > 0 && rowCount % 100000 == 0) {
            log.info(L.l("rebuilding indexes for '{0}': processed {1} rows", this, rowCount));
          }

          long rowAddress = iter.getRowAddress();
          int rowOffset = iter.getRowOffset();

          try {
            if (! isValid(blockBuffer, rowOffset, columns)) {
              log.warning(L.l("{0}: removing corrupted row (0x{1})", this, Long.toHexString(rowAddress)));

              iter.delete();
              continue;
            }

            for (int i = 0; i < columns.length; i++) {
              Column column = columns[i];

              /*
              if (column.getIndex() != null)
                System.out.println(Long.toHexString(iter.getBlock().getBlockId()) + ":" + Long.toHexString(rowAddress) + ":" + Long.toHexString(rowOffset) + ": " + column.getIndexKeyCompare().toString(blockBuffer, rowOffset + column.getColumnOffset(), column.getLength()));
              */

              column.setIndex(xa, blockBuffer, rowOffset, rowAddress, null);
            }
          } catch (Exception e) {
            log.warning(L.l("{0} deleting row because of index rebuild failure: {1}"
                            + "\n  {2}", this, Long.toHexString(rowAddress), e));

            if (log.isLoggable(Level.FINER)) {
              log.log(Level.FINER, e.toString(), e);
            }

            iter.deleteRowOnly();
          }
        }
      }
    } finally {
      xa.commit();
    }
  }

  private boolean isValid(byte []blockBuffer,
                          int rowOffset,
                          Column []columns)
  {
    for (int i = 0; i < columns.length; i++) {
      if (! columns[i].isValid(blockBuffer, rowOffset)) {
        return false;
      }
    }

    return true;
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

  private boolean validateIndexesSafe()
  {
    try {
      if (! validateIndexes()) {
        return false;
      }

      return true;
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, e.toString(), e);
      }
      else {
        log.warning(e.toString());
      }
    }

    return false;
  }

  /**
   * Rebuilds the indexes
   */
  private boolean validateIndexes()
    throws IOException, SQLException
  {
    final Column []columns = _row.getColumns();

    boolean isIndex = false;

    for (Column column : columns) {
      if (column.getIndex() != null)
        isIndex = true;
    }

    if (! isIndex) {
      return true;
    }

    return validateIndexByRow();
  }


  /*
   * Rebuilds the indexes
   */
  private boolean validateIndexByRow()
    throws IOException, SQLException
  {
    final Column []columns = _row.getColumns();

    boolean isValid = false;

    DbTransaction xa = DbTransaction.create();
    xa.setAutoCommit(true);

    TableIterator iter = null;

    try {
      iter = createTableIterator();

      iter.init(xa);

      while (iter.nextBlock()) {
        iter.initRow();

        byte []blockBuffer = iter.getBuffer();

        while (iter.nextRow()) {
          long rowAddress = iter.getRowAddress();
          int rowOffset = iter.getRowOffset();

          for (int i = 0; i < columns.length; i++) {
            Column column = columns[i];

            column.validateIndex(xa, blockBuffer, rowOffset, rowAddress);
          }
        }
      }

      isValid = true;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (iter != null)
        iter.free();

      xa.commit();

    }

    return isValid;
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
      case IDENTITY:
        os.print("IDENTITY");
        break;
      case VARCHAR:
        os.print("VARCHAR(" + column.getDeclarationSize() + ")");
        break;
      case VARBINARY:
        os.print("VARBINARY(" + column.getDeclarationSize() + ")");
        break;
      case BINARY:
        os.print("BINARY(" + column.getDeclarationSize() + ")");
        break;
      case SHORT:
        os.print("SMALLINT");
        break;
      case INT:
        os.print("INTEGER");
        break;
      case LONG:
        os.print("BIGINT");
        break;
      case DOUBLE:
        os.print("DOUBLE");
        break;
      case DATE:
        os.print("TIMESTAMP");
        break;
      case BLOB:
        os.print("BLOB");
        break;
      case NUMERIC:
        {
          NumericColumn numeric = (NumericColumn) column;

          os.print("NUMERIC(" + numeric.getPrecision() + "," + numeric.getScale() + ")");
          break;
        }
      default:
        throw new UnsupportedOperationException(String.valueOf(column));
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
        long blockId = iter.getBlockId();

        long value = _autoIncrementColumn.getLong(blockId, buffer, iter.getRowOffset());

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
  public long insert(QueryContext queryContext, DbTransaction xa,
                     ArrayList<Column> columns,
                     ArrayList<Expr> values)
    throws IOException, SQLException
  {
    if (log.isLoggable(Level.ALL))
      log.log(Level.ALL, "db table " + getName() + " insert row xa:" + xa);

    Block block = null;

    try {
      while (true) {
        long blockId = _rowAllocator.allocateInsertRowBlock();

        block = xa.loadBlock(this, blockId);

        int rowOffset = _rowAllocator.allocateRow(block, xa);

        if (rowOffset >= 0) {
          insertRow(queryContext, xa, columns, values,
                    block, rowOffset);
          
          block.saveAllocation();

          _rowAllocator.freeRowBlockId(blockId);

          return blockIdToAddress(blockId, rowOffset);
        }

        Block freeBlock = block;
        block = null;
        freeBlock.free();
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    } finally {
      if (block != null)
        block.free();
    }
  }

  public void insertRow(QueryContext queryContext, DbTransaction xa,
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
    
    if (! queryContext.lock()) {
      log.warning("Unable to lock table");
      return;
    }

    try {
      iter.setRow(block, rowOffset);

      if (buffer[rowOffset] != ROW_ALLOC)
        throw new IllegalStateException(L.l("Expected ROW_ALLOC at '{0}'",
                                            buffer[rowOffset]));

      for (int i = rowOffset + _rowLength - 1; rowOffset < i; i--)
        buffer[i] = 0;

      // set non-blob fields first
      for (int i = 0; i < columns.size(); i++) {
        Column column = columns.get(i);
        Expr value = values.get(i);

        column.setExpr(xa, buffer, rowOffset, value, queryContext);
      }

      for (int i = 0; i < columns.size(); i++) {
        Column column = columns.get(i);
        Expr value = values.get(i);

        column.setExprBlob(xa, buffer, rowOffset, value, queryContext);
      }

      // lock for insert, i.e. entries, indices, and validation
      // XXX: the set index needs to handle the validation
      //xa.lockWrite(_insertLock);
      try {
        validate(block, rowOffset, queryContext, xa);

        for (int i = 0; i < columns.size(); i++) {
          Column column = columns.get(i);

          column.setIndex(xa, buffer, rowOffset, rowAddr, queryContext);
        }
        
        xa.writeData(); // 

        buffer[rowOffset] = (byte) ((buffer[rowOffset] & ~ROW_MASK) | ROW_VALID);

        xa.addUpdateBlock(block);

        long autoId = 0;

        if (_autoIncrementColumn != null) {
          long blockId = iter.getBlockId();

          autoId = _autoIncrementColumn.getLong(blockId, buffer, rowOffset);

          synchronized (this) {
            if (_autoIncrementValue < autoId)
              _autoIncrementValue = autoId;
          }
        }

        block.setDirty(rowOffset, rowOffset + _rowLength);

        GeneratedKeysResultSet rs = queryContext.getGeneratedKeysResultSet();

        if (rs != null) {
          if (_autoIncrementColumn != null) {
            rs.setColumn(1, _autoIncrementColumn);
            rs.setLong(1, autoId);

          }
          else if (_identityColumn != null) {
            rs.setColumn(1, _identityColumn);
            rs.setLong(1, rowAddr);
          }
        }

        isOkay = true;
      } catch (SQLException e) {
        // e.printStackTrace();
        throw e;
      } finally {
        // xa.unlockWrite(_insertLock);
        queryContext.unlock();

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
  // insert
  //

  /**
   * Validates the given row.
   */
  private void validate(Block block, int rowOffset,
                        QueryContext queryContext, DbTransaction xa)
    throws SQLException
  {
    TableIterator row = createTableIterator();
    TableIterator []rows = new TableIterator[] { row };

    row.setRow(block, rowOffset);

    for (int i = 0; i < _constraints.length; i++) {
      _constraints[i].validate(rows, queryContext, xa);
    }
  }

  boolean delete(DbTransaction xa, Block block,
                 byte []buffer, int rowOffset,
                 boolean isDeleteIndex)
    throws SQLException
  {
    byte rowState = buffer[rowOffset];

    //if ((rowState & ROW_MASK) == 0) {
    if ((rowState & ROW_MASK) != ROW_VALID) {
      return false;
    }

    buffer[rowOffset] = (byte) ((rowState & ~ROW_MASK) | ROW_ALLOC);

    Column []columns = _row.getColumns();

    for (int i = 0; i < columns.length; i++) {
      try {
        columns[i].deleteData(xa, buffer, rowOffset);
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
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

    Arrays.fill(buffer, rowOffset, rowOffset + _row.getLength(), (byte) 0); 
    buffer[rowOffset] = 0;

    _rowDeleteCount.incrementAndGet();
    
    return true;
  }

  /**
   * @return
   */
  public long getRowDeleteCount()
  {
    return _rowDeleteCount.get();
  }

  @Override
  public void close()
  {
    if (! _lifecycle.toDestroy()) {
      return;
    }

    try {
      if (fsync()) {
        writeShutdownTimestamp();
                
        fsync();
      }
      else {
        log.warning(this + " fsync on close failed.");
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    _row.close();

    super.close();

    _rowAllocator.close();
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

  @Override
  public String toString()
  {
    int id = CurrentTime.isTest() ? 1 : getId();

    String path = "";

    if (! CurrentTime.isTest()) {
      path = "," + getPath().getNativePath();
    }

    return getClass().getSimpleName() + "[" + getName() + ":" + id + path + "]";
  }
}
