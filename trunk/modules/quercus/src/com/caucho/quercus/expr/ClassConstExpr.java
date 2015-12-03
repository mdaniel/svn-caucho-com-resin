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

import java.io.IOException;
import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.util.L10N;

/**
 * Represents a PHP parent::FOO constant call expression.
 */
public class ClassConstExpr extends Expr {
  private static final L10N L = new L10N(ClassMethodExpr.class);

  protected final String _className;
  protected final StringValue _name;

  public ClassConstExpr(Location location, String className, StringValue name)
  {
    super(location);

    _className = className.intern();
    _name = name;
  }

  public ClassConstExpr(String className, StringValue name)
  {
    _className = className.intern();
    _name = name;
  }

  //
  // function call creation
  //

  /**
   * Creates a function call expression
   */
  @Override
  public Expr createCall(QuercusParser parser,
                         Location location,
                         ArrayList<Expr> args)
    throws IOException
  {
    /*
    if (_className.equals(_name))
      return factory.createClassConstructor(location, _className, _name, args);
    else
      return factory.createClassMethodCall(location, _className, _name, args);
      */
    ExprFactory factory = parser.getExprFactory();

    return factory.createClassMethodCall(location, _className, _name, args);
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value eval(Env env)
  {
    return env.getClass(_className).getConstant(env, _name);
  }

  public String toString()
  {
    return _className + "::" + _name;
  }
}

