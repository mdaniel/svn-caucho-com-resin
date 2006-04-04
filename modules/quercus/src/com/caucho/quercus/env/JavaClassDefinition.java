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
import com.caucho.quercus.module.Construct;
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

  private final HashMap<String, MethodMarshallPair> _getMap
    = new HashMap<String, MethodMarshallPair>();

  private final HashMap<String, JavaMethod> _setMap
    = new HashMap<String, JavaMethod>();

  // _fieldMap stores all public non-static fields
  // used by getField and setField
  private final HashMap<String, FieldMarshallPair> _fieldMap
    = new HashMap<String, FieldMarshallPair> ();

  private Method __get = null;
  private Marshall __getReturn = null;

  private Method __getField = null;
  private Marshall __getFieldReturn = null;

  private Method __set = null;
  private Marshall __setName = null;
  private Marshall __setValue = null;

  private Method __setField = null;
  //private Marshall __setFieldName = null;
  private Marshall __setFieldValue = null;

  private Method _printRImpl = null;
  private Method _varDumpImpl = null;

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

  public Class getType()
  {
    return _type;
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
        Object result = __get.invoke(obj, name);
        return __getReturn.unmarshall(null, result);

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
    //Marshall marshall;

    MethodMarshallPair methodPair = _getMap.get(name);

    if (methodPair != null) {
      try {
        result = methodPair._method.invoke(obj);
        return methodPair._marshall.unmarshall(env, result);

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
          result = __getField.invoke(obj, name);
          return __getFieldReturn.unmarshall(env, result);

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
        // XXX: make sure expected class is never used
        Object marshalledName = __setName.marshall(env, name, null);

        Object marshalledValue = __setValue.marshall(env, value, null);

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
          //XXX: make sure expected Class Type is never used
          Object marshalledValue = __setFieldValue.marshall(env, value, null);
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

    if (cons.length > 0) {
      int i;
      for (i = 0; i < cons.length; i++) {
        if (cons[i].isAnnotationPresent(Construct.class))
          break;
      }

      if (i < cons.length)
        _cons = new JavaConstructor(_quercus, cons[i]);
      else
        _cons = new JavaConstructor(_quercus, cons[0]);

    } else
      _cons = null;

    try {
      Method method = _type.getMethod("iterator", new Class[0]);

      if (method != null &&
          Iterator.class.isAssignableFrom(method.getReturnType()))
        _iterator = method;
    } catch (Throwable e) {
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
        if (methodName.startsWith("get")) {
          Marshall marshall = Marshall.create(_quercus, method.getReturnType(), false);
          MethodMarshallPair pair = new MethodMarshallPair(method, marshall);
          _getMap.put(javaToQuercusConvert(methodName.substring(3, length)), pair);

        }
        else if (methodName.startsWith("is")) {
          Marshall marshall = Marshall.create(_quercus, method.getReturnType(), false);
          MethodMarshallPair pair = new MethodMarshallPair(method, marshall);
          _getMap.put(javaToQuercusConvert(methodName.substring(2, length)), pair);
        }
        else if (methodName.startsWith("set")) {
          JavaMethod javaMethod = new JavaMethod(quercus, method);
          _setMap.put(javaToQuercusConvert(methodName.substring(3, length)), javaMethod);

        } else if ("__get".equals(methodName)) {
          __get = method;
          __getReturn = Marshall.create(_quercus, __get.getReturnType(), false);

        } else if ("__getField".equals(methodName)) {
          __getField = method;
          __getFieldReturn = Marshall.create(_quercus, __getField.getReturnType(), false);

        } else if ("__set".equals(methodName)) {
          __set = method;
          __setName = Marshall.create(_quercus, __set.getParameterTypes()[0]);
          __setValue = Marshall.create(_quercus, __set.getParameterTypes()[1]);

        } else if ("__setField".equals(methodName)) {
          __setField = method;
          __setFieldValue = Marshall.create(_quercus, __setField.getParameterTypes()[1], false);

        }
      }
    }

    // Introspect public non-static fields
    Field[] fields = type.getFields();

    for (Field field : fields) {

      if (Modifier.isStatic(field.getModifiers()))
        continue;

      Marshall marshall = Marshall.create(_quercus,field.getType(), false);
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
      } else if ("varDumpImpl".equals(method.getName())) {
        _varDumpImpl = method;
      }else {
        JavaMethod javaMethod = new JavaMethod(quercus, method);

        _functionMap.put(method.getName(), javaMethod);
      }
    }

    introspectMethods(quercus, type.getSuperclass());
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
   * @throws Throwable
   */
  protected boolean printRImpl(Env env,
                               Object obj,
                               WriteStream out,
                               int depth,
                               IdentityHashMap<Value, String> valueSet)
    throws IOException, Throwable
  {

    if (_printRImpl == null) {
      return false;

    }

    _printRImpl.invoke(obj, env, out, depth, valueSet);
    return true;
  }

  public boolean varDumpImpl(Env env,
                             Object obj,
                             WriteStream out,
                             int depth,
                             IdentityHashMap<Value, String> valueSet)
    throws IOException, Throwable
  {
    if (_varDumpImpl == null) {
      return false;

    }

    _varDumpImpl.invoke(obj, env, out, depth, valueSet);
    return true;
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
}

