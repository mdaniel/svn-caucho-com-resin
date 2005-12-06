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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.lang.annotation.Annotation;

import com.caucho.php.Quercus;

import com.caucho.php.expr.Expr;
import com.caucho.php.expr.NullLiteralExpr;
import com.caucho.php.expr.DefaultExpr;
import com.caucho.php.expr.FunctionExpr;

import com.caucho.php.env.Env;
import com.caucho.php.env.Value;

import com.caucho.php.parser.PhpParser;

import com.caucho.php.program.AbstractFunction;

import com.caucho.php.module.Marshall;
import com.caucho.php.module.Reference;
import com.caucho.php.module.Optional;

import com.caucho.php.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
public class JavaMethod extends AbstractFunction {
  private static final L10N L = new L10N(JavaMethod.class);

  private final Method _method;
  private final Class []_paramTypes;
  private final boolean _hasEnv;
  private final Expr []_defaultExprs;
  private final Marshall []_marshallArgs;
  private final boolean _hasRestArgs;
  private final Marshall _unmarshallReturn;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public JavaMethod(Quercus quercus, Method method)
  {
    _method = method;

    Class []param = method.getParameterTypes();

    _paramTypes = param;

    _hasEnv = param.length > 0 && param[0].equals(Env.class);
    _hasRestArgs = param.length > 0 && param[param.length - 1].equals(Value[].class);

    Annotation [][]paramAnn = method.getParameterAnnotations();
    
    int argLength = paramAnn.length;

    if (_hasRestArgs)
      argLength -= 1;

    int envOffset = _hasEnv ? 1 : 0;

    _defaultExprs = new Expr[argLength - envOffset];

    _marshallArgs = new Marshall[argLength - envOffset];

    for (int i = 0; i < argLength - envOffset; i++) {
      boolean isOptional = false;
      boolean isReference = false;

      for (Annotation ann : paramAnn[i + envOffset]) {
        if (Optional.class.isAssignableFrom(ann.annotationType())) {
	  Optional opt = (Optional) ann;

	  if (! opt.value().equals("")) {
	    Expr expr = PhpParser.parseDefault(opt.value());
	    
	    _defaultExprs[i] = expr;
	  }
	  else
	    _defaultExprs[i] = new DefaultExpr();
        }
        else if (Reference.class.isAssignableFrom(ann.annotationType())) {
	  isReference = true;
	}
      }

      Class argType = param[i + envOffset];

      _marshallArgs[i] = Marshall.create(quercus, argType);

      if (isReference)
	_marshallArgs[i] = Marshall.MARSHALL_REFERENCE;
    }

    _unmarshallReturn = Marshall.create(quercus, method.getReturnType());
  }

  /**
   * Returns the function's method.
   *
   * @return the reflection method.
   */
  public Method getMethod()
  {
    return _method;
  }

  /**
   * Returns true if the environment is an argument.
   */
  public boolean getHasEnv()
  {
    return _hasEnv;
  }

  /**
   * Returns true if the environment has rest-style arguments.
   */
  public boolean getHasRestArgs()
  {
    return _hasRestArgs;
  }

  /**
   * Returns true if the result is a boolean.
   */
  public boolean isBoolean()
  {
    return _unmarshallReturn.isBoolean();
  }

  /**
   * Returns true if the result is a string.
   */
  public boolean isString()
  {
    return _unmarshallReturn.isString();
  }

  /**
   * Returns true if the result is a long.
   */
  public boolean isLong()
  {
    return _unmarshallReturn.isLong();
  }

  /**
   * Returns true if the result is a double.
   */
  public boolean isDouble()
  {
    return _unmarshallReturn.isDouble();
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
    throws Exception
  {
    if (_defaultExprs.length == args.length)
      return args;

    int length = _defaultExprs.length;;

    if (_defaultExprs.length < args.length) {
      if (_hasRestArgs)
	length = args.length;
      else {
	env.warning(L.l("function '{0}' has {1} required arguments, but only {2} were provided",
			_method.getName(), _defaultExprs.length, args.length));

	return null;
      }
    }
    else if (_defaultExprs[args.length] == null) {
      int required;

      for (required = args.length;
	   required < _defaultExprs.length;
	   required++) {
	if (_defaultExprs[required] != null)
	  break;
      }

      env.warning(L.l("function '{0}' has {1} required arguments, but only {2} were provided",
		      _method.getName(), required, args.length));

      return null;
    }

    Expr []expandedArgs = new Expr[length];

    System.arraycopy(args, 0, expandedArgs, 0, args.length);

    if (args.length < expandedArgs.length) {
      for (int i = args.length; i < expandedArgs.length; i++) {
	Expr defaultExpr = _defaultExprs[i];

	if (defaultExpr == null)
	  defaultExpr = NullLiteralExpr.NULL;

	expandedArgs[i] = defaultExpr;
      }
    }

    return expandedArgs;
  }

  public Value eval(Env env, Value []value)
  {
    throw new UnsupportedOperationException();
  }

  public Value eval(Env env, Object obj, Expr []expr)
    throws Throwable
  {
    if (expr.length != _marshallArgs.length) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object []args = new Object[_marshallArgs.length];

    for (int i = 0; i < args.length; i++) {
      args[i] = _marshallArgs[i].marshall(env, expr[i], _paramTypes[i]);
    }

    Object value = _method.invoke(obj, args);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj, Value []phpArgs)
    throws Throwable
  {
    if (phpArgs.length != _marshallArgs.length) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object []args = new Object[_marshallArgs.length];

    for (int i = 0; i < args.length; i++) {
      args[i] = _marshallArgs[i].marshall(env, phpArgs[i], _paramTypes[i]);
    }

    Object value = _method.invoke(obj, args);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj)
    throws Throwable
  {
    if (_marshallArgs.length != 0) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object value = _method.invoke(obj);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj, Value phpArg1)
    throws Throwable
  {
    if (_marshallArgs.length != 1) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object []javaArgs = new Object[] {
      _marshallArgs[0].marshall(env, phpArg1, _paramTypes[0]),
    };
    
    Object value = _method.invoke(obj, javaArgs);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public void generate(PhpWriter out, Expr funExpr, Expr []expr)
  {
    throw new UnsupportedOperationException();
  }
}
