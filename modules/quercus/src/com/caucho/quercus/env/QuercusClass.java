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

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.logging.Logger;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.InstanceInitializer;

import com.caucho.util.L10N;
import com.caucho.util.IdentityIntMap;

/**
 * Represents a Quercus runtime class.
 */
public class QuercusClass {
  private final L10N L = new L10N(QuercusClass.class);
  private final Logger log = Logger.getLogger(QuercusClass.class.getName());

  private final ClassDef _classDef;
  
  private ClassDef []_classDefList;

  private QuercusClass _parent;

  private AbstractFunction _constructor;
  
  private final ArrayList<InstanceInitializer> _initializers
    = new ArrayList<InstanceInitializer>();
  
  private final ArrayList<String> _fieldNames
    = new ArrayList<String>();
  
  private final IdentityIntMap _fieldMap
    = new IdentityIntMap();
  
  private final IdentityHashMap<String,AbstractFunction> _methodMap
    = new IdentityHashMap<String,AbstractFunction>();
  
  private final HashMap<String,AbstractFunction> _lowerMethodMap
    = new HashMap<String,AbstractFunction>();

  public QuercusClass(ClassDef classDef, QuercusClass parent)
  {
    _classDef = classDef;
    _parent = parent;

    ClassDef []classDefList;
    
    if (_parent != null) {
      classDefList = new ClassDef[parent._classDefList.length + 1];

      System.arraycopy(parent._classDefList, 0, classDefList, 1,
		       parent._classDefList.length);

      classDefList[0] = classDef;
    }
    else {
      classDefList = new ClassDef[] { classDef };
    }
    
    _classDefList = classDefList;

    for (int i = classDefList.length - 1; i >= 0; i--) {
      classDefList[i].initClass(this);
    }
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
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }

  /**
   * Adds an initializer
   */
  public void addInitializer(InstanceInitializer init)
  {
    _initializers.add(init);
  }

  /**
   * Adds a field.
   */
  public void addField(String name, int index)
  {
    _fieldNames.add(name);
    _fieldMap.put(name, index);
  }

  /**
   * Adds a field.
   */
  public int addFieldIndex(String name)
  {
    int index = _fieldMap.get(name);

    if (index >= 0)
      return index;
    else {
      index = _fieldNames.size();
    
      _fieldMap.put(name, index);
      _fieldNames.add(name);

      return index;
    }
  }

  /**
   * Adds a method.
   */
  public void addMethod(String name, AbstractFunction fun)
  {
    _methodMap.put(name, fun);
    _lowerMethodMap.put(name.toLowerCase(), fun);
  }

  /**
   * Returns the number of fields.
   */
  public int getFieldSize()
  {
    return _fieldNames.size();
  }

  /**
   * Returns the field index.
   */
  public int findFieldIndex(String name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Returns the key set.
   */
  public ArrayList<String> getFieldNames()
  {
    return _fieldNames;
  }

  /**
   * Creates a new instance.
   */
  public Value evalNew(Env env, Expr []expr)
    throws Throwable
  {
    Value object = newInstance(env);

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
    Value object = newInstance(env);

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
      if (_classDefList[i].getName().equals(name))
	return true;
    }

    return false;
  }

  /**
   * Creates a new instance.
   */
  public Value newInstance(Env env)
    throws Throwable
  {
    // CompiledObjectValue object = new CompiledObjectValue(this);
    ObjectValue object = new ObjectExtValue(this);
    

    for (int i = 0; i < _initializers.size(); i++) {
      _initializers.get(i).initInstance(env, object);
    }
    
    return object;
  }

  /**
   * Finds the matching constructor.
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    return _methodMap.get(name);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionLowerCase(String name)
  {
    return _lowerMethodMap.get(name.toLowerCase());
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
    for (ClassDef a_classDefList : _classDefList) {
      Expr expr = a_classDefList.findConstant(name);

      if (expr != null)
        return expr;
    }
    
    return null;
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(String name)
  {
    AbstractFunction fun = findFunction(name);

    if (fun != null)
      return fun;

    fun = findFunctionLowerCase(name.toLowerCase());
    
    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), name));
    }
  }

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name, Expr []args)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, args);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name, Value []args)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, args);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name,
			  Value a1)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, a1);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, a1, a2);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, a1, a2, a3);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, a1, a2, a3, a4);
  }  

  /**
   * evaluates the function.
   */
  public Value evalMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return getFunction(name).evalMethod(env, thisValue, a1, a2, a3, a4, a5);
  }  

  /**
   * Finds a function.
   */
  public AbstractFunction findStaticFunctionLowerCase(String name)
  {
    return null;
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getStaticFunction(String name)
  {
    AbstractFunction fun = findStaticFunction(name);

    if (fun != null)
      return fun;

    fun = findStaticFunctionLowerCase(name.toLowerCase());
    
    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), name));
    }
  }

  /**
   * Finds the matching constant
   */
  public Value findConstant(Env env, String name)
  {
    return null;
  }

  /**
   * Finds the matching constant
   */
  public final Value getConstant(Env env, String name)
    throws Throwable
  {
    Value value = findConstant(env, name);

    if (value != null)
      return value;

    throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown constant",
					getName(), name));
  }

  public String toString()
  {
    return "QuercusClass[" + getName() + "]";
  }
  
  /**
   * Returns a set of the fields and their values
   * @return a set of the fields and their values
   */
  public Set<Map.Entry<String, Expr>> getClassVars()
  {
  	return _classDef.fieldSet();
  }
  
  /**
   * Returns a set of the method names and their values
   * @return a set of the method names and their values
   */
  public Set<Map.Entry<String, AbstractFunction>> getClassMethods()
  {
  	return _classDef.functionSet();
  }
}

