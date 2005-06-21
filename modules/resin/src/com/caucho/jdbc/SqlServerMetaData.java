/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.jdbc;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.caucho.util.Log;

/**
 * Metadata for the microsoft SQL server database.
 */
public class SqlServerMetaData extends JdbcMetaData {
  private static final Logger log = Log.open(SqlServerMetaData.class);

  protected SqlServerMetaData(DataSource ds)
  {
    super(ds);
  }

  /**
   * Returns true if identity is supported.
   */
  public boolean supportsIdentity()
  {
    return true;
  }

    /**
   * New version to Return SQL for the table with the given
   * SQL type.  Takes, length, precision and scale.
   */
  public String getCreateTableSQL(int sqlType, int length, int precision, int scale) {
       String type = null;

    switch (sqlType) {
    case Types.BOOLEAN:
        type = getCreateColumnSQL(Types.BIT, length, precision, scale);
      break;

    case Types.TINYINT:
        type = getCreateColumnSQL(Types.TINYINT, length, precision, scale);
      break;

    case Types.DATE:
      type = getCreateColumnSQL(sqlType, length, precision, scale);
      if (type == null)
	type = getCreateColumnSQL(Types.TIMESTAMP, length, precision, scale);
      break;

    case Types.TIME:
      type = getCreateColumnSQL(sqlType, length, precision, scale);
      if (type == null)
	type = getCreateColumnSQL(Types.TIMESTAMP, length, precision, scale);
      break;

    case Types.DOUBLE:
      type = getCreateColumnSQL(Types.FLOAT, length, precision, scale);
      break;

    case Types.NUMERIC:
        type = getCreateColumnSQL(Types.NUMERIC, length, precision, scale);
        break;

    default:
      type = getCreateColumnSQL(sqlType, length, precision, scale);
      break;
    }

    if (type == null)
      type = getDefaultCreateTableSQL(sqlType, length, precision, scale);

    return type;
  }

  /**
   * Returns the identity property
   */
  public String createIdentitySQL(String sqlType)
  {
    return " uniqueidentifier NOT NULL DEFAULT(NEWID())";
  }

  public String generateBoolean(String term)
  {
    return term + "= 1";
  }
}
