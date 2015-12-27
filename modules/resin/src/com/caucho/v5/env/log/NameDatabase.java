/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.log;

import io.baratine.core.Result;
import io.baratine.core.ResultFuture;
import io.baratine.db.Cursor;

import java.util.concurrent.TimeUnit;

import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.kraken.query.QueryKraken;
import com.caucho.v5.kraken.table.TableManagerKraken;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.util.Murmur64;

/**
 * Persistent logging.
 */
class NameDatabase
{
  private LruCache<String,Long> _nameCache
    = new LruCache<>(8192);

  private LruCache<Long,String> _revNameCache
    = new LruCache<>(8192);

  private TableManagerKraken _tableManager;
  private QueryKraken _insertQuery;
  private QueryKraken _findByNameQuery;
  private QueryKraken _findByIdQuery;

  NameDatabase()
  {
    createDatabases();
  }

  long getNameId(String name)
  {
    Long cachedId = _nameCache.get(name);

    if (cachedId != null) {
      return cachedId;
    }
    
    long id = loadId(name);

    if (id != 0) {
      return id;
    }
    
    id = Math.abs(Murmur64.generate(0, name));
    
    _insertQuery.exec(Result.ignore(), id, name);

    _nameCache.put(name, id);
    _revNameCache.put(id, name);

    return id;
  }
  
  int[] getNameIds(String []names)
  {
    int []ids = new int[names.length];
    
    for(int i = 0; i < ids.length; i++) {
      ids[i] = (int) getNameId(names[i]);
    }
    
    return ids;
  }
  
  private long loadId(String name)
  {
    Cursor cursor = _findByNameQuery.findOneFuture(new Object[] { name });
    
    if (cursor != null) {
      return cursor.getLong(1);
    }
      
    return 0;
  }

  String getName(long id)
  {
    String key = _revNameCache.get(id);

    if (key != null) {
      return key;
    }

    Cursor cursor = _findByIdQuery.findOneFuture(new Object[] { id });
    
    if (cursor != null) {
      String name = cursor.getString(1);

      _nameCache.put(name, id);
      _revNameCache.put(id, name);

      return name;
    }
    
    return null;
  }

  private void createDatabases()
  {
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    _tableManager = kraken.getTableManager();

    String logTableSql
      = ("create table local.resin_log_name ("
          + " id int64 primary key"
         + ", name string"
         + ")");
    
    QueryKraken createQuery = _tableManager.query(logTableSql);
    
    ResultFuture<Object> result = new ResultFuture<>();
    createQuery.exec(result);
    result.get(10, TimeUnit.SECONDS);

    String insertSql = ("insert into local.resin_log_name"
                        + " (id,name)"
                        + " VALUES (?,?)");

    _insertQuery = _tableManager.query(insertSql);

    String findByNameSql = ("select id from local.resin_log_name where name=?");

    _findByNameQuery = _tableManager.query(findByNameSql);

    String findByIdSql = ("select name from local.resin_log_name where id=?");

    _findByIdQuery = _tableManager.query(findByIdSql);
  }
}
