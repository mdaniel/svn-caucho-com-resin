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

import com.caucho.quercus.env.Value;

import java.sql.Types;

public enum ColumnType {
  BLOB(Types.BLOB),
  STRING(Types.VARCHAR),
  LONG(Types.INTEGER),
  DOUBLE(Types.DOUBLE),
  BOOLEAN(Types.BOOLEAN),
  NULL(Types.NULL),
  LOB(Types.LONGVARCHAR);

  private final int _type;

  private ColumnType(int type)
  {
    _type = type;
  }

  public static ColumnType getColumnType(Value value)
  {
    if (value.isString()) {
      return STRING;
    }
    else if (value.isBoolean()) {
      return BOOLEAN;
    }
    else if (value.isLong()) {
      return LONG;
    }
    else if (value.isDouble()) {
      return DOUBLE;
    }
    else if (value.isNull()) {
      return NULL;
    }
    else {
      return STRING;
    }
  }

  public int getType()
  {
    return _type;
  }
}
