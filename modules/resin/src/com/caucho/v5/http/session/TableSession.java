/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.session;

import io.baratine.core.Result;
import io.baratine.db.Cursor;

import java.io.InputStream;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.kelp.TableListener;
import com.caucho.v5.kraken.KrakenSystem;
import com.caucho.v5.kraken.query.QueryKraken;
import com.caucho.v5.kraken.table.TableKraken;
import com.caucho.v5.kraken.table.TableManagerKraken;
import com.caucho.v5.util.Hex;

/**
 * Persistent table for a session.
 */
class TableSession
{
  private final String _contextId;
  
  private TableKraken _table;
  private TableManagerKraken _tableManager;
  private QueryKraken _querySave;
  private QueryKraken _queryLoad;

  private QueryKraken _queryRemove;

  private QueryKraken _queryExplain;

  private TableListenerSession _listener;

  private SessionManager _sessionManager;
  
  /**
   * Creates the table.
   */
  TableSession(SessionManager manager, String contextId)
  {
    _sessionManager = manager;
    
    _contextId = contextId;
    
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    _tableManager = kraken.getTableManager();
    
    _table = createTable();
    
    String sqlSave = "insert into cluster.caucho_session (id,hash,access_time,access_timeout,value) values (?,?,?,?,?)";
    
    _querySave = _tableManager.query(sqlSave);
    
    String sqlRemove = "delete from cluster.caucho_session where id=?";
    
    _queryRemove = _tableManager.query(sqlRemove);
    
    String sqlLoad = "select id,hash,access_time,access_timeout,value from cluster.caucho_session where id=?";
    
    _queryLoad = _tableManager.query(sqlLoad);
    
    String sqlExplain = "explain " + sqlLoad;
    
    _queryExplain = _tableManager.query(sqlExplain);
    
    _listener = new TableListenerSession();
    _table.addListener(_listener);
  }
  
  private TableKraken createTable()
  {
    String sql;
    
    sql = ("create table cluster.caucho_session ("
           + "  id string primary key,"
           + "  hash int64,"
           + "  access_time int32,"
           + "  access_timeout int32,"
           + "  value blob"
           + ") with hash '" + HashSession.class.getName() + "'");
    
    //TableKraken table = _tableManager.createTable("caucho_session", sql);
    
    TableKraken table = (TableKraken) _tableManager.execSync(sql, new Object[0]);
    
    return table;
  }

  void save(String sid, 
            long hash,
            long accessTime,
            long accessTimeout,
            InputStream is)
  {
    String id = generateId(sid);
    //ServiceFuture<Object> future = new ServiceFuture<>();
    
    //_querySave.exec(future, id, sid, hash, is);
    
    //future.get(1, TimeUnit.SECONDS);
    
    int accessTimeSec = (int) (accessTime / 1000);
    int accessTimeoutSec = (int) (accessTimeout / 1000);
    
    if (accessTimeoutSec <= 0 && accessTimeout < 0) {
      accessTimeoutSec = -1;
    }

    if (_sessionManager.isSaveWaitForAcknowledge()) {
      _querySave.execSync(id, hash, accessTimeSec, accessTimeoutSec, is);
    }
    else {
      _querySave.exec(Result.ignore(), id, hash, accessTimeSec, accessTimeoutSec, is);
    }
  }

  public void remove(String sid)
  {
    String id = generateId(sid);


    if (_sessionManager.isSaveWaitForAcknowledge()) {
      _queryRemove.execSync(id);
    }
    else {
      _queryRemove.exec(Result.ignore(), id);
    }
  }

  Cursor load(String sid)
  {
    String id = generateId(sid);
    
    return _queryLoad.findOneFuture(id);
  }
  
  long loadHash(Cursor cursor)
  {
    return cursor.getLong(2);
  }
  
  long loadAccessTime(Cursor cursor)
  {
    return cursor.getInt(3) * 1000L;
  }
  
  long loadAccessTimeout(Cursor cursor)
  {
    return cursor.getInt(4) * 1000L;
  }
  
  InputStream loadValue(Cursor cursor)
  {
    return cursor.getInputStream(5);
  }
  
  String generateId(String sid)
  {
    return sid + "/" + _contextId;
    /*
    long hash = Murmur64.SEED;
    
    hash = Murmur64.generate(hash, _contextId);
    hash = Murmur64.generate(hash, sid);
    
    return hash;
    */
  }
  
  void close()
  {
    _table.removeListener(_listener);
  }
  
  private class TableListenerSession implements TableListener {
    @Override
    public void onPut(byte[] key, TypePut type)
    {
      //System.out.println("PUT: " + Hex.toShortHex(key) + " " + type + " " + BartenderSystem.getCurrentSelfServer());
    }

    @Override
    public void onRemove(byte[] key, TypePut type)
    {
      //System.out.println("REMOVE: " + Hex.toShortHex(key) + " " + type + " " + BartenderSystem.getCurrentSelfServer());
    }
  }
}
