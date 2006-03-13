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

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.JavaMarshall;
import com.caucho.quercus.module.Marshall;
import com.caucho.util.L10N;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an introspected Java class.
 */
public class JavaClassDefinition extends AbstractQuercusClass {
  private final static Logger log
    = Logger.getLogger(JavaClassDefinition.class.getName());
  private final static L10N L = new L10N(JavaClassDefinition.class);

  private final Quercus _quercus;

  private final String _name;
  private final Class _type;

  private final HashMap<String, Value> _constMap
    = new HashMap<String, Value>();

  private final HashMap<String, JavaMethod> _functionMap
    = new HashMap<String, JavaMethod>();

  private final HashMap<String, Method> _getMap
    = new HashMap<String, Method>();

  private final HashMap<String, JavaMethod> _setMap
    = new HashMap<String, JavaMethod>();

  // _fieldMap stores all public non-static fields
  // used by getField and setField
  private final HashMap<String, Field> _fieldMap
    = new HashMap<String, Field> ();

  private Method __get = null;
  private Method __getField = null;
  private Method __set = null;
  private Method __setField = null;
  private Method _printRImpl = null;

  private JavaConstructor _cons;

  private Method _iterator;

  private Marshall _marshall;

  public JavaClassDefinition(Quercus quercus, String name, Class type)
  {
    _quercus = quercus;

    _name = name;

    _type = type;
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _name;
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
 /* Object result;

    if (obj instanceof Map) {
      Object index;
      // Marshall the index
      if (name instanceof LongValue) {
        index = name.toInt();
      } else if (name instanceof StringValueImpl) {
        index = name.toString();
      } else if (name instanceof JavaValue) {
        index = name.toJavaObject();
      } else {
        //XXX: need to figure out what we should do if name is not 
        //one of the above
        index = name.toString();
      }

      result = ((Map) obj).get(index);
      if (result != null)
        return env.wrapJava(result);
    }

    if (obj instanceof List) {
      int index = name.toInt();
      result = ((List) obj).get(index);
      if (result != null)
        return env.wrapJava(result);
    }
    */
    if (__get != null) {

      try {
       //__get needs to handle a Value $foo[5] vs. $foo['bar']
        Object result = __get.invoke(obj, name);
        Marshall marshall = Marshall.create(_quercus, __get.getReturnType(), false);
        return marshall.unmarshall(null, result);
        
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
  public Value getField(Env env, String name, Object obj)
  {
    Object result;
    Marshall marshall;

    Method getter = _getMap.get(name);

    if (getter != null) {

      try {

        result = getter.invoke(obj);
        marshall = Marshall.create(_quercus, getter.getReturnType(), false);
        return marshall.unmarshall(env, result);

      } catch (Throwable e) {

        log.log(Level.FINE, L.l(e.getMessage()), e);
        return NullValue.NULL;

      }

    } else {

      Field field = _fieldMap.get(name);
      if (field != null) {


        try {

          result = field.get(obj);
          marshall = Marshall.create(_quercus, field.getType(), false);
          return marshall.unmarshall(env, result);

        } catch (Throwable e) {

          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;

        }

      } else if (__getField != null) {
        try {

          result = __getField.invoke(obj, name);
          marshall = Marshall.create(_quercus, result.getClass(), false);
          return marshall.unmarshall(env, result);

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
        Class type;
        Marshall marshall;
        Object marshalledName, marshalledValue;

        type = __set.getParameterTypes()[0];
        marshall = Marshall.create(_quercus, type, false);
        marshalledName = marshall.marshall(env, name, type);

        type = __set.getParameterTypes()[1];
        marshall = Marshall.create(_quercus, type, false);
        marshalledValue = marshall.marshall(env, value, type);

        __set.invoke(obj, marshalledName, marshalledValue);

        return value;

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

   /* if (setter == null) {
      
  log.log(Level.FINE,"'" + name + "' is an unknown field.");
  return NullValue.NULL;*/

    if (setter != null) {

      try {

        return setter.eval(env, obj, value);

      } catch (Throwable e) {

        log.log(Level.FINE,  L.l(e.getMessage()), e);
        return NullValue.NULL;

      }

    } else {

      Field field = _fieldMap.get(name);
      if (field != null) {

        try {

          Class type = field.getType();
          Marshall marshall = Marshall.create(_quercus, type, false);
          Object marshalledValue = marshall.marshall(env, value, type);
          field.set(obj, marshalledValue);

          return value;

        } catch (Throwable e) {
          log.log(Level.FINE,  L.l(e.getMessage()), e);
          return NullValue.NULL;
        }

      } else if (__setField != null) {

        try {

          Class type = __setField.getParameterTypes()[1];
          Marshall marshall = Marshall.create(_quercus, type, false);
          Object marshalledValue = marshall.marshall(env, value, type);
          __setField.invoke(obj, env, name, marshalledValue);

          return value;

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
  public Value newInstance()
  {
    try {
      //Object obj = _type.newInstance();

      return new JavaValue(null, _type.newInstance(), this);
    } catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Expr []args)
    throws Throwable
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Value []args)
    throws Throwable
  {
    return _cons.eval(env, null, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Expr []args)
    throws Throwable
  {
    JavaMethod method = _functionMap.get(name);

    if (method == null) {
      env.warning(env.getLocation() + L.l("{0}::{1} is an unknown method.",
                                          _name, name));

      return NullValue.NULL;
    }

    return method.eval(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value []args)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, args);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name, Value a1)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return getMethod(env, name).eval(env, obj, a1, a2, a3, a4);
  }

  /**
   * Eval a method
   */
  public Value evalMethod(Env env, Object obj, String name,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
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
   * Finds the matching constant
   */
  public Value findConstant(Env env, String name)
  {
    return _constMap.get(name);
  }

  /**
   * Introspects the Java class.
   */
  public void introspect(Quercus quercus)
  {
    introspectConstants(_type);
    introspectMethods(quercus, _type);
    introspectFields(quercus, _type);

    _marshall = new JavaMarshall(this, false);

    Constructor []cons = _type.getConstructors();

    if (cons.length > 0)
      _cons = new JavaConstructor(_quercus, cons[0]);
    else
      _cons = null;

    try {
      Method method = _type.getMethod("iterator", new Class[0]);

      if (method != null &&
          Iterator.class.isAssignableFrom(method.getReturnType()))
        _iterator = method;
    } catch (Throwable e) {
      log.log(Level.FINE, L.l(e.getMessage()), e);
    }
  }

  /**
   * Introspects the Java class.
   */
  private void introspectFields(Quercus quercus, Class type)
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
        String prefix = methodName.substring(0, 3);

        if ("get".equals(prefix)) {

          _getMap.put(javaToPhpConvert(methodName.substring(3,length)), method);

        } else if ("set".equals(prefix)) {

          JavaMethod javaMethod = new JavaMethod(quercus, method);
          _setMap.put(javaToPhpConvert(methodName.substring(3, length)), javaMethod);

        } else if ("__get".equals(methodName)) {

          __get = method;

        } else if ("__getField".equals(methodName)) {

          __getField = method;

        } else if ("__set".equals(methodName)) {

          __set = method;

        } else if ("__setField".equals(methodName)) {

          __setField = method;

        }
      }
    }

    // Introspect public non-static fields
    Field[] fields = type.getFields();

    for (Field field : fields) {

      if (Modifier.isStatic(field.getModifiers()))
        continue;

      _fieldMap.put(field.getName(), field);
    }


   // introspectFields(quercus, type.getSuperclass());
  }

  /**
   * helper for introspectFields
   * 
   * @param s (IE: Foo, URL)
   * @return (foo, URL)
   */
  private String javaToPhpConvert(String s)
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
          _constMap.put(field.getName(), value);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    introspectConstants(type.getSuperclass());
  }

  /**
   * Introspects the Java class.
   */
  private void introspectMethods(Quercus quercus, Class type)
  {
    if (type == null || type.equals(Object.class))
      return;

    Class []ifcs = type.getInterfaces();

    for (Class ifc : ifcs) {
      introspectMethods(quercus, ifc);
    }

    Method []methods = type.getDeclaredMethods();

    for (Method method : methods) {
      if (_functionMap.get(method.getName()) != null)
        continue;
      else if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if ("printRImpl".equals(method.getName())) {
        _printRImpl = method;
      } else {
        JavaMethod javaMethod = new JavaMethod(quercus, method);
  
        _functionMap.put(method.getName(), javaMethod);
      }
    }

    introspectMethods(quercus, type.getSuperclass());
  }

  public void printRImpl(Env env,
                         Object obj,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException, Throwable
  {
    
    if (_printRImpl == null) {
      env.error("need to implement printRImpl(Env, WriteStream, int, IdentityHashMap<Value, String> in order to use print_r");
    }
    
    _printRImpl.invoke(obj, env, out, depth, valueSet);
  }
}

