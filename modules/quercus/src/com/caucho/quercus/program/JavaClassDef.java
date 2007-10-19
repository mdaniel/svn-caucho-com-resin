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

package com.caucho.quercus.program;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.annotation.Construct;
import com.caucho.quercus.annotation.Delegates;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.LiteralExpr;
import com.caucho.quercus.function.JavaMarshal;
import com.caucho.quercus.function.Marshal;
import com.caucho.quercus.function.MarshalFactory;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an introspected Java class.
 */
public class JavaClassDef extends ClassDef {
  private final static Logger log
    = Logger.getLogger(JavaClassDef.class.getName());
  private final static L10N L = new L10N(JavaClassDef.class);

  private final ModuleContext _moduleContext;

  private final String _name;
  private final Class _type;
  private final boolean _isAbstract;
  private final boolean _isArray;
  private final boolean _isInterface;

  private JavaClassDef _componentDef;

  protected volatile boolean _isInit;

  private final HashMap<String, Value> _constMap
    = new HashMap<String, Value>();

  private final MethodMap<AbstractJavaMethod> _functionMap
    = new MethodMap<AbstractJavaMethod>();

  private final HashMap<StringValue, AbstractJavaMethod> _getMap
    = new HashMap<StringValue, AbstractJavaMethod>();

  private final HashMap<StringValue, AbstractJavaMethod> _setMap
    = new HashMap<StringValue, AbstractJavaMethod>();

  // _fieldMap stores all public non-static fields
  // used by getField and setField
  private final HashMap<StringValue, FieldMarshalPair> _fieldMap
    = new HashMap<StringValue, FieldMarshalPair> ();

  private JavaMethod __getField = null;
  private JavaMethod __setField = null;
  
  private JavaMethod __get = null;
  private JavaMethod __set = null;
  
  private JavaMethod __call = null;

  private Method _printRImpl = null;
  private Method _varDumpImpl = null;

  private ArrayDelegate _arrayDelegate;
  private TraversableDelegate _traversableDelegate;
  private CountDelegate _countDelegate;

  private AbstractJavaMethod _cons;

  private Method _iteratorMethod;
  private Method _keySetMethod;

  private Marshal _marshal;

  public JavaClassDef(ModuleContext moduleContext, String name, Class type)
  {
    super(null, name, null, new String[] {});

    _moduleContext = moduleContext;
    _name = name;
    _type = type;

    _isAbstract = Modifier.isAbstract(type.getModifiers());
    _isArray = type.isArray();
    _isInterface = type.isInterface();
  }

  public static JavaClassDef create(ModuleContext moduleContext,
				    String name, Class type)
  {
    if (Double.class.isAssignableFrom(type)
	|| Float.class.isAssignableFrom(type))
      return new DoubleClassDef(moduleContext);
    else if (Number.class.isAssignableFrom(type))
      return new LongClassDef(moduleContext);
    else if (String.class.isAssignableFrom(type)
	     || Character.class.isAssignableFrom(type))
      return new StringClassDef(moduleContext);
    else if (Boolean.class.isAssignableFrom(type))
      return new BooleanClassDef(moduleContext);
    else if (Calendar.class.isAssignableFrom(type))
      return new CalendarClassDef(moduleContext);
    else if (Date.class.isAssignableFrom(type))
      return new DateClassDef(moduleContext);
    else if (URL.class.isAssignableFrom(type))
      return new URLClassDef(moduleContext);
    else if (Map.class.isAssignableFrom(type))
      return new JavaMapClassDef(moduleContext, name, type);
    else if (List.class.isAssignableFrom(type))
      return new JavaListClassDef(moduleContext, name, type);
    else if (Collection.class.isAssignableFrom(type))
      return new JavaCollectionClassDef(moduleContext, name, type);
    else
      return null;
  }

  /**
   * Returns the class name.
   */
  @Override
  public String getName()
  {
    return _name;
  }

  public Class getType()
  {
    return _type;
  }

  @Override
  public boolean isA(String name)
  {
    Class type = _type;

    while (type != null) {
      if (type.getSimpleName().equalsIgnoreCase(name))
        return true;

      if (hasInterface(name, type))
        return true;

      type = type.getSuperclass();
    }

    return false;
  }

  private boolean hasInterface(String name, Class type)
  {
    Class[] interfaces = type.getInterfaces();

    if (interfaces != null) {
      for (Class intfc : interfaces) {
        if (intfc.getSimpleName().equalsIgnoreCase(name))
          return true;

        if (hasInterface(name, intfc))
          return true;
      }
    }

    return false;
  }

  @Override
  public boolean isAbstract()
  {
    return _isAbstract;
  }

  public boolean isArray()
  {
    return _isArray;
  }

  @Override
  public boolean isInterface()
  {
    return _isInterface;
  }

  public JavaClassDef getComponentDef()
  {
    if (_componentDef == null) {
      Class compType = getType().getComponentType();
      _componentDef = _moduleContext.getJavaClassDefinition(compType.getName());
    }
    
    return _componentDef;
  }

  public Value wrap(Env env, Object obj)
  {
    if (!_isInit)
      init();
    
    return new JavaValue(env, obj, this);
  }

  private int cmpObject(Object lValue, Object rValue)
  {
    if (lValue == rValue)
      return 0;

    if (lValue == null)
      return -1;

    if (rValue == null)
      return 1;

    if (lValue instanceof Comparable) {
      if (!(rValue instanceof Comparable))
        return -1;

      return ((Comparable) lValue).compareTo(rValue);
    }
    else if (rValue instanceof Comparable) {
      return 1;
    }

    if (lValue.equals(rValue))
      return 0;

    String lName = lValue.getClass().getName();
    String rName = rValue.getClass().getName();

    return lName.compareTo(rName);
  }

  public int cmpObject(Object lValue, Object rValue, JavaClassDef rClassDef)
  {
    int cmp = cmpObject(lValue, rValue);

    if (cmp != 0)
        return cmp;

    // attributes
    // XX: not sure how to do this, to imitate PHP objects,
    // should getters be involved as well?

    for (Map.Entry<StringValue, FieldMarshalPair> lEntry : _fieldMap.entrySet()) {
      StringValue lFieldName = lEntry.getKey();
      FieldMarshalPair rFieldPair = rClassDef._fieldMap.get(lFieldName);

      if (rFieldPair == null)
        return 1;

      FieldMarshalPair lFieldPair = lEntry.getValue();

      try {
        Object lResult = lFieldPair._field.get(lValue);
        Object rResult = rFieldPair._field.get(lValue);

        int resultCmp = cmpObject(lResult, rResult);

        if (resultCmp != 0)
          return resultCmp;
      }
      catch (IllegalAccessException e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return 0;
      }
    }

    return 0;
  }

  /**
   * Returns the field getter.
   *
   * @param name
   * @return Value attained through invoking getter
   */
  public Value getField(Env env, Object obj, StringValue name)
  {
    AbstractJavaMethod get = _getMap.get(name);

    if (get != null) {
      try {
        return get.call(env, obj);
      } catch (Exception e) {
        log.log(Level.FINE, L.l(e.getMessage()), e);
	
        return null;
      }
    }

    FieldMarshalPair fieldPair = _fieldMap.get(name);
    if (fieldPair != null) {
      try {
        Object result = fieldPair._field.get(obj);
        return fieldPair._marshal.unmarshal(env, result);
      } catch (Exception e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
	
        return null;
      }
    }

    if (__getField != null) {
      try {
        return __getField.call(env, obj, name);
      } catch (Exception e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);

	return null;
      }
    }

    return null;
  }

  public Value putField(Env env,
                        Object obj,
                        StringValue name,
                        Value value)
  {
    AbstractJavaMethod setter = _setMap.get(name);

    if (setter != null) {
      try {
        return setter.call(env, obj, value);
      } catch (Exception e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
	
        return NullValue.NULL;
      }
    }

    FieldMarshalPair fieldPair = _fieldMap.get(name);

    if (fieldPair != null) {
      try {
        Class type = fieldPair._field.getType();
        Object marshaledValue = fieldPair._marshal.marshal(env, value, type);
        fieldPair._field.set(obj, marshaledValue);

        return value;

      } catch (Exception e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return NullValue.NULL;
      }
    }

    if (__setField != null) {
      try {
        return __setField.call(env, obj, name, value);
      } catch (Exception e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
	
        return NullValue.NULL;
      }
    }

    return null;
  }

  /**
   * Returns the marshal instance.
   */
  public Marshal getMarshal()
  {
    return _marshal;
  }

  /**
   * Creates a new instance.
   */
  @Override
  public ObjectValue newInstance(Env env, QuercusClass qClass)
  {
    // return newInstance();
    return null;
  }

  public Value newInstance()
  {
    return null;
    /*
    try {
      //Object obj = _type.newInstance();
      return new JavaValue(null, _type.newInstance(), this);
    } catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
    */
  }

  /**
   * Eval new
   */
  @Override
  public Value callNew(Env env, Expr []args)
  {
    return _cons.call(env, (Object) null, args);
  }

  /**
   * Eval new
   */
  @Override
  public Value callNew(Env env, Value []args)
  {
    if (_cons != null)
      return _cons.call(env, (Object) null, args);
    else
      return NullValue.NULL;
  }

  /**
   * Returns the matching method.
   */
  public AbstractFunction findFunction(String name)
  {
    return _functionMap.get(name);
  }

  /**
   * Returns the __call.
   */
  public AbstractFunction getCallMethod()
  {
    return __call;
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Expr []args)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method == null) {
      env.warning(L.l("{0}::{1} is an unknown method.",
                      _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }

    return method.call(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Value value,
                          int hash, char []name, int nameLen,
                          Expr []args)
  {
    return callMethod(env, value.toJavaObject(), hash, name, nameLen, args);
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Value value,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    return callMethod(env, value.toJavaObject(), hash, name, nameLen, args);
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, args);
    else if (__call != null) {
      Value []extArgs = new Value[args.length + 1];

      extArgs[0] = env.createString(name, nameLen);

      System.arraycopy(args, 0, extArgs, 1, args.length);
      
      return __call.call(env, obj, extArgs);
    }
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj);
    else if (__call != null)
      return __call.call(env, obj, env.createString(name, nameLen));
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value a1)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, a1);
    else if (__call != null)
      return __call.call(env, obj, env.createString(name, nameLen), a1);
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value a1, Value a2)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, a1, a2);
    else if (__call != null)
      return __call.call(env, obj, env.createString(name, nameLen),
                         a1, a2);
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, a1, a2, a3);
    else if (__call != null)
      return __call.call(env, obj, env.createString(name, nameLen),
                         a1, a2, a3);
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3, Value a4)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, a1, a2, a3, a4);
    else if (__call != null)
      return __call.call(env, obj, env.createString(name, nameLen),
                         a1, a2, a3, a4);
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  /**
   * Eval a method
   */
  public Value callMethod(Env env, Object obj,
                          int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    AbstractJavaMethod method = _functionMap.get(hash, name, nameLen);

    if (method != null)
      return method.call(env, obj, a1, a2, a3, a4, a5);
    else if (__call != null)
      return __call.call(env, obj,
                         new Value[] { env.createString(name, nameLen),
                                       a1, a2, a3, a4, a5 });
    else {
      env.error(L.l("'{0}::{1}' is an unknown method",
		    _name, toMethod(name, nameLen)));

      return NullValue.NULL;
    }
  }

  private String toMethod(char []name, int nameLen)
  {
    return new String(name, 0, nameLen);
  }

  /**
   * Initialize the quercus class.
   */
  @Override
  public void initClass(QuercusClass cl)
  {
    init();

    if (_cons != null) {
      cl.setConstructor(_cons);
      cl.addMethod("__construct", _cons);
    }

    for (AbstractJavaMethod value : _functionMap.values()) {
      cl.addMethod(value.getName(), value);
    }

    if (__getField != null)
      cl.setGetField(__getField);

    if (__setField != null)
      cl.setSetField(__setField);

    if (__call != null)
      cl.setCall(__call);

    if (_arrayDelegate != null)
      cl.setArrayDelegate(_arrayDelegate);

    if (_traversableDelegate != null)
      cl.setTraversableDelegate(_traversableDelegate);

    if (_countDelegate != null)
      cl.setCountDelegate(_countDelegate);

    for (Map.Entry<String,Value> entry : _constMap.entrySet()) {
      cl.addConstant(entry.getKey(), new LiteralExpr(entry.getValue()));
    }
  }

  /**
   * Finds the matching constant
   */
  public Value findConstant(Env env, String name)
  {
    return _constMap.get(name);
  }

  /**
   * Creates a new instance.
   */
  public void initInstance(Env env, Value value)
  {
  }

  /**
   * Returns the constructor
   */
  @Override
  public AbstractFunction findConstructor()
  {
    return null;
  }

  public synchronized void init()
  {
    if (_isInit)
      return;

    try {
      initInterfaceList(_type);
      introspect();
    }
    finally {
      _isInit = true;
    }
  }

  private void initInterfaceList(Class type)
  {
    Class[] ifaces = type.getInterfaces();

    if (ifaces == null)
      return;

    for (Class iface : ifaces) {

      JavaClassDef javaClassDef = _moduleContext.getJavaClassDefinition(iface);

      if (javaClassDef != null)
        addInterface(javaClassDef.getName());

      // recurse for parent interfaces
      initInterfaceList(iface);
    }
  }

  /**
   * Introspects the Java class.
   */
  private void introspect()
  {
    introspectConstants(_type);
    introspectMethods(_moduleContext, _type);
    introspectFields(_moduleContext, _type);

    _marshal = new JavaMarshal(this, false);

    Method consMethod = getConsMethod(_type);

    if (consMethod != null)
      _cons = new JavaMethod(_moduleContext, consMethod);
    else {
      Constructor []cons = _type.getConstructors();

      if (cons.length > 0) {
        int i;
        for (i = 0; i < cons.length; i++) {
          if (cons[i].isAnnotationPresent(Construct.class))
            break;
        }

        if (i < cons.length) {
          _cons = new JavaConstructor(_moduleContext, cons[i]);
        }
        else {
          _cons = new JavaConstructor(_moduleContext, cons[0]);
          for (i = 1; i < cons.length; i++) {
            _cons = _cons.overload(new JavaConstructor(_moduleContext, cons[i]));
          }
        }

      } else
        _cons = null;
    }


    introspectAnnotations(_type);

    try {
      Method method = _type.getMethod("iterator", new Class[0]);

      if (method != null
	  && Iterator.class.isAssignableFrom(method.getReturnType()))
        _iteratorMethod = method;
    } catch (Exception e) {
    }

    try {
      Method method = _type.getMethod("keySet", new Class[0]);

      if (method != null &&
          Set.class.isAssignableFrom(method.getReturnType()))
        _keySetMethod = method;
    } catch (Exception e) {
    }
  }

  private void introspectAnnotations(Class type)
  {
    try {
      if (type == null || type == Object.class)
	return;

      // interfaces
      for (Class<?> iface : type.getInterfaces())
	introspectAnnotations(iface);

      // super-class
      introspectAnnotations(type.getSuperclass());

      // this
      for (Annotation annotation : type.getAnnotations()) {
	if (annotation.annotationType() == Delegates.class) {
	  Class[] delegateClasses = ((Delegates) annotation).value();

	  for (Class cl : delegateClasses) {
	    boolean isDelegate = addDelegate(cl);
	  
	    if (! isDelegate)
	      throw new IllegalArgumentException(L.l("unknown @Delegate class '{0}'",
						     cl));
	  }
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (InstantiationException e) {
      throw new QuercusModuleException(e.getCause());
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  private boolean addDelegate(Class cl)
    throws InstantiationException, IllegalAccessException
  {
    boolean isDelegate = false;
    
    if (TraversableDelegate.class.isAssignableFrom(cl)) {
      _traversableDelegate = (TraversableDelegate) cl.newInstance();
      isDelegate = true;
    }

    if (ArrayDelegate.class.isAssignableFrom(cl)) {
      _arrayDelegate = (ArrayDelegate) cl.newInstance();
      isDelegate = true;
    }

    if (CountDelegate.class.isAssignableFrom(cl)) {
      _countDelegate = (CountDelegate) cl.newInstance();
      isDelegate = true;
    }

    return isDelegate;
  }

  private <T> boolean addDelegate(Class<T> cl,
                                  ArrayList<T> delegates,
                                  Class<? extends Object> delegateClass)
  {
    if (!cl.isAssignableFrom(delegateClass))
      return false;

    for (T delegate : delegates) {
      if (delegate.getClass() == delegateClass) {
        return true;
      }
    }

    try {
      delegates.add((T) delegateClass.newInstance());
    }
    catch (InstantiationException e) {
      throw new QuercusModuleException(e);
    }
    catch (IllegalAccessException e) {
      throw new QuercusModuleException(e);
    }
    
    return true;
  }

  private Method getConsMethod(Class type)
  {
    Method []methods = type.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      
      if (! method.getName().equals("__construct"))
	continue;
      if (! Modifier.isStatic(method.getModifiers()))
	continue;
      if (! Modifier.isPublic(method.getModifiers()))
	continue;

      return method;
    }

    return null;
  }

  protected void setCons(Method method)
  {
    _cons = new JavaMethod(_moduleContext, method);
  }

  /**
   * Introspects the Java class.
   */
  private void introspectFields(ModuleContext moduleContext, Class type)
  {
    if (type == null || type.equals(Object.class))
      return;

    if (! Modifier.isPublic(type.getModifiers()))
      return;

    // Introspect getXXX and setXXX
    // also register whether __get, __getField, __set, __setField exists
    Method[] methods = type.getMethods();

    for (Method method : methods) {
      if (Modifier.isStatic(method.getModifiers()))
        continue;

      String methodName = method.getName();
      int length = methodName.length();

      if (length > 3) {
        if (methodName.startsWith("get")) {
          StringValue quercusName
	    = javaToQuercusConvert(methodName.substring(3, length));
	  
          AbstractJavaMethod existingGetter = _getMap.get(quercusName);
          AbstractJavaMethod newGetter = new JavaMethod(moduleContext, method);
          
          if (existingGetter != null) {
            newGetter = existingGetter.overload(newGetter);
          }
          
          _getMap.put(quercusName, newGetter);
        }
        else if (methodName.startsWith("is")) {
          StringValue quercusName
	    = javaToQuercusConvert(methodName.substring(2, length));
          
          AbstractJavaMethod existingGetter = _getMap.get(quercusName);
          AbstractJavaMethod newGetter = new JavaMethod(moduleContext, method);
          
          if (existingGetter != null) {
            newGetter = existingGetter.overload(newGetter);
          }
          
          _getMap.put(quercusName, newGetter);
        }
        else if (methodName.startsWith("set")) {
          StringValue quercusName
	    = javaToQuercusConvert(methodName.substring(3, length));
          
          AbstractJavaMethod existingSetter = _setMap.get(quercusName);
          AbstractJavaMethod newSetter = new JavaMethod(moduleContext, method);
	  
          if (existingSetter != null)
            newSetter = existingSetter.overload(newSetter);
	    
	  _setMap.put(quercusName, newSetter);
        } else if ("__get".equals(methodName)) {
          __get = new JavaMethod(moduleContext, method);
        } else if ("__getField".equals(methodName)) {
          __getField = new JavaMethod(moduleContext, method);
        } else if ("__set".equals(methodName)) {
          __set = new JavaMethod(moduleContext, method);
        } else if ("__setField".equals(methodName)) {
          __setField = new JavaMethod(moduleContext, method);
        }
      }
    }

    // Introspect public non-static fields
    Field[] fields = type.getFields();

    for (Field field : fields) {
      if (Modifier.isStatic(field.getModifiers()))
        continue;

      MarshalFactory factory = moduleContext.getMarshalFactory();
      Marshal marshal = factory.create(field.getType(), false);
      
      _fieldMap.put(new StringBuilderValue(field.getName()),
		    new FieldMarshalPair(field, marshal));
    }


   // introspectFields(quercus, type.getSuperclass());
  }

  /**
   * helper for introspectFields
   *
   * @param s (eg: Foo, URL)
   * @return (foo, URL)
   */
  private StringValue javaToQuercusConvert(String s)
  {
    if (s.length() == 1) {
      return new StringBuilderValue(new char[] {Character.toLowerCase(s.charAt(0))});
    }

    if (Character.isUpperCase(s.charAt(1)))
      return new StringBuilderValue(s);
    else {
      StringBuilderValue sb = new StringBuilderValue();
      sb.append(Character.toLowerCase(s.charAt(0)));

      int length = s.length();
      for (int i = 1; i < length; i++) {
        sb.append(s.charAt(i));
      }

      return sb;
    }
  }

  /**
   * Introspects the Java class.
   */
  private void introspectConstants(Class type)
  {
    if (type == null || type.equals(Object.class))
      return;

    if (! Modifier.isPublic(type.getModifiers()))
      return;

    Class []ifcs = type.getInterfaces();

    for (Class ifc : ifcs) {
      introspectConstants(ifc);
    }

    Field []fields = type.getDeclaredFields();

    for (Field field : fields) {
      if (_constMap.get(field.getName()) != null)
        continue;
      else if (! Modifier.isPublic(field.getModifiers()))
        continue;
      else if (! Modifier.isStatic(field.getModifiers()))
        continue;
      else if (! Modifier.isFinal(field.getModifiers()))
        continue;

      try {
        Value value = Quercus.objectToValue(field.get(null));

        if (value != null)
          _constMap.put(field.getName().intern(), value);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    introspectConstants(type.getSuperclass());
  }

  /**
   * Introspects the Java class.
   */
  private void introspectMethods(ModuleContext moduleContext, Class type)
  {
    if (type == null || type.equals(Object.class))
      return;

    Method []methods = type.getMethods();

    for (Method method : methods) {
      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if (method.getDeclaringClass() == Object.class)
        continue;

      if ("printRImpl".equals(method.getName())) {
        _printRImpl = method;
      } else if ("varDumpImpl".equals(method.getName())) {
        _varDumpImpl = method;
      } else if ("__call".equals(method.getName())) {
        __call = new JavaMethod(moduleContext, method);
      } else {
        if (method.getName().startsWith("quercus_"))
          throw new UnsupportedOperationException(L.l("{0}: use @Name instead", method.getName()));

        JavaMethod newFun = new JavaMethod(moduleContext, method);
        AbstractJavaMethod fun = _functionMap.get(newFun.getName());

        if (fun != null)
          fun = fun.overload(newFun);
        else
          fun = newFun;

        _functionMap.put(fun.getName(), fun);
      }
    }

    introspectMethods(moduleContext, type.getSuperclass());

    Class []ifcs = type.getInterfaces();

    for (Class ifc : ifcs) {
      introspectMethods(moduleContext, ifc);
    }
  }

  /**
   *
   * @return false if printRImpl not implemented
   * @throws IOException
   */
  public boolean printRImpl(Env env,
                               Object obj,
                               WriteStream out,
                               int depth,
                               IdentityHashMap<Value, String> valueSet)
    throws IOException
  {

    try {
      if (_printRImpl == null) {
        return false;

      }

      _printRImpl.invoke(obj, env, out, depth, valueSet);
      return true;
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  public boolean varDumpImpl(Env env,
                             Object obj,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    try {
      if (_varDumpImpl == null) {
	return false;
      }

      _varDumpImpl.invoke(obj, env, out, depth, valueSet);
      return true;
    } catch (Exception e) {
      throw new QuercusException(e);
    }
  }

  private class MethodMarshalPair {
    public Method _method;
    public Marshal _marshal;

    public MethodMarshalPair(Method method,
                              Marshal marshal)
    {
      _method = method;
      _marshal = marshal;
    }
  }

  private class FieldMarshalPair {
    public Field _field;
    public Marshal _marshal;

    public FieldMarshalPair(Field field,
                             Marshal marshal)
    {
      _field = field;
      _marshal = marshal;
    }
  }
  
  private static class LongClassDef extends JavaClassDef {
    LongClassDef(ModuleContext module)
    {
      super(module, "Long", Long.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return LongValue.create(((Number) obj).longValue());
    }
  }
  
  private static class DoubleClassDef extends JavaClassDef {
    DoubleClassDef(ModuleContext module)
    {
      super(module, "Double", Double.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return new DoubleValue(((Number) obj).doubleValue());
    }
  }
  
  private static class StringClassDef extends JavaClassDef {
    StringClassDef(ModuleContext module)
    {
      super(module, "String", String.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return env.createString((String) obj);
    }
  }
  
  private static class BooleanClassDef extends JavaClassDef {
    BooleanClassDef(ModuleContext module)
    {
      super(module, "Boolean", Boolean.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      if (Boolean.TRUE.equals(obj))
	return BooleanValue.TRUE;
      else
	return BooleanValue.FALSE;
    }
  }
  
  private static class CalendarClassDef extends JavaClassDef {
    CalendarClassDef(ModuleContext module)
    {
      super(module, "Calendar", Calendar.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return new JavaCalendarValue(env, (Calendar)obj, this);
    }
  }
  
  private static class DateClassDef extends JavaClassDef {
    DateClassDef(ModuleContext module)
    {
      super(module, "Date", Date.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return new JavaDateValue(env, (Date)obj, this);
    }
  }
  
  private static class URLClassDef extends JavaClassDef {
    URLClassDef(ModuleContext module)
    {
      super(module, "URL", URL.class);
    }

    @Override
    public Value wrap(Env env, Object obj)
    {
      return new JavaURLValue(env, (URL)obj, this);
    }
  }
}

