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

package com.caucho.quercus.env;

import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.Arg;

/**
 * Represents a call to a function.
 */
@SuppressWarnings("serial")
public class CallbackFunction extends Callback {
  // public static final CallbackFunction INVALID_CALLBACK
  // = new CallbackFunction(null, "Invalid Callback");

  private StringValue _funName;
  private AbstractFunction _fun;

  public CallbackFunction(Env env, StringValue funName)
  {
    _funName = funName;
  }

  public CallbackFunction(AbstractFunction fun)
  {
    _fun = fun;
  }

  public CallbackFunction(AbstractFunction fun, StringValue funName)
  {
    _fun = fun;
    _funName = funName;
  }

  /**
   * Allow subclasses to set the abstract function directly.
   */
  protected void setFunction(AbstractFunction fun)
  {
    _fun = fun;
  }

  @Override
  public boolean isValid(Env env)
  {
    if (_fun != null) {
      return true;
    }

    _fun = env.findFunction(_funName);

    return _fun != null;
  }

  /**
   * Serializes the value.
   */
  public void serialize(Env env, StringBuilder sb)
  {
    CharSequence name;

    if (_fun != null)
      name = _fun.getName();
    else
      name = _funName;

    sb.append("S:");
    sb.append(name.length());
    sb.append(":\"");
    sb.append(name);
    sb.append("\";");
  }

  /**
   * Evaluates the callback with no arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env)
  {
    return getFunction(env).call(env);
  }

  /**
   * Evaluates the callback with 1 argument.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1)
  {
    return getFunction(env).call(env, a1);
  }

  /**
   * Evaluates the callback with 2 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2)
  {
    return getFunction(env).call(env, a1, a2);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    return getFunction(env).call(env, a1, a2, a3);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3,
                    Value a4)
  {
    return getFunction(env).call(env, a1, a2, a3, a4);
  }

  /**
   * Evaluates the callback with 3 arguments.
   *
   * @param env the calling environment
   */
  @Override
  public Value call(Env env, Value a1, Value a2, Value a3,
                    Value a4, Value a5)
  {
    return getFunction(env).call(env, a1, a2, a3, a4, a5);
  }

  @Override
  public Value call(Env env, Value []args)
  {
    return getFunction(env).call(env, args);
  }

  public String getCallbackName()
  {
    return _funName.toString();
  }

  public AbstractFunction getFunction(Env env)
  {
    if (_fun == null) {
      _fun = env.getFunction(_funName);
    }

    return _fun;
  }

  @Override
  public boolean isInternal(Env env)
  {
    return getFunction(env) instanceof JavaInvoker;
  }

  @Override
  public String getDeclFileName(Env env)
  {
    return getFunction(env).getDeclFileName(env);
  }

  @Override
  public int getDeclStartLine(Env env)
  {
    return getFunction(env).getDeclStartLine(env);
  }

  @Override
  public int getDeclEndLine(Env env)
  {
    return getFunction(env).getDeclEndLine(env);
  }

  @Override
  public String getDeclComment(Env env)
  {
    return getFunction(env).getDeclComment(env);
  }

  @Override
  public boolean isReturnsReference(Env env)
  {
    return getFunction(env).isReturnsReference(env);
  }

  @Override
  public Arg []getArgs(Env env)
  {
    return getFunction(env).getArgs(env);
  }

  /**
   * Exports the value.
   */
  @Override
  protected void varExportImpl(StringValue sb, int level)
  {
    sb.append("'' . \"\\0\" . '" + _funName.substring(1) + "'");
  }

  public String toString()
  {
    return getClass().getName() + '[' + _funName + ']';
  }
}
