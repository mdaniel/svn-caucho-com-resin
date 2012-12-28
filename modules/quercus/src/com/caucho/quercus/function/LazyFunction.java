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

package com.caucho.quercus.function;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Lazily load a compiled function.
 */
public class LazyFunction extends AbstractFunction
{
  private static final Logger log
    = Logger.getLogger(LazyFunction.class.getName());
  private static final L10N L = new L10N(LazyFunction.class);

  private final QuercusContext _quercus;
  private final String _name;
  private final Class<?> _pageClass;
  private final String _className;

  private AbstractFunction _fun;

  public LazyFunction(QuercusContext quercus, String name,
                      Class<?> pageClass,
                      String className)
  {
    _quercus = quercus;
    _name = name;
    _pageClass = pageClass;
    _className = className;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  public AbstractFunction toFun()
  {
    if (_fun != null)
      return _fun;
    else
      return this;
  }

  private AbstractFunction getFun(Env env)
  {
    try {
      ClassLoader loader = _pageClass.getClassLoader();

      String className = _pageClass.getName() + "$" + _className;

      Class<?> cl = Class.forName(className, false, loader);

      /*
      Constructor ctor = cl.getConstructors()[0];
      ctor.setAccessible(true);
      */

      _fun = (AbstractFunction) cl.newInstance();

      int id = _quercus.findFunctionId(env.createString(_name));

      env._fun[id] = _fun;

      return _fun;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getComment()
  {
    if (_fun != null)
      return _fun.getComment();
    else
      return getFun(Env.getInstance()).getComment();
  }

  //
  // call
  //

  @Override
  public Value call(Env env, Value []argValues)
  {
    return getFun(env).call(env, argValues);
  }

  @Override
  public Value call(Env env, Value arg)
  {
    return getFun(env).call(env, arg);
  }

  @Override
  public Value call(Env env, Value a1, Value a2)
  {
    return getFun(env).call(env, a1, a2);
  }

  @Override
  public Value call(Env env, Value a1, Value a2, Value a3)
  {
    return getFun(env).call(env, a1, a2, a3);
  }

  @Override
  public Value call(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return getFun(env).call(env, a1, a2, a3, a4);
  }

  @Override
  public Value call(Env env, Value a1, Value a2,
                    Value a3, Value a4, Value a5)
  {
    return getFun(env).call(env, a1, a2, a3, a4, a5);
  }

  //
  // call ref
  //

  @Override
  public Value callRef(Env env, Value []argValues)
  {
    return getFun(env).callRef(env, argValues);
  }

  @Override
  public Value callRef(Env env, Value arg)
  {
    return getFun(env).callRef(env, arg);
  }

  @Override
  public Value callRef(Env env, Value a1, Value a2)
  {
    return getFun(env).callRef(env, a1, a2);
  }

  @Override
  public Value callRef(Env env, Value a1, Value a2, Value a3)
  {
    return getFun(env).callRef(env, a1, a2, a3);
  }

  @Override
  public Value callRef(Env env, Value a1, Value a2, Value a3, Value a4)
  {
    return getFun(env).callRef(env, a1, a2, a3, a4);
  }

  @Override
  public Value callRef(Env env, Value a1, Value a2,
                       Value a3, Value a4, Value a5)
  {
    return getFun(env).callRef(env, a1, a2, a3, a4, a5);
  }

  //
  // call copy
  //

  @Override
  public Value callCopy(Env env, Value []argValues)
  {
    return getFun(env).callCopy(env, argValues);
  }
}

