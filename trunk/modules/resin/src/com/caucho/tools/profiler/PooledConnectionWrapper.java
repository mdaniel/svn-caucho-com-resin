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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.tools.profiler;

import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.StatementEventListener;

public final class PooledConnectionWrapper
  implements PooledConnection
{
  private final PooledConnection _connection;
  private final ProfilerPoint _profilerPoint;

  public PooledConnectionWrapper(ProfilerPoint profilerPoint,
                                 PooledConnection connection)
  {
    _profilerPoint = profilerPoint;
    _connection = connection;
  }

  private Connection wrap(Connection connection)
  {
    return new ConnectionWrapper(_profilerPoint, connection);
  }

  public Connection getConnection()
    throws SQLException
  {
    return wrap(_connection.getConnection());
  }

  public void close()
    throws SQLException
  {
    _connection.close();
  }

  public void addConnectionEventListener(ConnectionEventListener listener)
  {
    _connection.addConnectionEventListener(listener);
  }

  public void removeConnectionEventListener(ConnectionEventListener listener)
  {
    _connection.removeConnectionEventListener(listener);
  }

  public String toString()
  {
    return "PooledConnectionWrapper[" + _profilerPoint.getName() + "]";
  }

    public void addStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}


