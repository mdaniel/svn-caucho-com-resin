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
 * Represents a set collection
 * 
 * SetData := '{' DataList '}'
 * DataList := (expression (',' expression)* )?
 * 
 */
public class SetExpr extends Expr 
{
  private final Expr []_exprs;
  
  public SetExpr(List<Expr> exprs)
  {
    _exprs = new Expr[exprs.size()];
    exprs.toArray(_exprs);
  }

  public SetExpr(Expr... expr)
  {
    _exprs = expr;
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {
    HashSet<Object> set = new LinkedHashSet<>();
    
    for (Expr expr : _exprs) {
      set.add(expr.getValue(env));
    }

    return set;
  }

  @Override
  public boolean isConstant()
  {
    for (Expr expr : _exprs) {
      if (! expr.isConstant()) {
        return false;
      }
    }
    
    return true;
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof ListExpr)) {
      return false;
    }

    SetExpr other = (SetExpr) o;
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
    os.print("new com.caucho.v5.el.SetExpr(");
    
    boolean isFirst = true;
    for (Expr expr : _exprs) {
      if (! isFirst)
        os.print(", ");
      expr.printCreate(os);
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
    for (Expr expr : _exprs) {
      if (! isFirst)
        sb.append(", ");
      sb.append(expr);
      isFirst = false;
    }
    
    sb.append("}");
    return sb.toString();
  }
  
}
