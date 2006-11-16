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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.function;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.management.ObjectName;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Code for marshaling (PHP to Java) and unmarshaling (Java to PHP) arguments.
 */
abstract public class Marshal {
  protected static final L10N L = new L10N(Marshal.class);

  /**
   * Returns true if the result is a primitive boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true if the result is a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true if the result is a long.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true if the result is a double.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Return true for read-only.
   */
  public boolean isReadOnly()
  {
    return true;
  }

  /**
   * Return true for a reference
   */
  public boolean isReference()
  {
    return false;
  }

  abstract public Object marshal(Env env, Expr expr, Class argClass);

  public Object marshal(Env env, Value value, Class argClass)
  {
    return value;
  }

  public Value unmarshal(Env env, Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}

