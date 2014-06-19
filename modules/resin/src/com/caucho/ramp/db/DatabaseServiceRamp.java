/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.ramp.db;

import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.db.Cursor;
import io.baratine.db.DatabaseService;
import io.baratine.db.DatabaseWatch;

import com.caucho.kraken.KrakenSystem;
import com.caucho.kraken.table.TableManagerKraken;

/*
 * Entry to the store.
 */
@Service
public class DatabaseServiceRamp
{
  private DatabaseService _db;
  private TableManagerKraken _tableManager;
  
  public DatabaseServiceRamp()
  {
    KrakenSystem krakenSystem = KrakenSystem.getCurrent();
    
    TableManagerKraken kraken = krakenSystem.getTableManager();
    
    _db = kraken.getDatabaseServiceRef()
                .service(this)
                .as(DatabaseService.class);
    
    _tableManager = kraken;
  }
  
  public DatabaseServiceRamp(String name, String hostName)
  {
    this();
  }
  
  public DatabaseService getDatabaseService()
  {
    return _db;
  }
  
  public void exec(String sql, Result<Object> result, Object []args)
  {
    _tableManager.exec(sql, args, result);
  }
  
  public void findOne(String sql, Result<Cursor> result, Object []args)
  {
    _tableManager.findOne(sql, args, result);
  }
  
  public void findAll(String sql, Result<Iterable<Cursor>> result, Object []args)
  {
    _tableManager.findAll(sql, args, result);
  }
  
  public void addTableWatch(String tableName, @Service DatabaseWatch watch)
  {
    _tableManager.addWatch(tableName, watch);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
