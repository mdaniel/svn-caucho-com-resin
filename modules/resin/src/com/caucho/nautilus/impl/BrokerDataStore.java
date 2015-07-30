/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus.impl;

import io.baratine.core.Result;
import io.baratine.db.Cursor;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bartender.BartenderSystem;
import com.caucho.bartender.ServerBartender;
import com.caucho.kraken.KrakenSystem;
import com.caucho.kraken.query.QueryKraken;
import com.caucho.kraken.table.TableManagerKraken;
import com.caucho.nautilus.broker.BrokerNautilusBase;
import com.caucho.util.CurrentTime;

/**
 * Backing store for the broker.
 */
class BrokerDataStore extends BrokerNautilusBase
{
  private static final Logger log
    = Logger.getLogger(BrokerDataStore.class.getName());
  
  private AtomicLong _qidGen = new AtomicLong();
  private HashMap<String,BrokerQueue> _queueCache = new HashMap<>();

  private QueryKraken _insertMessageQuery;

  private QueryKraken _restoreMessage;

  private QueryKraken _loadMessage;

  private QueryKraken _deleteMessage;

  BrokerDataStore(KrakenSystem kraken)
  {
    Objects.requireNonNull(kraken);
    
    long serverId = 0;
    
    try {
      ServerBartender server = BartenderSystem.getCurrentSelfServer();
    
      serverId = server.getServerIndex();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    // seq assigned to avoid conflict between servers and restarts
    long seq = (serverId << 56) + (CurrentTime.getCurrentTime() << 24);
    
    _qidGen.set(seq);
    
    TableManagerKraken tableManager = kraken.getTableManager();
    
    String createQueueSql = ("create table resin_nautilus_queue ("
                            + " qid int64 primary key,"
                            + " name string"
                            + ")");
    
    tableManager.createTable("resin_nautilus_queue", createQueueSql);
    
    String createMessageSql = ("create table resin_nautilus_message ("
                               + " qid int64,"
                               + " mid int64,"
                               + " value blob,"
                               + " primary key(qid, mid)"
                               + ")");
    
    tableManager.createTable("resin_nautilus_message", createMessageSql);
    
    String insertMessageSql = ("insert into resin_nautilus_message"
                              + " (qid, mid, value) VALUES (?,?,?)");
        
    _insertMessageQuery = tableManager.query(insertMessageSql);
    
    String restoreMessageSql = ("select qid, mid from resin_nautilus_message"
                               + " where qid=?");
    
    _restoreMessage = tableManager.query(restoreMessageSql);

    String loadMessageSql = ("select value from resin_nautilus_message"
                             + " where qid=? and mid=?");
    
    _loadMessage = tableManager.query(loadMessageSql);

    String deleteMessageSql = ("delete from resin_nautilus_message"
                             + " where qid=? and mid=?");
    
    _deleteMessage = tableManager.query(deleteMessageSql);
    
    /*
    sql = ("CREATE TABLE message ("
        + "  id IDENTITY,"
        + "  queue BIGINT,"
        + "  mid BIGINT,"
        + "  priority INTEGER,"
        + "  expire_time BIGINT,"
        + "  data BLOB"
        + ")");
   */

    /*
    _queueCache = new ClusterCache();
    _queueCache.setName("resin.message.nautilus");
    _queueCache.setAccessedExpireTimeoutMillis(Long.MAX_VALUE / 2);
    _queueCache.setModifiedExpireTimeoutMillis(Long.MAX_VALUE / 2);
    _queueCache.init();
    */
  }
  
  BrokerQueue addQueue(String name)
  {
    BrokerQueue queue = (BrokerQueue) _queueCache.get(name);

    if (queue == null) {
      queue = new BrokerQueue(name, _qidGen.incrementAndGet());
      
      //_queueCache.putIfAbsent(name, queue);
      _queueCache.put(name, queue);
      
      queue = (BrokerQueue) _queueCache.get(name);
      
      if (queue == null) {
        throw new NullPointerException();
      }
    }
    
    return queue;
  }

  void restoreMessage(QueueServiceLocal queue)
  {
    for (Cursor cursor : _restoreMessage.findAll(queue.getId())) {
      long qid = cursor.getLong(1);
      long mid = cursor.getLong(2);
      
      queue.onMessageRestore(qid, mid);
    }
  }
  void saveMessage(long qid, long mid, InputStream is)
  {
    _insertMessageQuery.exec(Result.ignore(), qid, mid, is);
  }

  InputStream loadMessage(long qid, long mid)
  {
    Cursor cursor = _loadMessage.findOneFuture(qid, mid);
    
    if (cursor != null) {
      return cursor.getInputStream(1);
    }
    else {
      System.out.println("FAIL: " + qid + " " + mid);
      return null;
    }
  }

  void deleteMessage(long qid, long mid)
  {
    _deleteMessage.exec(null, qid, mid);
  }

  @SuppressWarnings("serial")
  public static class BrokerQueue implements Serializable {
    private String _name;
    private long _qid;
    
    public BrokerQueue()
    {
    }
    
    public BrokerQueue(String name, long qid)
    {
      _name = name;
      _qid = qid;
    }
    
    public String getName()
    {
      return _name;
    }
    
    public long getId()
    {
      return _qid;
    }
    
    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "," + Long.toHexString(_qid) + "]";
    }
  }
}
