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
import java.util.IdentityHashMap;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.expr.Expr;

/**
 * Represents an array-get argument which might be a call to a reference
 * and which might automatically create an object.
 *
 * e.g. foo($a[0]->x) where foo($a) or foo(&amp;$a)
 */
public class ArgGetObjectValue extends Value {
  private final Env _env;
  private final Value _obj;
  private final Value _index;

  public ArgGetObjectValue(Env env, Value obj, Value index)
  {
    _env = env;
    _obj = obj;
    _index = index;
  }

  /**
   * Returns the value for a get arg.
   */
  public Value getArg(Value index)
  {
    return new ArgGetObjectValue(_env, this, index);
  }

  // XXX: getArgArray
  // XXX: getArgObject

  /**
   * Returns the value, converting to an object if necessary.
   */
  public Value getArgRef(Value index)
  {
     return _obj.getObject(_env, _index).getArgRef(index);
  }

  /**
   * Returns the value, converting to an object if necessary.
   */
  public Value getFieldArg(Env env, String index)
  {
    // php/3d2t
    return new ArgGetFieldValue(_env, this, index);
  }

  // XXX: getFieldArgArray
  // XXX: getFieldArgObject

  /**
   * Converts to a ref var.
   */
  public Var toRefVar()
  {
     return _obj.getArgRef(_index).toRefVar();
  }

  /**
   * Converts to a var.
   */
  /*
  public Var toVar()
  {
    System.out.println("TO_VAR:");
    // quercus/3d52
    return _obj.get(_index).toVar();
  }
  */

  /**
   * Returns the field reference of the value.
   */
  public Value getFieldRef(String index)
  {
    // php/3d2t
    return _obj.getObject(_env, _index).getFieldRef(_env, index);
  }

  /**
   * Returns the value, converting to an object if necessary.
   */
  /*
  public Value getObject(Env env, Value index)
  {
    return _obj.getObject(_env, _index).getObject(env, index);
  }
  */
}

