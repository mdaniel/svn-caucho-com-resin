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
public class JavaMethod extends JavaInvoker {
  private static final L10N L = new L10N(JavaMethod.class);

  private final Method _method;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public JavaMethod(Quercus quercus, Method method)
  {
    super(quercus, method.getName(),
	  method.getParameterTypes(),
	  method.getParameterAnnotations(),
	  method.getAnnotations(),
	  method.getReturnType());
    
    _method = method;
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

  public Object invoke(Object obj, Object []args)
    throws Throwable
  {
    return _method.invoke(obj, args);
  }

  public void generate(PhpWriter out, Expr funExpr, Expr []expr)
  {
    throw new UnsupportedOperationException();
  }
}
