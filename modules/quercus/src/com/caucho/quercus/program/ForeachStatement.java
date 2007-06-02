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

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.BreakValue;
import com.caucho.quercus.env.ContinueValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.AbstractVarExpr;
import com.caucho.quercus.expr.Expr;

/**
 * Represents a foreach statement.
 */
public class ForeachStatement
  extends Statement
{
  protected final Expr _objExpr;

  protected final AbstractVarExpr _key;

  protected final AbstractVarExpr _value;
  protected final boolean _isRef;

  protected final Statement _block;

  public ForeachStatement(Location location,
                          Expr objExpr,
                          AbstractVarExpr key,
                          AbstractVarExpr value,
                          boolean isRef,
                          Statement block)
  {
    super(location);

    _objExpr = objExpr;

    _key = key;
    _value = value;
    _isRef = isRef;

    _block = block;
  }

  public Value execute(Env env)
  {
    Value origObj = _objExpr.eval(env);
    Value obj = origObj.copy();

    if (_key == null && ! _isRef) {
      
      for (Value value : obj.getValueArray(env)) {
        _value.evalAssign(env, value);

        Value result = _block.execute(env);

        if (result == null || result instanceof ContinueValue) {
        }
        else if (result instanceof BreakValue)
          return null;
        else
          return result;
      }

      return null;
    } else {
      for (Value key : obj.getKeyArray(env)) {
	if (_key != null)
	  _key.evalAssign(env, key);

	if (_isRef) {
	  Value value = origObj.getRef(key);

	  _value.evalAssign(env, value);
	} else {
	  Value value = obj.get(key).toValue();

	  _value.evalAssign(env, value);
	}

	Value result = _block.execute(env);

	if (result == null || result instanceof ContinueValue) {
	} else if (result instanceof BreakValue)
	  return null;
	else
	  return result;
      }
    }

    return null;
  }
}

