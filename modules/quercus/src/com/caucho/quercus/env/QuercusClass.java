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
import com.caucho.quercus.program.AbstractClassDef;
import com.caucho.quercus.program.AbstractFunction;

/**
 * Represents a PHP class.
 */
public class QuercusClass extends AbstractQuercusClass {
 // private final L10N L = new L10N(QuercusClass.class);

  private final AbstractClassDef _classDef;
  
  private AbstractClassDef []_classDefList;

  private QuercusClass _parent;

  public QuercusClass(AbstractClassDef classDef, QuercusClass parent)
  {
    _classDef = classDef;
    _parent = parent;

    AbstractClassDef []classDefList;
    
    if (_parent != null) {
      classDefList = new AbstractClassDef[parent._classDefList.length + 1];

      System.arraycopy(parent._classDefList, 0, classDefList, 1,
		       parent._classDefList.length);

      classDefList[0] = classDef;
    }
    else {
      classDefList = new AbstractClassDef[] { classDef };
    }
    
    _classDefList = classDefList;
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _classDef.getName();
  }

  /**
   * Returns the parent class.
   */
  public QuercusClass getParent()
  {
    return _parent;
  }

  /**
   * Creates a new instance.
   */
  public Value evalNew(Env env, Expr []expr)
    throws Throwable
  {
    ObjectValue object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null) {
      fun.evalMethod(env, object, expr);
    }
    else {
      //  if expr
    }

    return object;
  }

  /**
   * Creates a new instance.
   */
  public Value evalNew(Env env, Value []args)
    throws Throwable
  {
    ObjectValue object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null)
      fun.evalMethod(env, object, args);
    else {
      //  if expr
    }

    return object;
  }

  /**
   * Returns the parent class.
   */
  public String getParentName()
  {
    return _classDefList[0].getParentName();
  }

  /**
   * Returns true for an implementation of a class
   */
  public boolean isA(String name)
  {
    for (int i = _classDefList.length - 1; i >= 0; i--) {
      if (_classDefList[i].isA(name))
	return true;
    }

    return false;
  }

  /**
   * Creates a new instance.
   */
  public ObjectValue newInstance(Env env)
    throws Throwable
  {
    ObjectValue object = new ObjectValue(this);

    for (int i = _classDefList.length - 1; i >= 0; i--) {
      _classDefList[i].initInstance(env, object);
    }

    return object;
  }

  /**
   * Finds the matching constructor.
   */
  public AbstractFunction findConstructor()
  {
    // XXX: cache method
    for (AbstractClassDef a_classDefList : _classDefList) {
      AbstractFunction fun = a_classDefList.findConstructor();

      if (fun != null)
        return fun;
    }
    
    return null;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    // XXX: cache method
    for (AbstractClassDef a_classDefList : _classDefList) {
      AbstractFunction fun = a_classDefList.findFunction(name);

      if (fun != null)
        return fun;

    }

    name = name.toLowerCase();
    for (AbstractClassDef a_classDefList1 : _classDefList) {
      AbstractFunction fun = a_classDefList1.findFunctionLowerCase(name);

      if (fun != null)
        return fun;

    }
    
    return null;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionLowerCase(String name)
  {
    // XXX: cache method
    for (AbstractClassDef a_classDefList : _classDefList) {
      AbstractFunction fun = a_classDefList.findFunctionLowerCase(name);

      if (fun != null)
        return fun;
    }
    
    return null;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findStaticFunction(String name)
  {
    return findFunction(name);
  }

  /**
   * Finds the matching constant.
   */
  public Expr findConstant(String name)
  {
    // XXX: cache constant
    for (AbstractClassDef a_classDefList : _classDefList) {
      Expr expr = a_classDefList.findConstant(name);

      if (expr != null)
        return expr;
    }
    
    return null;
  }

  public String toString()
  {
    return "QuercusClass[" + getName() + "]";
  }
}

