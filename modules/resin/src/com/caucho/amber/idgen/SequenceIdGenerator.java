/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.amber.idgen;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.config.ConfigException;

import com.caucho.jdbc.JdbcMetaData;

import com.caucho.amber.AmberManager;

import com.caucho.amber.connection.AmberConnectionImpl;

/**
 * Generator table.
 */
public class SequenceIdGenerator extends IdGenerator {
  private static final L10N L = new L10N(SequenceIdGenerator.class);
  private static final Logger log = Log.open(SequenceIdGenerator.class);
  
  private AmberManager _manager;
  private String _name;
  private int _size;

  private String _selectSQL;

  private boolean _isInit;
  
  /**
   * Creates the table generator.
   */
  public SequenceIdGenerator(AmberManager manager,
			     String name,
			     int size)
    throws ConfigException
  {
    _manager = manager;
    _name = name;
    _size = size;
  }

  /**
   * Allocates the next group of ids.
   */
  public long allocateGroup(AmberConnectionImpl aConn)
    throws SQLException
  {
    // XXX: should use non-XA
    Connection conn = aConn.getConnection();
    PreparedStatement selectStmt = conn.prepareStatement(_selectSQL);

    long value = -1;

    ResultSet rs = selectStmt.executeQuery();
    if (rs.next())
      value = rs.getLong(1);
    
    rs.close();

    return value;
  }

  /**
   * Initialize the table.
   */
  public void init(AmberManager amberManager)
    throws SQLException
  {
    if (_isInit)
      return;
    _isInit = true;

    DataSource ds = amberManager.getDataSource();
    Connection conn = ds.getConnection();
    try {
      JdbcMetaData metaData = amberManager.getMetaData();

      _selectSQL = metaData.selectSequenceSQL(_name);

      if (amberManager.getCreateDatabaseTables()) {
	String sql = metaData.createSequenceSQL(_name, getGroupSize());

	try {
	  Statement stmt = conn.createStatement();
	  stmt.executeUpdate(sql);
	  stmt.close();
	} catch (Exception e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }
    } finally {
      conn.close();
    }
  }
}
