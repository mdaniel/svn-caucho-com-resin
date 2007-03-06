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

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.util.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Represents the introspected static function information.
 */
public class JavaOverloadMethod extends AbstractJavaMethod {
  private static final L10N L = new L10N(JavaOverloadMethod.class);
  
  private AbstractJavaMethod [][]_methodTable = new AbstractJavaMethod[0][];

  public JavaOverloadMethod(AbstractJavaMethod fun)
  {
    overload(fun);
  }

  public int getArgumentLength()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an overloaded java method.
   */
  public AbstractJavaMethod overload(AbstractJavaMethod fun)
  {
    int len = fun.getArgumentLength();

    if (_methodTable.length <= len) {
      AbstractJavaMethod [][]methodTable = new AbstractJavaMethod[len + 1][];

      System.arraycopy(_methodTable, 0, methodTable, 0, _methodTable.length);

      _methodTable = methodTable;
    }

    AbstractJavaMethod []methods = _methodTable[len];

    if (methods == null)
      _methodTable[len] = new AbstractJavaMethod[] { fun };
    else {
      AbstractJavaMethod []newMethods
        = new AbstractJavaMethod[methods.length + 1];

      System.arraycopy(methods, 0, newMethods, 0, methods.length);

      newMethods[methods.length] = fun;

      _methodTable[len] = newMethods;
    }

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
    if (_methodTable.length <= args.length)
      throw new RuntimeException("too long");
    else {
      AbstractJavaMethod []methods = _methodTable[args.length];

      if (methods == null || methods.length == 0)
        throw new QuercusException(L.l("'{0}' method call does not match expected length", getName()));
      else if (methods.length == 1)
        return methods[0].call(env, obj, args);
      else {
        AbstractJavaMethod method
          = getBestFitJavaMethod(_methodTable[args.length], args);
    
        return method.call(env, obj, args);
      }
    }
  }
  
  /**
   * Returns the Java function that matches the args passed in.
   */
  private AbstractJavaMethod getBestFitJavaMethod(AbstractJavaMethod []methods,
                                                  Value []args)
  {
    int size = methods.length;
    
    AbstractJavaMethod minCostJavaMethod = null;
    int minCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < size; i++) {
      AbstractJavaMethod javaMethod = methods[i];
      
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
      return methods[0];
  }
  
  /**
   * Returns the cost of marshaling for this method given the args.
   */
  public int getMarshalingCost(Value []args)
  {
    throw new UnsupportedOperationException();
    /*
    int size = _methods.size();
    int minCost = Integer.MAX_VALUE;
    
    for (int i = 0; i < size; i++) {
      int cost = _methods.get(i).getMarshalingCost(args);
      
      if (cost < minCost)
        minCost = cost;
    }

    return minCost;
    */
  }

  @Override
  public String getName()
  {
    AbstractJavaMethod method;

    for (int i = 0; i < _methodTable.length; i++) {
      if (_methodTable[i] != null)
        return _methodTable[i][0].getName();
    }

    return "unknown";
  }

  public String toString()
  {

    return "JavaOverloadMethod[" + getName() + "]";
  }
}
