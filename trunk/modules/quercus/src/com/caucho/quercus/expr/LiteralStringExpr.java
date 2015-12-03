/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.parser.QuercusParser;

/**
 * Represents a PHP string literal expression.
 */
public class LiteralStringExpr extends Expr {
  protected final StringValue _value;

  public LiteralStringExpr(StringValue value)
  {
    _value = value;
  }

  /**
   * Returns true for a literal expression.
   */
  public boolean isLiteral()
  {
    return true;
  }

  /**
   * Returns true if the expression evaluates to a string.
   */
  public boolean isString()
  {
    return true;
  }

  //
  // expression creation
  //

  /**
   * Creates a class field $class::foo
   */
  @Override
  public Expr createClassConst(QuercusParser parser, StringValue name)
  {
    ExprFactory factory = parser.getExprFactory();

    String className = _value.toString();

    if ("self".equals(className)) {
      className = parser.getSelfClassName();

      return factory.createClassConst(className, name);
    }
    else if ("parent".equals(className)) {
      className = parser.getParentClassName();

      return factory.createClassConst(className, name);
    }
    else if ("static".equals(className)) {
      return factory.createClassVirtualConst(name);
    }
    else {
      return factory.createClassConst(className, name);
    }
  }

  /**
   * Creates a class field $class::foo
   */
  @Override
  public Expr createClassConst(QuercusParser parser, Expr name)
  {
    ExprFactory factory = parser.getExprFactory();

    String className = _value.toString();

    if ("self".equals(className)) {
      className = parser.getSelfClassName();

      return factory.createClassConst(className, name);
    }
    else if ("parent".equals(className)) {
      className = parser.getParentClassName();

      return factory.createClassConst(className, name);
    }
    else if ("static".equals(className)) {
      return factory.createClassVirtualConst(name);
    }
    else {
      return factory.createClassConst(className, name);
    }
  }

  /**
   * Evaluates the expression as a constant.
   *
   * @return the expression value.
   */
  public Value evalConstant()
  {
    return _value;
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
    return _value;
  }

  /**
   * Evaluates the expression as a string value.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public StringValue evalStringValue(Env env)
  {
    return _value;
  }

  @Override
  public String toString()
  {
    return "\"" + _value + "\"";
  }
}

