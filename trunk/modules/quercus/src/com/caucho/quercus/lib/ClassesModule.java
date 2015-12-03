/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.ReturnNullAsFalse;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.ClassField;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.util.L10N;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Quercus class information
 */
public class ClassesModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(ClassesModule.class);

  /**
   * Calls an object method.
   */
  public static Value call_user_method(Env env,
                                       StringValue name,
                                       Value obj,
                                       Value []args)
  {
    if (obj.isObject()) {
      return obj.callMethod(env, name, args);
    }
    else {
      QuercusClass cls = env.getClass(obj.toString());

      Value result
        = cls.callMethod(env, env.getThis(), name, name.hashCode(), args);

      return result.copyReturn();
    }
  }

  /**
   * Calls a object method with arguments in an array.
   */
  public static Value call_user_method_array(Env env,
                                             StringValue methodName,
                                             Value obj,
                                             ArrayValue params)
  {
    Value []args = params.valuesToArray();

    return call_user_method(env, methodName, obj, args);
  }

  /**
   * bool class_alias ( string $original , string $alias [, bool $autoload = TRUE ] )
   */
  public boolean class_alias(Env env,
                             String original, String alias,
                             @Optional("true") boolean isAutoLoad)
  {
    QuercusClass aliasCls = env.findClass(alias, -1, false, true, true);

    if (aliasCls != null) {
      env.warning(L.l("cannot redeclare class {0}", alias));

      return false;
    }

    QuercusClass cls = env.findClass(original, -1, isAutoLoad, true, true);

    if (cls == null) {
      env.warning(L.l("original class not found {0}", original));

      return false;
    }

    env.addClassAlias(alias, cls);

    return true;
  }

  /**
   * returns true if the class exists.
   */
  public static boolean class_exists(Env env,
                                     String className,
                                     @Optional("true") boolean useAutoload)
  {
    if (className == null)
      return false;

    QuercusClass cl =  env.findClass(className, -1, useAutoload, true, true);

    // php/[03]9m1
    return cl != null && ! cl.isInterface();
  }

  /**
   * Returns the object's class name
   */
  public static Value get_class(Env env, Value value)
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      return env.createString(obj.getName());
    }
    else if (value instanceof JavaValue) {
      JavaValue obj = (JavaValue) value;

      return env.createString(obj.getClassName());
    }
    else {
      env.warning(L.l("expected an object, but saw {0}", value.getType()));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the calling class name.
   */
  @ReturnNullAsFalse
  public static String get_called_class(Env env)
  {
    Value qThis = env.getThis();

    if (qThis == null || qThis.getQuercusClass() == null) {
      env.warning("get_called_class was not called from a class method");
      return null;
    }

    return qThis.getQuercusClass().getName();
  }

  /**
   * Returns an array of method names
   *
   * @param clss the name of the class, or an instance of a class
   *
   * @return an array of method names
   */
  public static Value get_class_methods(Env env, Value cls)
  {
    // php/1j11

    QuercusClass cl;

    if (cls.isObject())
      cl = ((ObjectValue) cls).getQuercusClass();
    else
      cl = env.findClass(cls.toString());

    if (cl == null)
      return NullValue.NULL;

    ArrayValue array = new ArrayValueImpl();

    HashSet<String> set = new HashSet<String>();

    // to combine __construct and class name constructors
    for (AbstractFunction fun : cl.getClassMethods()) {
      if (fun.isPublic())
        set.add(fun.getName());
    }

    for (String name : set) {
      array.put(name);
    }

    return array;
  }

  /**
   * Returns an array of member names and values
   *
   * @param clss the name of the class, or an instance of a class
   *
   * @return an array of member names and values
   */
  public static Value get_class_vars(Env env, Value obj)
  {
    // php/1j10

    QuercusClass cl;

    if (obj instanceof ObjectValue)
      cl = ((ObjectValue) obj).getQuercusClass();
    else
      cl = env.findClass(obj.toString());

    if (cl == null)
      return BooleanValue.FALSE;

    ArrayValue varArray = new ArrayValueImpl();

    for (ClassField field : cl.getClassFields().values()) {
      if (field.isPublic()) {
        StringValue name = field.getName();
        Expr initValue = field.getInitExpr();

        Value value = initValue.eval(env);

        varArray.append(name, value);
      }
    }

    ArrayModule.ksort(env, varArray, ArrayModule.SORT_STRING);

    return varArray;
  }

  /**
   * Returns the declared classes
   */
  public static Value get_declared_classes(Env env)
  {
    return env.getDeclaredClasses();
  }

  // XXX: get_declared_interfaces

  /**
   * Returns the object's variables
   */
  public static Value get_object_vars(Env env, Value obj)
  {
    ArrayValue result = new ArrayValueImpl();

    // #3253, php/4as7 - XXX: needs cleanup

    if (obj instanceof ObjectValue) {
      for (Map.Entry<Value,Value> entry : ((ObjectValue) obj).entrySet()) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    else {
      Iterator<Map.Entry<Value,Value>> iter = obj.getIterator(env);

      while (iter.hasNext()) {
        Map.Entry<Value,Value> entry = iter.next();

        result.put(entry.getKey(), entry.getValue());
      }
    }

    return result;
  }

  /**
   * Returns the object's class name
   */
  public Value get_parent_class(Env env, @ReadOnly Value value)
  {
    if (value instanceof ObjectValue) {
      ObjectValue obj = (ObjectValue) value;

      String parent = obj.getParentClassName();

      if (parent != null)
        return env.createString(parent);
    }
    else if (value.isString()) {
      String className = value.toString();

      QuercusClass cl = env.findClass(className);

      if (cl != null) {
        String parent = cl.getParentName();

        if (parent != null)
          return env.createString(parent);
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns true if the class exists.
   */
  public boolean interface_exists(Env env,
                                  String interfaceName,
                                  @Optional("true") boolean useAutoload)
  {
    QuercusClass cl =  env.findClass(interfaceName, -1, useAutoload, true, true);

    // php/[03]9m0
    return cl != null && cl.isInterface();
  }

  /**
   * Returns true if the object implements the given class.
   */
  public static boolean is_a(Env env, @ReadOnly Value value, String name)
  {
    return value.isA(env, name);
  }

  /**
   * Returns true if the argument is an object.
   */
  public static boolean is_object(@ReadOnly Value value)
  {
    return value.isObject();
  }

  /**
   * Returns true if the object implements the given class.
   */
  public static boolean is_subclass_of(Env env,
                                       @ReadOnly Value value,
                                       String name)
  {
    if (value.isString()) {
      QuercusClass cl = env.findClass(value.toString());

      return cl.isA(env, name) && ! cl.getName().equalsIgnoreCase(name);
    }
    else {
      return value.isA(env, name) && ! value.getClassName().equalsIgnoreCase(name);
    }
  }

  /**
   * Returns true if the named method exists on the object.
   *
   * @param obj the object to test
   * @param methodName the name of the method
   */
  public static boolean method_exists(Env env,
                                      Value obj,
                                      StringValue methodName)
  {
    if (obj instanceof Closure && methodName.equalsString("__invoke")) {
      // XXX: Closure needs to be a proper PHP object

      return true;
    }

    QuercusClass qClass = obj.getQuercusClass();


    if (qClass == null) {
      qClass = env.findClass(obj.toString());
    }

    if (qClass != null) {
      return qClass.findFunction(methodName) != null;
    }
    else {
      return false;
    }
  }

  /**
   * Returns true if the named property exists on the object.
   */
  public static Value property_exists(Env env,
                                      Value obj,
                                      StringValue name)
  {
    if (obj.isString()) {
      QuercusClass cls = env.findClass(obj.toString());

      if (cls != null && cls.getClassField(name) != null)
        return BooleanValue.TRUE;
      else
        return BooleanValue.FALSE;
    }
    else if (obj.isObject()) {
      boolean result = obj.isFieldExists(env, name);

      return BooleanValue.create(result);
    }
    else {
      env.warning("must pass in object or name of class");
      return NullValue.NULL;
    }
  }

}
