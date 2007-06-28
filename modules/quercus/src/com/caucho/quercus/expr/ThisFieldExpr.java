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

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.util.L10N;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExpr extends AbstractVarExpr {
  private static final L10N L = new L10N(ThisFieldExpr.class);

  protected final InterpretedClassDef _quercusClass;
  
  protected final String _name;

  public ThisFieldExpr(Location location, InterpretedClassDef quercusClass, String name)
  {
    super(location);
    _quercusClass = quercusClass;
    
    _name = name.intern();
  }

  public ThisFieldExpr(InterpretedClassDef quercusClass, String name)
  {
    _quercusClass = quercusClass;
    
    _name = name.intern();
  }

  private Value cannotUseThisError(Env env)
  {
    return env.error(getLocation(),
                     "Cannot use '$this' when not in object context.");
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getField(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getField(env, _name).copy();
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getFieldRef(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArg(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getFieldArg(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalAssign(Env env, Value value)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    obj.putField(env, _name, value);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalArray(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getFieldArray(env, _name);
  }

  /**
   * Evaluates the expression, creating an array if the value is unset..
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalObject(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      return cannotUseThisError(env);
    
    return obj.getFieldObject(env, _name);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public void evalUnset(Env env)
  {
    Value obj = env.getThis();

    if (obj.isNull())
      cannotUseThisError(env);
    
    obj.removeField(_name);
  }
  
  public String toString()
  {
    return "$this->" + _name;
  }
}

