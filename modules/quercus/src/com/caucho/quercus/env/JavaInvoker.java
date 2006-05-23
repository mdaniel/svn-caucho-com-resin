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

package com.caucho.quercus.env;

import java.io.IOException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.expr.DefaultExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.Marshall;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.UsesSymbolTable;
import com.caucho.quercus.module.VariableArguments;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
abstract public class JavaInvoker
  extends AbstractFunction
{
  private static final L10N L = new L10N(JavaInvoker.class);

  private static final Object [] NULL_ARGS = new Object[0];
  private static final Value [] NULL_VALUES = new Value[0];

  private final String _name;
  private final Class [] _paramTypes;
  private final Annotation [][] _paramAnn;
  
  private boolean _hasEnv;
  private Expr [] _defaultExprs;
  private Marshall [] _marshallArgs;
  private boolean _hasRestArgs;
  private Marshall _unmarshallReturn;

  private boolean _isRestReference;

  private boolean _isCallUsesVariableArgs;
  private boolean _isCallUsesSymbolTable;


  /**
   * Creates the statically introspected function.
   */
  public JavaInvoker(ModuleContext moduleContext,
                     String name,
                     Class []param,
                     Annotation [][]paramAnn,
                     Annotation []methodAnn,
                     Class retType)
  {
    _name = name;
    _paramTypes = param;
    _paramAnn = paramAnn;

    init(moduleContext, param, paramAnn, methodAnn, retType);
  }

  private void init(ModuleContext moduleContext,
		    Class []param,
		    Annotation [][]paramAnn,
		    Annotation []methodAnn,
		    Class retType)
  {
    boolean callUsesVariableArgs = false;
    boolean callUsesSymbolTable = false;
    boolean returnNullAsFalse = false;

    for (Annotation ann : methodAnn) {
      if (VariableArguments.class.isAssignableFrom(ann.annotationType())) {
        callUsesVariableArgs = true;
      }

      if (UsesSymbolTable.class.isAssignableFrom(ann.annotationType())) {
        callUsesSymbolTable = true;
      }

      if (ReturnNullAsFalse.class.isAssignableFrom(ann.annotationType())) {
        returnNullAsFalse = true;
      }
    }

    _isCallUsesVariableArgs = callUsesVariableArgs;
    _isCallUsesSymbolTable = callUsesSymbolTable;

    _hasEnv = param.length > 0 && param[0].equals(Env.class);

    boolean hasRestArgs = false;
    boolean isRestReference = false;

    if (param.length > 0 && param[param.length - 1].equals(Value[].class)) {
      hasRestArgs = true;

      for (Annotation ann : paramAnn[param.length - 1]) {
        if (Reference.class.isAssignableFrom(ann.annotationType()))
          isRestReference = true;
      }
    }

    _hasRestArgs = hasRestArgs;
    _isRestReference = isRestReference;

    int argLength = param.length;

    if (_hasRestArgs)
      argLength -= 1;

    int envOffset = _hasEnv ? 1 : 0;

    _defaultExprs = new Expr[argLength - envOffset];

    _marshallArgs = new Marshall[argLength - envOffset];

    for (int i = 0; i < argLength - envOffset; i++) {
      boolean isReference = false;

      for (Annotation ann : paramAnn[i + envOffset]) {
        if (Optional.class.isAssignableFrom(ann.annotationType())) {
          Optional opt = (Optional) ann;

          if (! opt.value().equals("")) {
            Expr expr = QuercusParser.parseDefault(opt.value());

            _defaultExprs[i] = expr;
          } else
            _defaultExprs[i] = new DefaultExpr(getLocation());
        } else if (Reference.class.isAssignableFrom(ann.annotationType())) {
          isReference = true;
        }
      }

      Class argType = param[i + envOffset];

      if (isReference)
        _marshallArgs[i] = Marshall.MARSHALL_REFERENCE;
      else
        _marshallArgs[i] = Marshall.create(moduleContext, argType);
    }

    _unmarshallReturn = Marshall.create(moduleContext, retType, false,
					returnNullAsFalse);
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
   * Returns true if the rest argument is a reference.
   */
  public boolean isRestReference()
  {
    return _isRestReference;
  }

  /**
   * Returns the unmarshaller for the return
   */
  public Marshall getUnmarshallReturn()
  {
    return _unmarshallReturn;
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
   * Returns the marshall arguments.
   */
  protected Marshall []getMarshallArgs()
  {
    return _marshallArgs;
  }

  /**
   * Returns the parameter annotations.
   */
  protected Annotation [][]getParamAnn()
  {
    return _paramAnn;
  }

  /**
   * Returns the default expressions.
   */
  protected Expr []getDefaultExprs()
  {
    return _defaultExprs;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    if (_defaultExprs.length == args.length)
      return args;

    int length = _defaultExprs.length;
    ;

    if (_defaultExprs.length < args.length) {
      if (_hasRestArgs)
        length = args.length;
      else {
        env.warning(L.l(
          "function '{0}' has {1} required arguments, but only {2} were provided",
          _name,
          _defaultExprs.length,
          args.length));

        return null;
      }
    } else if (_defaultExprs[args.length] == null) {
      int required;

      for (required = args.length;
           required < _defaultExprs.length;
           required++) {
        if (_defaultExprs[required] != null)
          break;
      }

      env.warning(L.l(
        "function '{0}' has {1} required arguments, but only {2} were provided",
        _name,
        required,
        args.length));

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

  public Value call(Env env, Value []value)
  {
    return call(env, env.getThis(), value);
  }

  public Value call(Env env, Object obj, Expr []exprs)
  {
    int len = _defaultExprs.length + (_hasEnv ? 1 : 0) + (_hasRestArgs ? 1 : 0);

    Object []values = new Object[len];

    int k = 0;

    if (_hasEnv)
      values[k++] = env;

    for (int i = 0; i < _marshallArgs.length; i++) {
      Expr expr;

      if (i < exprs.length && exprs[i] != null)
        expr = exprs[i];
      else {
        expr = _defaultExprs[i];

        if (expr == null)
          expr = new DefaultExpr(getLocation());
      }

      values[k] = _marshallArgs[i].marshall(env, expr, _paramTypes[k]);

      k++;
    }

    if (_hasRestArgs) {
      Value []rest;

      int restLen = exprs.length - _marshallArgs.length;

      if (restLen <= 0)
        rest = NULL_VALUES;
      else {
        rest = new Value[restLen];


        for (int i = _marshallArgs.length; i < exprs.length; i++) {
          if (_isRestReference)
            rest[i - _marshallArgs.length] = exprs[i].evalRef(env);
          else
            rest[i - _marshallArgs.length] = exprs[i].eval(env);
        }
      }

      values[values.length - 1] = rest;
    }

    Object result = invoke(obj, values);
    
    return _unmarshallReturn.unmarshall(env, result);
  }

  public Value call(Env env, Object obj, Value []args)
  {
    int len = _defaultExprs.length + (_hasEnv ? 1 : 0) + (_hasRestArgs ? 1 : 0);

    Object []values = new Object[len];

    int k = 0;

    if (_hasEnv)
      values[k++] = env;

    for (int i = 0; i < _marshallArgs.length; i++) {
      Value value;

      if (i < args.length && args[i] != null)
        values[k] = _marshallArgs[i].marshall(env, args[i], _paramTypes[k]);
      else if (_defaultExprs[i] != null) {
        values[k] = _marshallArgs[i].marshall(env,
                                              _defaultExprs[i],
                                              _paramTypes[k]);
      } else {
        values[k] = _marshallArgs[i].marshall(env,
                                              NullValue.NULL,
                                              _paramTypes[k]);
      }

      k++;
    }

    if (_hasRestArgs) {
      Value []rest;

      int restLen = args.length - _marshallArgs.length;

      if (restLen <= 0)
        rest = NULL_VALUES;
      else {
        rest = new Value[restLen];

        for (int i = _marshallArgs.length; i < args.length; i++) {
          if (_isRestReference)
            rest[i - _marshallArgs.length] = args[i];
          else
            rest[i - _marshallArgs.length] = args[i].toValue();
        }
      }

      values[values.length - 1] = rest;
    }

    Object result = invoke(obj, values);
    return _unmarshallReturn.unmarshall(env, result);
  }

  public Value call(Env env, Object obj)
  {
    return call(env, obj, new Value[0]);
  }

  public Value call(Env env, Object obj, Value a1)
  {
    return call(env, obj, new Value[]{a1});
  }

  public Value call(Env env, Object obj, Value a1, Value a2)
  {
    return call(env, obj, new Value[]{a1, a2});
  }

  public Value call(Env env, Object obj, Value a1, Value a2, Value a3)
  {
    return call(env, obj, new Value[]{a1, a2, a3});
  }

  public Value call(Env env, Object obj,
                    Value a1, Value a2, Value a3, Value a4)
  {
    return call(env, obj, new Value[]{a1, a2, a3, a4});
  }

  public Value call(Env env, Object obj,
                    Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return call(env, obj, new Value[]{a1, a2, a3, a4, a5});
  }

  public void generate(PhpWriter out, Expr funExpr, Expr []expr)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  abstract public Object invoke(Object obj, Object []args);
}
