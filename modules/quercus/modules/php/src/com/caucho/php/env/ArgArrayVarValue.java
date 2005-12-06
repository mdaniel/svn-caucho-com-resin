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

package com.caucho.php.env;

import java.io.IOException;

import java.util.Collection;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.php.PhpRuntimeException;

import com.caucho.php.program.AbstractFunction;

import com.caucho.php.gen.PhpWriter;

import com.caucho.php.expr.Expr;

/**
 * Represents an array argument which might be a call to a reference.
 */
public class ArgArrayVarValue extends Value {
  private final Var _var;

  public ArgArrayVarValue(Var var)
  {
    _var = var;
  }

  /**
   * Returns the wrapper for the get arg array.
   */
  public Value getArgArray(Value index)
  {
    // php/3d1r
    return new ArgArrayGetValue(this, index);
  }

  /**
   * Returns the wrapper for the get arg object.
   */
  public Value getArgObject(Env env, Value index)
  {
    // php/3d2t
    return new ArgObjectGetValue(env, this, index);
  }

  /**
   * Returns the value, converting to an array if needed.
   */
  public Value getArray(Value index)
  {
    // php/3d1t
    return _var.getArray().getArray(index);
  }

  /**
   * Returns the value, converting to an object if needed.
   */
  public Value getObject(Env env, Value index)
  {
    // php/3d2t
    return _var.getArray().getObject(env, index);
  }
}

