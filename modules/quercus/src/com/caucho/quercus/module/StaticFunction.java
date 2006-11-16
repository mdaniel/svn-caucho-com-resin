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

package com.caucho.quercus.module;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.JavaInvoker;
import com.caucho.quercus.expr.DefaultExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.expr.RequiredExpr;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;

/**
 * Represents the introspected static function information.
 */
public class StaticFunction extends JavaInvoker {
  private static final L10N L = new L10N(StaticFunction.class);
  private static final Logger log =
    Logger.getLogger(StaticFunction.class.getName());

  protected final QuercusModule _quercusModule;
  protected final Method _method;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public StaticFunction(ModuleContext moduleContext,
                        QuercusModule quercusModule,
                        Method method)
  {
    super(moduleContext,
          method.getName(),
          method.getParameterTypes(),
          method.getParameterAnnotations(),
          method.getAnnotations(),
          method.getReturnType());

    _method = method;
    _quercusModule = quercusModule;
  }

  /**
   * Returns the owning module object.
   *
   * @return the module object
   */
  public QuercusModule getModule()
  {
    return _quercusModule;
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
   * Evalutes the function.
   */
  @Override
  public Object invoke(Object obj, Object []javaArgs)
  {
    try {
      return _method.invoke(_quercusModule, javaArgs);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw QuercusModuleException.create(e.getCause());
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  public String toString()
  {
    return "StaticFunction[" + _method + "]";
  }
}
