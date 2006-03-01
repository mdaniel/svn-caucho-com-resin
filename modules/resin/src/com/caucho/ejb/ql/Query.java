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

package com.caucho.ejb.ql;

import com.caucho.bytecode.JMethod;

import com.caucho.util.IntArray;

import com.caucho.config.ConfigException;

import com.caucho.ejb.cfg.EjbConfig;

/**
 * A select/find query
 */
class Query {
  final static int IDENTIFIER = 128;
  final static int INTEGER = IDENTIFIER + 1;
  final static int LONG = INTEGER + 1;
  final static int DOUBLE = LONG + 1;
  final static int STRING = DOUBLE + 1;
  final static int TRUE = STRING + 1;
  final static int FALSE = TRUE + 1;
  final static int UNKNOWN = FALSE + 1;
  final static int MEMBER = UNKNOWN + 1;
  final static int OF = MEMBER + 1;
  final static int EMPTY = OF + 1;
  final static int NULL = EMPTY + 1;
  
  final static int FROM = NULL + 1;
  final static int IN = FROM + 1;
  final static int SELECT = IN + 1;
  final static int DISTINCT = SELECT + 1;
  final static int WHERE = SELECT + 1;
  final static int AS = WHERE + 1;
  final static int ORDER = AS + 1;
  final static int BY = ORDER + 1;
  final static int ASC = BY + 1;
  final static int DESC = ASC + 1;
  final static int LIMIT = DESC + 1;
  final static int OFFSET = LIMIT + 1;
  
  final static int BETWEEN = OFFSET + 1;
  final static int LIKE = BETWEEN + 1;
  final static int ESCAPE = LIKE + 1;
  final static int IS = ESCAPE + 1;
  
  final static int EQ = IS + 1;
  final static int NE = EQ + 1;
  final static int LT = NE + 1;
  final static int LE = LT + 1;
  final static int GT = LE + 1;
  final static int GE = GT + 1;
  
  final static int AND = GE + 1;
  final static int OR = AND + 1;
  final static int NOT = OR + 1;

  final static int EXTERNAL_DOT = NOT + 1;
  
  final static int ARG = EXTERNAL_DOT + 1;
  final static int THIS = ARG + 1;

  private JMethod _method;
  private EjbConfig _config;

  private IntArray _argSize = new IntArray();

  JMethod getMethod()
  {
    return _method;
  }

  void setMethod(JMethod method)
  {
    _method = method;
  }

  void setConfig(EjbConfig config)
  {
    _config = config;
  }

  EjbConfig getConfig()
  {
    return _config;
  }

  /**
   * Sets the number of sub-arguments for an arg.
   */
  public void setArgSize(int index, int size)
  {
    while (_argSize.size() < index) {
      _argSize.add(0);
    }

    _argSize.set(index - 1, size);
  }

  /**
   * Sets the number of sub-arguments for an arg.
   */
  public int getArgIndex(int index)
  {
    int size = 1;
    
    for (int i = 0; i < index - 1; i++) {
      size += _argSize.get(i);
    }

    return size;
  }
  
  /**
   * Creates an error.
   */
  public ConfigException error(String msg)
  {
    return new ConfigException(msg);
    /*
    msg += "\nin \"" + _query + "\"";
    if (_qlConfig != null)
      return new SelectLineParseException(_qlConfig.getFilename() + ":" +
					  _qlConfig.getLine() + ": " +
					  msg);
    else if (_location != null)
      return new SelectLineParseException(_location + msg);
    else
      return new SelectParseException(msg);
    */
  }
}
