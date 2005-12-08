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

import com.caucho.quercus.resources.JdbcConnectionResource;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.Optional;

/**
 * mysqli object oriented API facade
 */
public class Mysqli {
  private JdbcConnectionResource _conn;
  
  public Mysqli()
  {
  }

  /**
   * returns JdbcResultResource representing the results of the query.
   *
   * <i>resultMode</i> is ignored, MYSQLI_USE_RESULT would represent
   * an unbuffered query, but that is not supported.
   */
  public Value query(String sql,
    		     @Optional("MYSQLI_STORE_RESULT") int resultMode)
  {
    return QuercusMysqliModule.mysqli_query(_conn, sql, resultMode);
  }

  /**
   * Closes the connection.
   */
  public boolean close(Env env)
  {
    return QuercusMysqliModule.mysqli_close(env, _conn);
  }
}
