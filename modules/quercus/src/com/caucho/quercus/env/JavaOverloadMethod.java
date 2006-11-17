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

import java.util.*;
import java.lang.reflect.Method;

import com.caucho.quercus.expr.*;
import com.caucho.quercus.module.*;
import com.caucho.quercus.program.*;

/**
 * Represents the introspected static function information.
 */
public class JavaOverloadMethod extends AbstractJavaMethod {
  private final JavaMethod []_methods;

  /**
   * Creates the statically introspected function.
   *
   * @param method the introspected method.
   */
  public JavaOverloadMethod(ModuleContext moduleContext,
			    ArrayList<Method> methods)
  {
    Collections.sort(methods, OVERLOAD_COMPARATOR);

    int length = methods.get(methods.size() - 1).getParameterTypes().length;

    _methods = new JavaMethod[length + 1];

    for (int i = 0; i < methods.size(); i++) {
      Method method = methods.get(i);

      JavaMethod javaMethod = new JavaMethod(moduleContext, method);
      
      Class []param = method.getParameterTypes();

      for (int j = param.length; j >= 0; j--) {
	if (_methods[j] == null)
	  _methods[j] = javaMethod;
      }
    }
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Object obj, Expr []args)
  {
    if (args.length < _methods.length)
      return _methods[args.length].call(env, obj, args);
    else
      return _methods[_methods.length - 1].call(env, obj, args);
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Object obj, Value []args)
  {
    if (args.length < _methods.length)
      return _methods[args.length].call(env, obj, args);
    else
      return _methods[_methods.length - 1].call(env, obj, args);
  }

  public String toString()
  {
    return "JavaOverloadMethod[" + _methods[0] + "]";
  }

  private static final Comparator<Method> OVERLOAD_COMPARATOR
    = new Comparator<Method>() {
      public int compare(Method a, Method b)
      {
	return a.getParameterTypes().length - b.getParameterTypes().length;
      }
    };
}
