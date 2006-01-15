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

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AbstractClassDef;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP class.
 */
abstract public class AbstractQuercusClass {
  private static final L10N L = new L10N(AbstractQuercusClass.class);
  
  /**
   * Returns the name.
   */
  abstract public String getName();
  
  /**
   * Creates a new instance.
   */
  abstract public Value evalNew(Env env, Expr []expr)
    throws Throwable;

  /**
   * Creates a new instance.
   */
  abstract public Value evalNew(Env env, Value []args)
    throws Throwable;

  /**
   * Finds a function.
   */
  public AbstractFunction findFunction(String name)
  {
    return null;
  }

  /**
   * Finds a function.
   */
  public AbstractFunction findFunctionLowerCase(String name)
  {
    return null;
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(String name)
  {
    AbstractFunction fun = findFunction(name);

    if (fun != null)
      return fun;

    fun = findFunctionLowerCase(name.toLowerCase());
    
    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), name));
    }
  }

  /**
   * Finds the matching constant
   */
  public Value findConstant(Env env, String name)
  {
    return null;
  }

  /**
   * Finds the matching constant
   */
  public final Value getConstant(Env env, String name)
    throws Throwable
  {
    Value value = findConstant(env, name);

    if (value != null)
      return value;

    throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown constant",
					getName(), name));
  }

  /**
   * Returns the parent class
   */
  public String getParentName()
  {
    return null;
  }

  /**
   * Returns value for instanceof.
   */
  public boolean isA(String name)
  {
    return false;
  }
}

