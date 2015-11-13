/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.el;

import java.io.IOException;
import java.util.*;

import javax.el.*;

import com.caucho.v5.vfs.WriteStream;

/**
 * Represents a map collection
 * 
 * Map := '{' MapEntries '}'
 * MapEntries := (MapEntry (',' MapEntry)* )?
 * MapEntry := expression ':' expression
 * 
 */
public class MapExpr extends Expr 
{
  protected final MapEntryExpr []_exprs;
  
  public MapExpr(List<MapEntryExpr> exprs)
  {
    _exprs = new MapEntryExpr[exprs.size()];
    exprs.toArray(_exprs);
  }

  public MapExpr(MapEntryExpr... entries)
  {
    _exprs = entries;
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {
    Map<Object, Object> map = new LinkedHashMap<>();
    
    for (MapEntryExpr entry : _exprs) {
      map.put(entry.getLeftValue(env), entry.getValue(env));
    }

    //return map.entrySet();
    return map;
  }

  @Override
  public boolean isConstant()
  {
    for (MapEntryExpr entry : _exprs) {
      if (! entry.isConstant()) {
        return false;
      }
    }
    
    return true;
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof MapExpr)) {
      return false;
    }

    MapExpr other = (MapExpr) o;
    
    if (_exprs.length != other._exprs.length) {
      return false;
    }
    
    for (int i = 0; i < _exprs.length; i++) {
      if (! _exprs[i].equals(other._exprs[i])) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.MapExpr(");
    
    boolean isFirst = true;
    for (MapEntryExpr entry : _exprs) {
      if (! isFirst)
        os.print(", ");
      entry.printCreate(os);
      isFirst = false;
    }
    
    os.print(")");
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    
    boolean isFirst = true;
    for (MapEntryExpr entry : _exprs) {
      if (! isFirst)
        sb.append(", ");
      sb.append(entry);
      isFirst = false;
    }
    
    sb.append("}");
    return sb.toString();
  }
}
