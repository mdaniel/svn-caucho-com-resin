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

import javax.el.*;

import com.caucho.v5.vfs.WriteStream;

public class MapEntryExpr extends Expr 
{
  private final Expr _left;
  private final Expr _right;

  /**
   * Creates the identifier
   */
  public MapEntryExpr(Expr left, Expr right)
  {
    _left = left;
    _right = right;
  }
  
  public Expr getLeft()
  {
    return _left;
  }
  
  public Object getLeftValue(ELContext env) throws ELException
  {
    return _left.getValue(env);
  }
  
  public Expr getRight()
  {
    return _right;
  }
  
  public Object getRightValue(ELContext env) throws ELException
  {
    return getValue(env);
  }
  
  @Override
  public Object getValue(ELContext env) throws ELException
  {
    return _right.getValue(env);
  }

  @Override
  public boolean isReadOnly(ELContext env)
  {
    return _left.isReadOnly(env) && _right.isReadOnly(env);
  }

  @Override
  public boolean isConstant()
  {
    return _left.isConstant() && _right.isConstant();
  }

  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.v5.el.MapEntryExpr(");
    _left.printCreate(os);
    os.print(", ");
    _right.printCreate(os);
    os.print(")");
  }

  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof MapEntryExpr))
      return false;

    MapEntryExpr expr = (MapEntryExpr) o;

    return (_left.equals(expr._left) &&
            _right.equals(expr._right));
  }
  
  @Override
  public String toString()
  {
    return String.format("%s:%s", _left, _right);
  }
}
