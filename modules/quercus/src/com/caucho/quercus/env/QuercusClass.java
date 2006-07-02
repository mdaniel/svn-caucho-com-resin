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
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.logging.Logger;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.Function;
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
  
  private AbstractFunction _get;
  private AbstractFunction _set;
  private AbstractFunction _call;
  
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

  private final IdentityHashMap<String,Expr> _constMap
    = new IdentityHashMap<String,Expr>();

  private final HashMap<String,Expr> _staticFieldMap
    = new LinkedHashMap<String,Expr>();
  
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
   * Sets the __get
   */
  public void setGet(AbstractFunction fun)
  {
    _get = fun;
  }

  /**
   * Sets the __set
   */
  public void setSet(AbstractFunction fun)
  {
    _set = fun;
  }

  /**
   * Sets the __set
   */
  public AbstractFunction getSetField()
  {
    return _set;
  }

  /**
   * Sets the __call
   */
  public void setCall(AbstractFunction fun)
  {
    _call = fun;
  }

  /**
   * Sets the __call
   */
  public AbstractFunction getCall()
  {
    return _call;
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
    _methodMap.put(name.intern(), fun);
    _lowerMethodMap.put(name.toLowerCase(), fun);
  }

  /**
   * Adds a static field
   */
  public void addStaticField(String name, Expr expr)
  {
    _staticFieldMap.put(name, expr);
  }

  /**
   * Adds a constant definition
   */
  public void addConstant(String name, Expr expr)
  {
    _constMap.put(name, expr);
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

  public void validate(Env env)
  {
    if (! _classDef.isAbstract() && ! _classDef.isInterface()) {
      for (AbstractFunction absFun : _methodMap.values()) {
	if (! (absFun instanceof Function))
	  continue;

	Function fun = (Function) absFun;

	if (fun.isAbstract())
	  throw env.errorException(L.l("Abstract function '{0}' must be implemented in concrete class {1}.",
			      fun.getName(), getName()));
      }
    }
  }

  public void init(Env env)
  {
    for (Map.Entry<String,Expr> entry : _staticFieldMap.entrySet()) {
      env.setGlobalValue(entry.getKey(), entry.getValue().eval(env));
    }
  }

  //
  // Constructors
  //
  
  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Expr []args)
  {
    Value object = _classDef.callNew(env, args);

    if (object != null)
      return object;
    
    object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null) {
      fun.callMethod(env, object, args);
    }

    return object;
  }

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Value []args)
  {
    Value object = _classDef.callNew(env, args);

    if (object != null)
      return object;
    
    object = newInstance(env);

    AbstractFunction fun = findConstructor();

    if (fun != null)
      fun.callMethod(env, object, args);
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
  public Value newInstance(Env env)
  {
    Value obj = _classDef.newInstance(env, this);
    
    for (int i = 0; i < _initializers.size(); i++) {
      _initializers.get(i).initInstance(env, obj);
    }
    
    return obj;
  }

  /**
   * Finds the matching constructor.
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  //
  // Fields
  //

  /**
   * Implements the __get method call.
   */
  public Value getField(Env env, Value qThis, String field)
  {
    if (_get != null)
      return _get.callMethod(env, qThis, new StringValueImpl(field));
    else
      return UnsetValue.UNSET;
  }

  /**
   * Implements the __set method call.
   */
  public void setField(Env env, Value qThis, String field, Value value)
  {
    if (_set != null)
      _set.callMethod(env, qThis, new StringValueImpl(field), value);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    AbstractFunction fun = _methodMap.get(name);

    if (fun == null)
      fun = _lowerMethodMap.get(name.toLowerCase());

    return fun;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionExact(String name)
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
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name, Expr []args)
  {
    return getFunction(name).callMethod(env, thisValue, args);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name, Value []args)
  {
    return getFunction(name).callMethod(env, thisValue, args);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name)
  {
    return getFunction(name).callMethod(env, thisValue);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name,
			  Value a1)
  {
    return getFunction(name).callMethod(env, thisValue, a1);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2)
  {
    return getFunction(name).callMethod(env, thisValue, a1, a2);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3)
  {
    return getFunction(name).callMethod(env, thisValue, a1, a2, a3);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4)
  {
    return getFunction(name).callMethod(env, thisValue, a1, a2, a3, a4);
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return getFunction(name).callMethod(env, thisValue, a1, a2, a3, a4, a5);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name, Expr []args)
  {
    return getFunction(name).callMethodRef(env, thisValue, args);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name, Value []args)
  {
    return getFunction(name).callMethodRef(env, thisValue, args);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name)
  {
    return getFunction(name).callMethodRef(env, thisValue);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name,
			  Value a1)
  {
    return getFunction(name).callMethodRef(env, thisValue, a1);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name,
			  Value a1, Value a2)
  {
    return getFunction(name).callMethodRef(env, thisValue, a1, a2);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3)
  {
    return getFunction(name).callMethodRef(env, thisValue, a1, a2, a3);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4)
  {
    return getFunction(name).callMethodRef(env, thisValue, a1, a2, a3, a4);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue, String name,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return getFunction(name).callMethodRef(env, thisValue, a1, a2, a3, a4, a5);
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
  public final Value getConstant(Env env, String name)
  {
    Expr expr = _constMap.get(name);

    if (expr != null)
      return expr.eval(env);

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

