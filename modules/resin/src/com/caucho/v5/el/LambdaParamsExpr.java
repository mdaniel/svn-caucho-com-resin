/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Paul Cowan
 */

package com.caucho.v5.el;

import java.io.IOException;
import java.util.*;

import javax.el.*;

import com.caucho.v5.vfs.WriteStream;

public class LambdaParamsExpr extends Expr implements Iterable<String>
{
  private List<String> _paramNames = new ArrayList<>();

  public LambdaParamsExpr()
  {

  }

  public LambdaParamsExpr(List<Expr> exprs)
  {
    for (Expr param : exprs) {
      _paramNames.add(param.getExpressionString());
    }
  }

  public LambdaParamsExpr(Expr... exprs)
  {
    for (Expr param : exprs) {
      _paramNames.add(param.getExpressionString());
    }
  }

  public LambdaParamsExpr(String... params)
  {
    for (String param : params) {
      _paramNames.add(param);
    }
  }

  public List<String> getParamNames()
  {
    return _paramNames;
  }

  public int getSize()
  {
    return _paramNames.size();
  }

  @Override
  public Object getValue(ELContext env) throws ELException
  {
    return getParamNames();
  }

  public String get(int index)
  {
    return _paramNames.get(index);
  }

  @Override
  public boolean isReadOnly(ELContext env)
  {
    return true;
  }

  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.LambdaParamsExpr(\"");

    boolean isFirst = true;
    for (String param : _paramNames) {
      if (! isFirst) {
        os.print(", ");
      }
      
      os.print(param);
      isFirst = false;
    }

    os.print(")");
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof LambdaParamsExpr)) {
      return false;
    }

    LambdaParamsExpr other = (LambdaParamsExpr) o;
    return _paramNames.equals(other._paramNames);
  }

  @Override
  public String toString()
  {
    if (_paramNames.isEmpty()) {
      return "()";
    }

    StringBuilder sb = new StringBuilder();

    if (_paramNames.size() == 1)
      return _paramNames.get(0);

    sb.append("(");

    boolean isFirst = true;
    for (String param : _paramNames) {
      if (! isFirst)
        sb.append(",");
      sb.append(param);
      isFirst = false;
    }

    sb.append(")");
    return sb.toString();
  }

  @Override
  public Iterator<String> iterator()
  {
    return _paramNames.iterator();
  }
}
