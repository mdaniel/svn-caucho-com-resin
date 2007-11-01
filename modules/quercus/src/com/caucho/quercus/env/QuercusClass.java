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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.ClassConstExpr;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.StringLiteralExpr;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.InstanceInitializer;
import com.caucho.util.IdentityIntMap;
import com.caucho.util.L10N;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Quercus runtime class.
 */
public class QuercusClass {
  private final L10N L = new L10N(QuercusClass.class);
  private final Logger log = Logger.getLogger(QuercusClass.class.getName());

  private final JavaClassDef _javaClassDef;
  private final ClassDef _classDef;

  private boolean _isJavaWrapper;
  
  private ClassDef []_classDefList;

  private QuercusClass _parent;

  private AbstractFunction _constructor;

  private AbstractFunction _fieldGet;
  private AbstractFunction _fieldSet;
  
  private AbstractFunction _call;

  private ArrayDelegate _arrayDelegate;
  private TraversableDelegate _traversableDelegate;
  private CountDelegate _countDelegate;

  private final ArrayList<InstanceInitializer> _initializers
    = new ArrayList<InstanceInitializer>();
  
  private final ArrayList<StringValue> _fieldNames
    = new ArrayList<StringValue>();
  
  private final IdentityIntMap _fieldMap
    = new IdentityIntMap();
  
  private final HashMap<StringValue,Expr> _fieldInitMap
    = new HashMap<StringValue,Expr>();

  /*
  private final IdentityHashMap<String,AbstractFunction> _methodMap
    = new IdentityHashMap<String,AbstractFunction>();
  
  private final HashMap<String,AbstractFunction> _lowerMethodMap
    = new HashMap<String,AbstractFunction>();
  */
  
  private final MethodMap<AbstractFunction> _methodMap
    = new MethodMap<AbstractFunction>();

  private final IdentityHashMap<String,Expr> _constMap
    = new IdentityHashMap<String,Expr>();

  private final HashMap<String,ArrayList<StaticField>> _staticFieldExprMap
    = new LinkedHashMap<String,ArrayList<StaticField>>();
  
  private final HashMap<String,Var> _staticFieldMap
    = new HashMap<String,Var>();

  public QuercusClass(ClassDef classDef, QuercusClass parent)
  {
    this(ModuleContext.getLocalContext(Thread.currentThread().getContextClassLoader()),
         classDef,
         parent);
  }

  public QuercusClass(ModuleContext moduleContext,
                      ClassDef classDef,
                      QuercusClass parent)
  {
    _classDef = classDef;
    _parent = parent;

    JavaClassDef javaClassDef = null;

    if (classDef instanceof JavaClassDef) {
      javaClassDef = (JavaClassDef) classDef;
      _isJavaWrapper = ! javaClassDef.isDelegate();
    }
    
    for (QuercusClass cls = parent; cls != null; cls = cls.getParent()) {
      AbstractFunction cons = cls.getConstructor();
      
      if (cons != null) {
        addMethod(cls.getName(), cons);
      }
    }

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

    for (int i = 0; i < classDefList.length; i++) {
      if (classDefList[i] instanceof JavaClassDef)
        javaClassDef = (JavaClassDef) classDefList[i];
    }
    
    _javaClassDef = javaClassDef;

    HashSet<String> ifaces = new HashSet<String>();

    for (int i = classDefList.length - 1; i >= 0; i--) {
      classDef = classDefList[i];

      if (classDef == null) {
        throw new NullPointerException("classDef:" + _classDef
                                       + " i:" + i + " parent:" + parent);
      }
      
      classDef.init();

      for (String iface : classDef.getInterfaces()) {
        
        // XXX: php/0cn2, but this is wrong:
        ClassDef ifaceDef = Env.getInstance().findClass(iface).getClassDef();
        // ClassDef ifaceDef = moduleContext.findClass(iface);

        if (ifaceDef != null) {
          if (ifaces.add(iface))
            ifaceDef.initClass(this);
        }
      }

      classDef.initClass(this);
    }
    
    if (_constructor == null && parent != null)
      _constructor = parent.getConstructor();
  }

  public ClassDef getClassDef()
  {
    return _classDef;
  }

  MethodMap<AbstractFunction> getMethodMap()
  {
    return _methodMap;
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

  public boolean isInterface()
  {
    return _classDef.isInterface();
  }

  /**
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }

  /**
   * Gets the constructor.
   */
  public AbstractFunction getConstructor()
  {
    return _constructor;
  }
  
  /**
   * Sets the array delegate (see ArrayAccess)
   */
  public void setArrayDelegate(ArrayDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} adding array delegate {1}",
				this,  delegate));

    _arrayDelegate = delegate;
  }
  
  /**
   * Gets the array delegate (see ArrayAccess)
   */
  public final ArrayDelegate getArrayDelegate()
  {
    return _arrayDelegate;
  }
  
  /**
   * Sets the traversable delegate
   */
  public void setTraversableDelegate(TraversableDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting traversable delegate {1}",
				this,  delegate));

    _traversableDelegate = delegate;
  }
  
  /**
   * Gets the traversable delegate
   */
  public final TraversableDelegate getTraversableDelegate()
  {
    return _traversableDelegate;
  }
  
  /**
   * Sets the count delegate
   */
  public void setCountDelegate(CountDelegate delegate)
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} setting count delegate {1}",
				this,  delegate));

    _countDelegate = delegate;
  }
  
  /**
   * Gets the count delegate
   */
  public final CountDelegate getCountDelegate()
  {
    return _countDelegate;
  }

  /**
   * Sets the __fieldGet
   */
  public void setFieldGet(AbstractFunction fun)
  {
    _fieldGet = fun;
  }

  /**
   * Returns the __fieldGet
   */
  public AbstractFunction getFieldGet()
  {
    return _fieldGet;
  }

  /**
   * Sets the __fieldSet
   */
  public void setFieldSet(AbstractFunction fun)
  {
    _fieldSet = fun;
  }

  /**
   * Returns the __fieldSet
   */
  public AbstractFunction getFieldSet()
  {
    return _fieldSet;
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
  public void addField(StringValue name, int index, Expr initExpr)
  {
    _fieldNames.add(name);
    _fieldMap.put(name, index);
    _fieldInitMap.put(name, initExpr);
  }

  /**
   * Adds a field.
   */
  public int addFieldIndex(StringValue name)
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
   * Returns a set of the fields and their initial values
   */
  public HashMap<StringValue,Expr> getClassVars()
  {
    return _fieldInitMap;
  }
  
  /**
   * Returns the declared functions.
   */
  public Iterable<AbstractFunction> getClassMethods()
  {
    return _methodMap.values();
  }

  /**
   * Adds a method.
   */
  public void addMethod(String name, AbstractFunction fun)
  {
    //php/09j9
    // XXX: this is a hack to get Zend Framework running, the better fix is
    // to initialize all interface classes before any concrete classes
    AbstractFunction existingFun = _methodMap.get(name);
    
    if (existingFun == null || ! fun.isAbstract())
      _methodMap.put(name, fun);
  }

  /**
   * Adds a static class field.
   */
  public void addStaticFieldExpr(String className, String name, Expr value)
  {
    ArrayList<StaticField> fieldList = _staticFieldExprMap.get(className);
    
    if (fieldList == null) {
      fieldList = new ArrayList<StaticField>();

      _staticFieldExprMap.put(className, fieldList);
    }
    
    fieldList.add(new StaticField(name, value));
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
  public int findFieldIndex(StringValue name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Returns the key set.
   */
  public ArrayList<StringValue> getFieldNames()
  {
    return _fieldNames;
  }

  public void validate(Env env)
  {
    if (! _classDef.isAbstract() && ! _classDef.isInterface()) {
      for (AbstractFunction fun : _methodMap.values()) {
        /* XXX: abstract methods need to be validated
              php/393g, php/393i, php/39j2
        if (! (absFun instanceof Function))
          continue;

        Function fun = (Function) absFun;
         */

        boolean isAbstract;

        // php/093g constructor
        if (_constructor != null && fun.getName().equals(_constructor.getName()))
          isAbstract = _constructor.isAbstract();
        else
          isAbstract = fun.isAbstract();

        if (isAbstract) {
          throw env.createErrorException(
            _classDef.getLocation(),
            L.l("Abstract function '{0}' must be implemented in concrete class {1}.",
                fun.getName(), getName()));
        }
      }
    }
  }

  public void init(Env env)
  {
    for (Map.Entry<String,ArrayList<StaticField>> map
	   : _staticFieldExprMap.entrySet()) {
      if (env.isInitializedClass(map.getKey()))
        continue;
      
      for (StaticField field : map.getValue()) {
        Value val;
        Expr expr = field._expr;

        //php/096f
        if (expr instanceof ClassConstExpr)
          val = ((ClassConstExpr)expr).eval(env);
        else
          val = expr.eval(env);

        Var var = new Var();
        var.set(val);
        //var.setGlobal();
        
        _staticFieldMap.put(field._name, var);
        //env.setGlobalValue(field._name, val);
      }
      
      env.addInitializedClass(map.getKey());
    }
  }

  public Var getStaticField(String name)
  {
    Var var = _staticFieldMap.get(name);
    
    if (var != null)
      return var;
    
    QuercusClass parent = getParent();
    
    if (parent != null)
      var = parent.getStaticField(name);

    return var;
  }
  
  //
  // Constructors
  //
  
  /**
   * Creates a new instance.
   */
  /*
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
  */

  /**
   * Creates a new instance.
   */
  public Value callNew(Env env, Value []args)
  {
    if (_classDef.isAbstract()) {
      throw env.createErrorException(L.l("abstract class '{0}' cannot be instantiated.",
					 _classDef.getName()));
    }
    else if (_classDef.isInterface()) {
      throw env.createErrorException(L.l("interface '{0}' cannot be instantiated.",
					 _classDef.getName()));
    }

    ObjectValue objectValue = null;

    if (_isJavaWrapper) {
      return _javaClassDef.callNew(env, args);
    }
    else if (_javaClassDef != null && _javaClassDef.isDelegate()) {
      objectValue = new ObjectExtValue(this);
    }
    else if (_javaClassDef != null && ! _javaClassDef.isDelegate()) {
      Value javaWrapper = _javaClassDef.callNew(env, new Value[0]);
      Object object = javaWrapper.toJavaObject();
      
      objectValue = new ObjectExtJavaValue(this, object, _javaClassDef);
    }
    else {
      objectValue = _classDef.newInstance(env, this);
    }

    for (int i = 0; i < _initializers.size(); i++) {
      _initializers.get(i).initInstance(env, objectValue);
    }
    
    AbstractFunction fun = findConstructor();

    if (fun != null)
      fun.callMethod(env, objectValue, args);
    else {
      //  if expr
    }

    return objectValue;
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
  public Value getField(Env env, Value qThis, StringValue name)
  {
    if (_fieldGet != null)
      return _fieldGet.callMethod(env, qThis, name);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Implements the __set method call.
   */
  public void setField(Env env, Value qThis, StringValue name, Value value)
  {
    if (_fieldSet != null)
      _fieldSet.callMethod(env, qThis, name, value);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    char []key = name.toCharArray();
    int hash = MethodMap.hash(key, key.length);

    AbstractFunction fun = _methodMap.get(hash, key, key.length);

    return fun;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionExact(String name)
  {
    throw new UnsupportedOperationException();
    
    // return _methodMap.get(name);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionLowerCase(String name)
  {
    throw new UnsupportedOperationException();
    
    //return _lowerMethodMap.get(name.toLowerCase());
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
    char []key = name.toCharArray();
    int hash = MethodMap.hash(key, key.length);

    return getFunction(hash, key, key.length);
  }

  /**
   * Finds the matching function.
   */
  public final AbstractFunction getFunction(int hash, char []name, int nameLen)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);
    
    if (fun != null)
      return fun;
    else {
      throw new QuercusRuntimeException(L.l("{0}::{1} is an unknown method",
					getName(), toMethod(name, nameLen)));
    }
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          int hash, char []name, int nameLength,
                          Expr []args)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLength);
    
    if (fun != null)
      return fun.callMethod(env, thisValue, args);
    else if (getCall() != null) {
      Expr []newArgs = new Expr[args.length + 1];
      newArgs[0] = new StringLiteralExpr(toMethod(name, nameLength));
      System.arraycopy(args, 0, newArgs, 1, args.length);
      
      return getCall().callMethod(env, thisValue, newArgs);
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}",
                           getName(), toMethod(name, nameLength)));
  }

  /**
   * calls the function.
   */
  public Value callMethod(Env env,
                          Value thisValue,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, args);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl(args));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl());
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
			  int hash, char []name, int nameLen,
			  Value a1)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, a1);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl()
				  .append(a1));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen,
			  Value a1, Value a2)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, a1, a2);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl()
				  .append(a1)
				  .append(a2));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, 
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, a1, a2, a3);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl()
				  .append(a1)
				  .append(a2)
				  .append(a3));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue, 
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, a1, a2, a3, a4);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl()
				  .append(a1)
				  .append(a2)
				  .append(a3)
				  .append(a4));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethod(Env env, Value thisValue,
                          int hash, char []name, int nameLen,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethod(env, thisValue, a1, a2, a3, a4, a5);
    else if (getCall() != null) {
      return getCall().callMethod(env,
				  thisValue,
				  env.createString(name, nameLen),
				  new ArrayValueImpl()
				  .append(a1)
				  .append(a2)
				  .append(a3)
				  .append(a4)
				  .append(a5));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Expr []args)
  {
    AbstractFunction fun = getFunction(hash, name, nameLen);
    
    return fun.callMethodRef(env, thisValue, args);
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value []args)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, args);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl(args));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl());
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, a1);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl()
                                     .append(a1));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, a1, a2);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl()
                                     .append(a1)
                                     .append(a2));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, a1, a2, a3);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl()
                                     .append(a1)
                                     .append(a2)
                                     .append(a3));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, a1, a2, a3, a4);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl()
                                     .append(a1)
                                     .append(a2)
                                     .append(a3)
                                     .append(a4));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  /**
   * calls the function.
   */
  public Value callMethodRef(Env env, Value thisValue,
                             int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    AbstractFunction fun = _methodMap.get(hash, name, nameLen);

    if (fun != null)
      return fun.callMethodRef(env, thisValue, a1, a2, a3, a4, a5);
    else if (getCall() != null) {
      return getCall().callMethodRef(env,
                                     thisValue,
                                     env.createString(name, nameLen),
                                     new ArrayValueImpl()
                                     .append(a1)
                                     .append(a2)
                                     .append(a3)
                                     .append(a4)
                                     .append(a5));
    }
    else
      return env.error(L.l("Call to undefined method {0}::{1}()",
                           getName(), toMethod(name, nameLen)));
  }  

  private String toMethod(char []key, int keyLength)
  {
    return new String(key, 0, keyLength);
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
    /*
    if (fun != null)
      return fun;

    fun = findStaticFunctionLowerCase(name.toLowerCase());
    */
    
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
    return getClass().getSimpleName() + "[" + getName() + "]";
  }

  static class StaticField
  {
    String _name;
    Expr _expr;
    
    StaticField(String name, Expr expr)
    {
      _name = name;
      _expr = expr;
    }
  }
}

