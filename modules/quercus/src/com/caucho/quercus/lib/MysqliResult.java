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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import java.sql.SQLException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.resources.JdbcConnectionResource;
import com.caucho.quercus.resources.JdbcResultResource;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;

/**
 * mysqli object oriented API facade
 */
public class MysqliResult {
  private static final Logger log
    = Logger.getLogger(MysqliResult.class.getName());
  private static final L10N L = new L10N(MysqliResult.class);

  private JdbcResultResource _rs;
  
  MysqliResult(JdbcResultResource rs)
  {
    _rs = rs;
  }

  /**
   * seeks to an arbitrary result pointer specified
   * by the offset in the result set represented by result.
   * Returns TRUE on success or FALSE on failure
   */
  public boolean data_seek(Env env, int rowNumber)
  {
    if (validateResult().setRowNumber(rowNumber))
      return true;
    else {
      env.warning(L.l("Offset {0} is invalid for MySQL (or the query data is unbuffered)", rowNumber));
      
      return false;
    }
  }

  /**
   * Returns an array representing the row.
   */
  public Value fetch_array(@Optional("MYSQLI_BOTH") int type)
    throws Exception
  {
    return validateResult().fetchArray(type);
  }

  /**
   * Returns an associative array representing the row.
   */
  public Value fetch_assoc()
    throws Exception
  {
    return validateResult().fetchArray(JdbcResultResource.ASSOC);
  }

  /**
   * Returns the field metadata
   */
  public Value fetch_field(Env env)
    throws Exception
  {
    return validateResult().fetchNextField(env);
  }

  /**
   * Returns the field metadata
   */
  public Value fetch_fields(Env env)
    throws Exception
  {
    return validateResult().getFieldDirectArray(env);
  }

  /**
   * Returns the field metadata
   */
  public Value fetch_field_direct(Env env, int offset)
    throws Exception
  {
    return validateResult().fetchFieldDirect(env, offset);
  }

  /**
   * Returns an object representing the row.
   */
  public Value fetch_object(Env env)
    throws Exception
  {
    return validateResult().fetchObject(env);
  }

  /**
   * Returns an object representing the row.
   */
  public Value fetch_row()
    throws Exception
  {
    return validateResult().fetchArray(JdbcResultResource.NUM);
  }

  /**
   * Returns the number of fields
   */
  public int field_count(Env env)
    throws Exception
  {
    return validateResult().getFieldCount();
  }

  /**
   * Sets the index into field metadata
   */
  public boolean field_seek(Env env, int offset)
    throws Exception
  {
    validateResult().setFieldOffset(offset);

    return true;
  }

  /**
   * Returns the index into field metadata
   */
  public int field_tell(Env env)
    throws Exception
  {
    return validateResult().getFieldOffset();
  }

  /**
   * Closes the result
   */
  public void free()
  {
    close();
  }

  /**
   * Closes the result
   */
  public void free_result()
  {
    close();
  }

  /**
   * returns the number of columns in the result set.
   */
  public int num_fields()
  {
    return validateResult().getFieldCount();
  }

  /**
   * returns the number of rows in the result set.
   */
  public Value num_rows()
    throws SQLException
  {
    return validateResult().getNumRows();
  }

  /**
   * Closes the result.
   */
  public void close()
  {
    JdbcResultResource rs = _rs;
    _rs = null;

    if (rs != null)
      rs.close();
  }

  private JdbcResultResource validateResult()
  {
    return _rs;
  }
  
  public String toString()
  {
    if (_rs != null)
      return "MysqliResult[" + _rs + "]";
    else
      return "MysqliResult[closed]";
  }
}
