/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.log;

import io.baratine.db.Cursor;
import io.baratine.service.Result;
import io.baratine.service.ResultFuture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.kraken.query.QueryKraken;
import com.caucho.v5.kraken.table.TableKraken;
import com.caucho.v5.kraken.table.TableManagerKraken;
import com.caucho.v5.management.server.LogMessage;

/**
 * Persistent logging.
 */
class LogDatabase
{
  private NameDatabase _nameDatabase;

  private TableManagerKraken _tableManager;
  private TableKraken _logTable;

  private QueryKraken _insertQuery;

  private QueryKraken _selectQuery;

  private QueryKraken _selectTimesQuery;

  LogDatabase(NameDatabase nameDatabase)
  {
    _nameDatabase = nameDatabase;

    createDatabases();
  }

  void log(long timestamp,
           long typeId,
           long nameId, 
           Level level,
           String message)
  {
    if (typeId < 0 || nameId < 0) {
      return;
    }
    
    _insertQuery.exec(Result.ignore(), timestamp, typeId, nameId, level.intValue(), message);
  }

  void log(long timestamp,
           long typeId,
           long nameId, 
           Level level,
           InputStream is)
  {
    if (typeId < 0 || nameId < 0) {
      return;
    }
    
    _insertQuery.exec(Result.ignore(), timestamp, typeId, nameId, level.intValue(), is);
  }

  private void createDatabases()
  {
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    _tableManager = kraken.getTableManager();

    String logTableSql
      = ("create table local.resin_log ("
          + " type int64"
         + ", time int64"
         + ", name int64"
         + ", level int32"
         + ", message string"
         + ", primary key(type, time)"
         + ")");

    QueryKraken createQuery = _tableManager.query(logTableSql);
    
    ResultFuture<Object> future = new ResultFuture<>();
    createQuery.exec(future);
    
    _logTable = (TableKraken) future.get(10, TimeUnit.SECONDS);
    // _logTable = _tableManager.createTable("resin_log", logTableSql);

    String insertSql = ("insert into local.resin_log"
                        + " (time,type,name,level,message)"
                        + " VALUES (?,?,?,?,?)");

    _insertQuery = _tableManager.query(insertSql);

    String selectSql = ("select time,name,level,message"
                        + " from local.resin_log "
                        + " where type=?"
                        + " AND ? <= level"
                        + " AND ? <= time and time <= ?");

    _selectQuery = _tableManager.query(selectSql);

    String selectTimesSql = ("select time,name,level"
                             + " from local.resin_log"
                             + " where type=?"
                             + " AND ? <= level"
                             + " AND ? <= time and time <= ?");

    _selectTimesQuery = _tableManager.query(selectTimesSql);
  }
  
  LogMessage []findMessages(long typeId,
                               String name,
                               int levelId,
                               long minTime, 
                               long maxTime)
  {
    String typeName = _nameDatabase.getName(typeId);
    
    ArrayList<LogMessage> messageList = new ArrayList<>();

    for (Cursor cursor : _selectQuery.findAll(new Object[] { typeId, levelId, minTime, maxTime })) {
      long time = cursor.getLong(1);
      long nameId = cursor.getLong(2);
      int level = cursor.getInt(3);
      
      String itemName = _nameDatabase.getName(nameId);

      if (name != null && (itemName == null || ! itemName.startsWith(name))) {
        continue;
      }
      
      LogMessage msg = new LogMessage();
      
      msg.setTimestamp(time);
      msg.setType(typeName);
      
      String levelName = getLevelName(level);
      
      msg.setLevel(levelName);
      msg.setName(itemName);
      
      msg.setMessage(cursor.getString(4));
      
      messageList.add(msg);
    }
    
    LogMessage []messages = new LogMessage[messageList.size()];
    
    messageList.toArray(messages);
    
    return messages;
  }
  
  LogMessage []findMessages(int []typeIds,
                            String name,
                            int levelId,
                            long minTime, 
                            long maxTime)
  {
    ArrayList<LogMessage> values = new ArrayList<>();
    
    for (int typeId : typeIds) {
      for (LogMessage msg : findMessages(typeId, name, levelId, minTime, maxTime)) {
        values.add(msg);
      }
    }

    LogMessage []messages = new LogMessage[values.size()];
    values.toArray(messages);

    return messages;
  }

  long []findMessageTimes(long typeId,
                          String name,
                          int levelId,
                          long minTime, 
                          long maxTime)
  {
    String typeName = _nameDatabase.getName(typeId);
    
    ArrayList<Long> values = new ArrayList<>();

    for (Cursor cursor : _selectTimesQuery.findAll(typeId, levelId, minTime, maxTime)) {
      long time = cursor.getLong(1);
      long nameId = cursor.getLong(2);
      int level = cursor.getInt(3);

      String itemName = _nameDatabase.getName(nameId);
        
      if (name != null && (itemName == null || ! itemName.startsWith(name)))
        continue;
        
      values.add(time);
    }

    long []times = new long[values.size()];
      
    for (int i = 0; i < times.length; i++) {
      times[i] = values.get(i);
    }
      
    Arrays.sort(times);
      
    return times;
  }

  public void deleteOldEntries(long delta)
  {
    /*
    ConnectionImpl conn = null;
    
    try {
      conn = getConnection();
      
      PreparedStatement pStmt = conn.getDeleteStmt();
      
      long now = CurrentTime.getCurrentTime();
      long first = now - delta;
      
      pStmt.setLong(1, first);
      pStmt.executeUpdate();
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      freeConnection(conn);
    }
    */
  }
  
  private String getLevelName(int level)
  {
    if (Level.SEVERE.intValue() <= level)
      return "severe";
    else if (Level.WARNING.intValue() <= level)
      return "warning";
    else if (Level.INFO.intValue() <= level)
      return "info";
    else if (Level.CONFIG.intValue() <= level)
      return "config";
    else if (Level.FINE.intValue() <= level)
      return "fine";
    else if (Level.FINER.intValue() <= level)
      return "finer";
    else if (Level.FINEST.intValue() <= level)
      return "finest";
    else
      return "off";
  }
}
