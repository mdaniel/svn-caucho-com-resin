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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.db;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

import com.caucho.util.L10N;

import java.io.Reader;
import java.io.Writer;

import java.sql.Clob;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Quercus Oracle OCI-Lob object oriented API.
 */
public class OracleOciLob {
  private static final Logger log = Logger.getLogger(OracleOciLob.class.getName());
  private static final L10N L = new L10N(OracleOciLob.class);

  private Object _lob;
  private int _type;


  /**
   * Constructor for OracleOciLob
   *
   * @param type one of the following types:
   *
   * OCI_D_FILE - a FILE descriptor
   *
   * OCI_D_LOB - a LOB descriptor
   *
   * OCI_D_ROWID - a ROWID descriptor
   */
  OracleOciLob(int type)
  {
    _type = type;
    _lob = null;
  }

  /**
   * Frees resources associated with the LOB descriptor
   */
  public boolean free(Env env)
  {
    try {

      _lob = null;

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  /**
   * Returns large object's contents
   */
  @ReturnNullAsFalse
  public String load(Env env)
  {
    try {

      StringBuilder contents = new StringBuilder();

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          Reader reader = clob.getCharacterStream();
          int nchars;
          char buffer[] = new char[10];
          while( (nchars = reader.read(buffer)) != -1 )
            contents.append(buffer, 0, nchars);
          reader.close();
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      return contents.toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }

  /**
   * Saves data to the large object
   */
  public boolean save(Env env,
                      @NotNull String data,
                      @Optional("0") int offset)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          Writer writer = clob.setCharacterStream(offset);
          writer.write(data);
          writer.close();
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

      return true;

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return false;
    }
  }

  protected void setLob(Object lob) {
    _lob = lob;
  }

  public String toString() {

    String typeName = "UNKNOWN";

    switch (_type) {
    case OracleModule.OCI_D_FILE:
      typeName = "OCI_D_FILE";
      break;
    case OracleModule.OCI_D_LOB:
      typeName = "OCI_D_LOB";
      break;
    case OracleModule.OCI_D_ROWID:
      typeName = "OCI_D_ROWID";
      break;
    }

    return "OracleOciLob("+typeName+")";
  }
}
