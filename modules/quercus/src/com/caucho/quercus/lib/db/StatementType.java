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

public enum StatementType
{
  SELECT, UPDATE, DELETE, INSERT, CREATE, DROP,
  ALTER, BEGIN, DECLARE, ROLLBACK, SET, UNKNOWN;

  public static StatementType getStatementType(String query)
  {
    int len = query.length();
    int i = 0;

    while (i < len && Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    if (i + 1 >= len) {
      return UNKNOWN;
    }

    int start = i;

    while (i < len && ! Character.isWhitespace(query.charAt(i))) {
      i++;
    }

    String token = query.substring(start, i);

    if ("SELECT".equalsIgnoreCase(token)) {
      return SELECT;
    }
    else if ("UPDATE".equalsIgnoreCase(token)) {
      return UPDATE;
    }
    else if ("DELETE".equalsIgnoreCase(token)) {
      return DELETE;
    }
    else if ("INSERT".equalsIgnoreCase(token)) {
      return INSERT;
    }
    else if ("CREATE".equalsIgnoreCase(token)) {
      return CREATE;
    }
    else if ("DROP".equalsIgnoreCase(token)) {
      return DROP;
    }
    else if ("ALTER".equalsIgnoreCase(token)) {
      return ALTER;
    }
    else if ("BEGIN".equalsIgnoreCase(token)) {
      return BEGIN;
    }
    else if ("DECLARE".equalsIgnoreCase(token)) {
      return DECLARE;
    }
    else if ("ROLLBACK".equalsIgnoreCase(token)) {
      return ROLLBACK;
    }
    else if ("SET".equalsIgnoreCase(token)) {
      return SET;
    }
    else {
      return UNKNOWN;
    }
  }
}
