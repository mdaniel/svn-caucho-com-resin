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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.ConnectionEntry;
import com.caucho.quercus.env.Env;

import java.sql.SQLException;

import javax.sql.DataSource;

public class DataSourceConnection extends JdbcConnectionResource
{
  private final ConnectionEntry _conn;

  public DataSourceConnection(Env env, DataSource ds)
  {
    this(env, ds, null, null);
  }

  public DataSourceConnection(Env env, DataSource ds,
                              String user, String pass)
  {
    super(env);

    _conn = new ConnectionEntry(env);
    _conn.init(ds, user, pass);

    connectInternal(env, null, user, pass, null, -1, null, 0, null, null, false);
  }

  protected ConnectionEntry connectImpl(Env env,
                                        String host,
                                        String userName,
                                        String password,
                                        String dbname,
                                        int port,
                                        String socket,
                                        int flags,
                                        String driver,
                                        String url,
                                        boolean isNewLink)
  {
    try {
      _conn.connect(! isNewLink);

      return _conn;
    }
    catch (SQLException e) {
      env.warning(e);

      return null;
    }
  }
}
