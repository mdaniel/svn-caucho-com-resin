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

import com.caucho.quercus.env.Value;

import com.caucho.util.L10N;

import java.util.logging.Logger;


/**
 * Quercus Oracle OCI-Lob object oriented API.
 */
public class OracleOciLob extends Value {
  private static final Logger log = Logger.getLogger(OracleOciLob.class.getName());
  private static final L10N L = new L10N(OracleOciLob.class);

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
