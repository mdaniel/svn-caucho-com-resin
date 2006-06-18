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
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;

import com.caucho.quercus.UnimplementedException;

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
   * Appends data from the large object to another large object
   */
  public boolean append(Env env,
                        OracleOciLob lobFrom)
  {
    throw new UnimplementedException("append");
  }

  /**
   * Closes LOB descriptor
   */
  public boolean close(Env env)
  {
    throw new UnimplementedException("close");
  }

  /**
   * Tests for end-of-file on a large object's descriptor
   */
  public boolean eof(Env env)
  {
    throw new UnimplementedException("eof");
  }

  /**
   * Erases a specified portion of the internal LOB data
   *
   * @return the actual number of characters/bytes erased or
   * FALSE in case of error.
   */
  @ReturnNullAsFalse
  public LongValue erase(Env env,
                         @Optional("0") int offset,
                         @Optional("-1") int length)
  {
    throw new UnimplementedException("erase");
  }

  /**
   * Exports LOB's contents to a file
   */
  public boolean export(Env env,
                        String fileName,
                        @Optional("0") int start,
                        @Optional("-1") int length)
  {
    throw new UnimplementedException("export");
  }

  /**
   * Flushes/writes buffer of the LOB to the server
   */
  public boolean flush(Env env,
                       @Optional("-1") int flag)
  {
    throw new UnimplementedException("flush");
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
   * Returns current state of buffering for the large object
   */
  public boolean getBuffering(Env env)
  {
    throw new UnimplementedException("getBuffering");
  }

  /*
   * Imports file data to the LOB
   *
  public boolean import(Env env,
                        String fileName)
  {
    throw new UnimplementedException("import");
    }*/

  /**
   * Returns large object's contents
   */
  @ReturnNullAsFalse
  public String load(Env env)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Clob) {
          return readInternalClob(env, -1);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Reads part of the large object
   */
  @ReturnNullAsFalse
  public String read(Env env,
                     int length)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Clob) {
          return readInternalClob(env, length);
        }
        break;
      case OracleModule.OCI_D_ROWID:
        break;
      }

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
    }

    return null;
  }

  /**
   * Moves the internal pointer to the beginning of the large object
   */
  public boolean rewind(Env env)
  {
    throw new UnimplementedException("rewind");
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

  /**
   * Alias of import()
   */
  public boolean saveFile(Env env,
                          String fileName)
  {
    // return import(env, fileName);
    throw new UnimplementedException("saveFile");
  }

  /**
   * Sets the internal pointer of the large object
   */
  public boolean seek(Env env,
                      int offset,
                      @Optional("-1") int whence)
  {
    throw new UnimplementedException("seek");
  }

  /**
   * Changes current state of buffering for the large object
   */
  public boolean setBuffering(Env env,
                              boolean onOff)
  {
    throw new UnimplementedException("setBuffering");
  }


  /**
   * Sets the underlying LOB
   */
  protected void setLob(Object lob) {
    _lob = lob;
  }

  /**
   * Returns size of large object
   */
  @ReturnNullAsFalse
  public LongValue size(Env env)
  {
    throw new UnimplementedException("size");
  }

  /**
   * Returns current position of internal pointer of large object
   */
  @ReturnNullAsFalse
  public LongValue tell(Env env)
  {
    throw new UnimplementedException("tell");
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

  /**
   * Truncates large object
   */
  public boolean truncate(Env env,
                          @Optional("0") int length)
  {
    try {

      switch (_type) {
      case OracleModule.OCI_D_FILE:
        break;
      case OracleModule.OCI_D_LOB:
        if (_lob instanceof Clob) {
          Clob clob = (Clob) _lob;
          clob.truncate(length);
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

  /**
   * Writes data to the large object
   */
  @ReturnNullAsFalse
  public LongValue write(Env env,
                         String data,
                         @Optional("-1") int length)
  {
    throw new UnimplementedException("write");
  }

  /**
   * Writes temporary large object
   */
  public boolean writeTemporary(Env env,
                                String data,
                                @Optional("-1") int lobType)
  {
    throw new UnimplementedException("writeTemporary");
  }

  /**
   * Alias of export()
   */
  public boolean writeToFile(Env env,
                             String fileName,
                             @Optional("0") int start,
                             @Optional("-1") int length)
  {
    return export(env, fileName, start, length);
  }

  private String readInternalClob(Env env,
                                  int length)
  {
    try {

      StringBuilder contents = new StringBuilder();

      Clob clob = (Clob) _lob;
      Reader reader = clob.getCharacterStream();

      int remaining = length < 0 ? Integer.MAX_VALUE : length;

      int nchars;
      char buffer[] = new char[10];
      while( (remaining > 0) &&
             ((nchars = reader.read(buffer)) != -1) ) {
        if (nchars > remaining)
          nchars = remaining;
        contents.append(buffer, 0, nchars);
        remaining -= nchars;
      }

      reader.close();

      return contents.toString();

    } catch (Exception ex) {
      log.log(Level.FINE, ex.toString(), ex);
      return null;
    }
  }
}
