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
import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.Construct;
import com.caucho.quercus.module.JavaMarshall;
import com.caucho.quercus.module.Marshall;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Iterator;
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

  private final HashMap<String, Value> _constMap
    = new HashMap<String, Value>();

  private final HashMap<String, JavaMethod> _functionMap
    = new HashMap<String, JavaMethod>();

  private final HashMap<String, JavaMethod> _getMap
    = new HashMap<String, JavaMethod>();

  private final HashMap<String, JavaMethod> _setMap
    = new HashMap<String, JavaMethod>();

  // _fieldMap stores all public non-static fields
  // used by getField and setField
  private final HashMap<String, FieldMarshallPair> _fieldMap
    = new HashMap<String, FieldMarshallPair> ();

  private JavaMethod __get = null;

  private JavaMethod __getField = null;

  private JavaMethod __set = null;

  private JavaMethod __setField = null;

  private Method _printRImpl = null;
  private Method _varDumpImpl = null;

  private JavaInvoker _cons;

  private Method _iterator;

  private Marshall _marshall;

  private JavaClassDef(ModuleContext moduleContext, String name, Class type)
  {
    super(name, null);

    _moduleContext = moduleContext;

    _name = name;

    _type = type;
  }

  public static JavaClassDef create(ModuleContext moduleContext,
				    String name, Class type)
  {
    if (Double.class.isAssignableFrom(type) ||
	Float.class.isAssignableFrom(type))
      return new DoubleClassDef(moduleContext);
    else if (Number.class.isAssignableFrom(type))
      return new LongClassDef(moduleContext);
    else if (String.class.isAssignableFrom(type) ||
	     Character.class.isAssignableFrom(type))
      return new StringClassDef(moduleContext);
    else if (Boolean.class.isAssignableFrom(type))
      return new BooleanClassDef(moduleContext);
    else
      return new JavaClassDef(moduleContext, name, type);
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _name;
  }

  public Class getType()
  {
    return _type;
  }

  public Value wrap(Env env, Object obj)
  {
    return new JavaValue(env, obj, this);
  }

  /**
   * For array dereferencing.
   *
   * Also designed to call __get()
   * IE: SimpleXMLElement
   *
   * @param name
   * @return Value
   */
  public Value get(Env env, Object obj, Value name)
  {
    if (__get != null) {
      try {
       //__get needs to handle a Value $foo[5] vs. $foo['bar']
        return __get.eval(env, obj, name);
      } catch (Throwable e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return NullValue.NULL;
      }
    }

    return NullValue.NULL;
  }

  /**
   * @param name
   * @return Value attained through invoking getter
   */
  public Value getField(Env env, Object obj, String name)
  {
    Object result;

    JavaMethod get = _getMap.get(name);

    if (get != null) {
      try {
        return get.eval(env, obj);
      } catch (Throwable e) {
        log.log(Level.FINE, L.l(e.getMessage()), e);
        return NullValue.NULL;
      }
    } else {
      FieldMarshallPair fieldPair = _fieldMap.get(name);
      if (fieldPair != null) {
        try {
          result = fieldPair._field.get(obj);
          return fieldPair._marshall.unmarshall(env, result);
        } catch (Throwable e) {
          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;
        }
      } else if (__getField != null) {
        try {
          return __getField.eval(env, obj, new StringValueImpl(name));
        } catch (Throwable e) {
          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;
        }
      }
    }

    return NullValue.NULL;
  }

  /**
   * specifically designed for __set()
   *
   * @param env
   * @param obj
   * @param name
   * @param value
   * @return value
   */
  public Value put(Env env,
                   Object obj,
                   Value name,
                   Value value)
  {
    if (__set != null) {
      try {
        return __set.eval(env, obj, name, value);
      } catch (Throwable e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return NullValue.NULL;

      }
    }

    return NullValue.NULL;
  }

  public Value putField(Env env,
                        Object obj,
                        String name,
                        Value value)
  {
    JavaMethod setter = _setMap.get(name);

    if (setter != null) {
      try {
        return setter.eval(env, obj, value);
      } catch (Throwable e) {
        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return NullValue.NULL;
      }
    } else {
      FieldMarshallPair fieldPair = _fieldMap.get(name);
      if (fieldPair != null) {
        try {
          Class type = fieldPair._field.getType();
          Object marshalledValue = fieldPair._marshall.marshall(env, value, type);
          fieldPair._field.set(obj, marshalledValue);

          return value;

        } catch (Throwable e) {
          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;
        }
      } else if (__setField != null) {
        try {
          return __setField.eval(env, obj, new StringValueImpl(name), value);
        } catch (Throwable e) {
          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;

        }
      }
    }

    return NullValue.NULL;

  }

  /**
   * Returns the marshall instance.
   */
  public Marshall getMarshall()
  {
    return _marshall;
  }

  /**
   * Creates a new instance.
   */
  public Value newInstance(Env env, QuercusClass qClass)
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
  public Value evalNew(Env env, Expr []args)
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Value []args)
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Expr []args)
  {
    JavaMethod method = _functionMap.get(name);

    if (method == null) {
      env.warning(env.getLocation().getMessagePrefix() + L.l("{0}::{1} is an unknown method.",
                                          _name, name));

      return NullValue.NULL;
    }

    return method.eval(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Value value, String name, Expr []args)
  {
    return evalMethod(env, value.toJavaObject(), name, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Value value, String name, Value []args)
  {
    return evalMethod(env, value.toJavaObject(), name, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value []args)
  {
    return getMethod(env, name).eval(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name)
  {
    return getMethod(env, name).eval(env, obj);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value a1)
  {
    return getMethod(env, name).eval(env, obj, a1);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2)
  {
    return getMethod(env, name).eval(env, obj, a1, a2);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3)
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3, a4);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3, a4, a5);
  }

  /**
   * Returns the values for an iterator.
   */
  public Value []getValueArray(Env env, Object obj)
  {
    try {
      if (_iterator == null)
        return new Value[0];

      Iterator iter = (Iterator) _iterator.invoke(obj);

      ArrayList<Value> values = new ArrayList<Value>();

      while (iter.hasNext()) {
        Object objValue = iter.next();

        if (objValue instanceof Value)
          values.add((Value) objValue);
        else
          values.add(env.wrapJava(objValue));
      }

      Value []valueArray = new Value[values.size()];

      values.toArray(valueArray);

      return valueArray;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private JavaMethod getMethod(Env env, String name)
  {
    JavaMethod method = _functionMap.get(name);

    if (method == null) {
      env.error("'" + name + "' is an unknown method.");
    }

    return method;
  }

  /**
   * Initialize the quercus class.
   */
  public void initClass(QuercusClass cl)
  {
    if (_cons != null)
      cl.setConstructor(_cons);

    if (__get != null)
      cl.setGet(__get);
    
    for (Map.Entry<String,JavaMethod> entry : _functionMap.entrySet()) {
      cl.addMethod(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String,Value> entry : _constMap.entrySet()) {
      cl.addConstant(entry.getKey(), entry.getValue());
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
  public AbstractFunction findConstructor()
  {
    return null;
  }

  /**
   * Introspects the Java class.
   */
  public void introspect(ModuleContext moduleContext)
  {
    introspectConstants(_type);
    introspectMethods(moduleContext, _type);
    introspectFields(moduleContext, _type);

    _marshall = new JavaMarshall(this, false);

    Method consMethod = getConsMethod(_type);

    if (consMethod != null)
      _cons = new JavaMethod(moduleContext, consMethod);
    else {
      Constructor []cons = _type.getConstructors();

      if (cons.length > 0) {
	int i;
	for (i = 0; i < cons.length; i++) {
	  if (cons[i].isAnnotationPresent(Construct.class))
	    break;
	}

	if (i < cons.length)
	  _cons = new JavaConstructor(moduleContext, cons[i]);
	else
	  _cons = new JavaConstructor(moduleContext, cons[0]);

      } else
	_cons = null;
    }

    try {
      Method method = _type.getMethod("iterator", new Class[0]);

      if (method != null &&
          Iterator.class.isAssignableFrom(method.getReturnType()))
        _iterator = method;
    } catch (Throwable e) {
    }
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
          _getMap.put(javaToQuercusConvert(methodName.substring(3, length)),
		      new JavaMethod(moduleContext, method));

        }
        else if (methodName.startsWith("is")) {
          _getMap.put(javaToQuercusConvert(methodName.substring(2, length)),
		      new JavaMethod(moduleContext, method));
        }
        else if (methodName.startsWith("set")) {
          JavaMethod javaMethod = new JavaMethod(moduleContext, method);
          _setMap.put(javaToQuercusConvert(methodName.substring(3, length)),
		      new JavaMethod(moduleContext, method));

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

      Marshall marshall = Marshall.create(moduleContext,
					  field.getType(), false);
      _fieldMap.put(field.getName(), new FieldMarshallPair(field, marshall));
    }


   // introspectFields(quercus, type.getSuperclass());
  }

  /**
   * helper for introspectFields
   *
   * @param s (IE: Foo, URL)
   * @return (foo, URL)
   */
  private String javaToQuercusConvert(String s)
  {
    if (s.length() == 1) {
      return new String(new char[] {Character.toLowerCase(s.charAt(0))});
    }

    if (Character.isUpperCase(s.charAt(1)))
      return s;
    else {
      StringBuilder sb = new StringBuilder();
      sb.append(Character.toLowerCase(s.charAt(0)));

      int length = s.length();
      for (int i = 1; i < length; i++) {
        sb.append(s.charAt(i));
      }

      return sb.toString();
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

    Class []ifcs = type.getInterfaces();

    for (Class ifc : ifcs) {
      introspectMethods(moduleContext, ifc);
    }

    Method []methods = type.getDeclaredMethods();

    for (Method method : methods) {
      if (_functionMap.get(method.getName()) != null)
        continue;
      else if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if ("printRImpl".equals(method.getName())) {
        _printRImpl = method;
      } else if ("varDumpImpl".equals(method.getName())) {
        _varDumpImpl = method;
      }else {
        JavaMethod javaMethod = new JavaMethod(moduleContext, method);

        _functionMap.put(method.getName(), javaMethod);
      }
    }

    introspectMethods(moduleContext, type.getSuperclass());
  }

  /**
   *
   * @param env
   * @param obj
   * @param out
   * @param depth
   * @param valueSet
   * @return false if printRImpl not implemented
   * @throws IOException
   */
  protected boolean printRImpl(Env env,
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

  private class MethodMarshallPair {
    public Method _method;
    public Marshall _marshall;

    public MethodMarshallPair(Method method,
                              Marshall marshall)
    {
      _method = method;
      _marshall = marshall;
    }
  }

  private class FieldMarshallPair {
    public Field _field;
    public Marshall _marshall;

    public FieldMarshallPair(Field field,
                             Marshall marshall)
    {
      _field = field;
      _marshall = marshall;
    }
  }
  
  private static class LongClassDef extends JavaClassDef {
    LongClassDef(ModuleContext module)
    {
      super(module, "Long", Long.class);
    }

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

    public Value wrap(Env env, Object obj)
    {
      return new StringValueImpl((String) obj);
    }
  }
  
  private static class BooleanClassDef extends JavaClassDef {
    BooleanClassDef(ModuleContext module)
    {
      super(module, "Boolean", Boolean.class);
    }

    public Value wrap(Env env, Object obj)
    {
      if (Boolean.TRUE.equals(obj))
	return BooleanValue.TRUE;
      else
	return BooleanValue.FALSE;
    }
  }
}

