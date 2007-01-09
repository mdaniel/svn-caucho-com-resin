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

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.ModuleContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Represents the introspected static function information.
 */
public class JavaOverloadMethod extends AbstractJavaMethod {
  private final ArrayList<AbstractJavaMethod> _methods;

  public JavaOverloadMethod(AbstractJavaMethod javaMethod)
  {
    _methods = new ArrayList<AbstractJavaMethod>();

    _methods.add(javaMethod);
  }
  
  /**
   * Returns an overloaded java method.
   */
  public AbstractJavaMethod overload(AbstractJavaMethod fun)
  {
    _methods.add(fun);
    
    return this;
  }

  /**
   * Evaluates the function.
   * XXX: define getBestFitMethod(Expr []args)
   */
  public Value call(Env env, Object obj, Expr []args)
  {
    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      values[i] = args[i].eval(env);
    }
    
    return call(env, obj, values);
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Object obj, Value []args)
  {
    AbstractJavaMethod javaMethod = getBestFitJavaMethod(args);
    
    return javaMethod.call(env, obj, args);
  }
  
  /**
   * Returns the Java function that matches the args passed in.
   */
  private AbstractJavaMethod getBestFitJavaMethod(Value []args)
  {
    int size = _methods.size();
    
    AbstractJavaMethod minCostJavaMethod = null;
    int minCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < size; i++) {
      AbstractJavaMethod javaMethod = _methods.get(i);
      
      int cost = javaMethod.getMarshalingCost(args);
      
      if (cost == 0)
        return javaMethod;
      
      if (cost < minCost) {
        minCost = cost;
        minCostJavaMethod = javaMethod;
      }
    }
    
    if (minCostJavaMethod != null)
      return minCostJavaMethod;
    else
      return _methods.get(0);
  }
  
  /**
   * Returns the cost of marshaling for this method given the args.
   */
  public int getMarshalingCost(Value []args)
  {
    int size = _methods.size();
    int minCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < size; i++) {
      int cost = _methods.get(i).getMarshalingCost(args);
      
      if (cost < minCost)
        minCost = cost;
    }

    return minCost;
  }

  public String toString()
  {
    return "JavaOverloadMethod[" + _methods.get(0) + "]";
  }
}
