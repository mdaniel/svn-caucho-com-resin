/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import java.io.IOException;

import java.util.Collection;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.expr.Expr;

/**
 * Represents an array-get argument which might be a call to a reference.
 */
public class ArgArrayGetValue extends Value {
  private final Value _obj;
  private final Value _index;

  public ArgArrayGetValue(Value obj, Value index)
  {
    _obj = obj;
    _index = index;
  }

  /**
   * Returns the wrapper for the get arg array.
   */
  public Value getArgArray(Value index)
  {
    return new ArgArrayGetValue(this, index);
  }

  /**
   * Returns the wrapper for the get arg array.
   */
  public Value getArg(Value index)
  {
    // quercus/3d1p
    return new ArgArrayGetValue(this, index);
  }

  /**
   * Returns the reference, creaing the array if necessary.
   */
  public Value getArgRef(Value index)
  {
    return _obj.getArray(_index).getArgRef(index);
  }

  /**
   * Converts to a ref var.
   */
  public Var toRefVar()
  {
    return _obj.getArgRef(_index).toRefVar();
  }

  /**
   * Converts to a function variable.
   */
  public Var toVar()
  {
    // quercus/3d54
    return _obj.get(_index).toVar();
  }
}

