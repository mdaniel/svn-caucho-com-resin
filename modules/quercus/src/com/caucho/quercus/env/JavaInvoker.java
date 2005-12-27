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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.lang.annotation.Annotation;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.expr.DefaultExpr;
import com.caucho.quercus.expr.FunctionExpr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.parser.PhpParser;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.module.Marshall;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
abstract public class JavaInvoker extends AbstractFunction {
  private static final L10N L = new L10N(JavaInvoker.class);
  
  private static final Object []NULL_ARGS = new Object[0];

  private final String _name;
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
  public JavaInvoker(Quercus quercus,
		     String name,
		     Class []param,
		     Annotation [][]paramAnn,
		     Class retType)
  {
    _name = name;
    _paramTypes = param;

    _hasEnv = param.length > 0 && param[0].equals(Env.class);
    _hasRestArgs = param.length > 0 && param[param.length - 1].equals(Value[].class);

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

    _unmarshallReturn = Marshall.create(quercus, retType);
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
			_name, _defaultExprs.length, args.length));

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
		      _name, required, args.length));

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

    int len = _marshallArgs.length + (_hasEnv ? 1 : 0);
    int k = 0;

    Object []args = new Object[len];

    if (_hasEnv)
      args[k++] = env;

    for (int i = 0; i < _marshallArgs.length; i++) {
      args[k++] = _marshallArgs[i].marshall(env, expr[i], _paramTypes[i]);
    }

    Object value = invoke(obj, args);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj, Value []quercusArgs)
    throws Throwable
  {
    if (quercusArgs.length != _marshallArgs.length) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object []args = new Object[_marshallArgs.length];

    for (int i = 0; i < args.length; i++) {
      args[i] = _marshallArgs[i].marshall(env, quercusArgs[i], _paramTypes[i]);
    }

    Object value = invoke(obj, args);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj)
    throws Throwable
  {
    if (_marshallArgs.length != 0) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object value = invoke(obj, NULL_ARGS);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public Value eval(Env env, Object obj, Value quercusArg1)
    throws Throwable
  {
    if (_marshallArgs.length != 1) {
      env.warning("mismatched length");
      return NullValue.NULL;
    }

    Object []javaArgs = new Object[] {
      _marshallArgs[0].marshall(env, quercusArg1, _paramTypes[0]),
    };
    
    Object value = invoke(obj, javaArgs);

    return _unmarshallReturn.unmarshall(env, value);
  }

  public void generate(PhpWriter out, Expr funExpr, Expr []expr)
  {
    throw new UnsupportedOperationException();
  }

  abstract public Object invoke(Object obj, Object []args)
    throws Throwable;
}
