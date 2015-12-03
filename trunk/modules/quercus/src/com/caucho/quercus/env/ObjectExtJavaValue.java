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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

/**
 * Represents a PHP object which extends a Java value.
 */
public class ObjectExtJavaValue extends ObjectExtValue
  implements Serializable
{
  private Object _object;
  private final JavaClassDef _javaClassDef;

  public ObjectExtJavaValue(Env env,
                            QuercusClass cl,
                            Object object,
                            JavaClassDef javaClassDef)
  {
    super(env, cl);

    _object = object;
    _javaClassDef = javaClassDef;
  }

  //
  // field
  //

  /**
   * Returns fields not explicitly specified by this value.
   */
  @Override
  protected Value getFieldExt(Env env, StringValue name)
  {
    if (_object == null) {
      _object = createJavaObject(env);
    }

    Value parentValue = super.getFieldExt(env, name);
    
    if (parentValue != NullValue.NULL && parentValue != UnsetValue.UNSET) {
      return parentValue;
    }

    Value value = _javaClassDef.getField(env, this, name);
    Value quercusValue = _quercusClass.getField(env,this, name);

    if (quercusValue != null
        && quercusValue != UnsetValue.UNSET 
        && quercusValue != NullValue.NULL) {
      return quercusValue;
    }

    if (value != null)
      return value;
    else
      return super.getFieldExt(env, name);
  }

  /**
   * Sets fields not specified by the value.
   */
  protected Value putFieldExt(Env env, StringValue name, Value value)
  {
    if (_object == null) {
      createJavaObject(env);
    }

    return _javaClassDef.putField(env, this, name, value);
  }

  /**
   * Returns the java object.
   */
  @Override
  public Object toJavaObject()
  {
    if (_object == null) {
      _object = createJavaObject(Env.getInstance());
    }

    return _object;
  }

  /**
   * Binds a Java object to this object.
   */
  @Override
  public void setJavaObject(Object obj)
  {
    _object = obj;
  }

  /**
   * Creats a backing Java object for this php object.
   */
  private Object createJavaObject(Env env)
  {
    Value javaWrapper = _javaClassDef.callNew(env, Value.NULL_ARGS);
    return javaWrapper.toJavaObject();
  }

  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (_object == null) {
      _object = createJavaObject(Env.getInstance());
    }

    if (! _javaClassDef.varDumpImpl(env, this, _object, out, depth, valueSet))
      super.varDumpImpl(env, out, depth, valueSet);
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (_object == null) {
      _object = createJavaObject(Env.getInstance());
    }

    _javaClassDef.printRImpl(env, _object, out, depth, valueSet);
  }

  /**
   * Converts to a string.
   * @param env
   */
  @Override
  public StringValue toString(Env env)
  {
    AbstractFunction toString = _quercusClass.getToString();

    if (toString != null) {
      return toString.callMethod(env, _quercusClass, this).toStringValue();
    }
    else if (_javaClassDef.getToString() != null) {
      JavaValue value = new JavaValue(env, _object, _javaClassDef);

      return _javaClassDef.toString(env, value);
    }
    else {
      return env.createString(_className + "[]");
    }
  }

  /**
   * Clone the object
   */
  @Override
  public Value clone(Env env)
  {
    Object obj = null;

    if (_object != null) {
      if (! (_object instanceof Cloneable)) {
        return env.error(L.l("Java class {0} does not implement Cloneable",
                             _object.getClass().getName()));
      }

      Class<?> cls = _javaClassDef.getType();

      try {
        Method method = cls.getMethod("clone", new Class[0]);
        method.setAccessible(true);

        obj = method.invoke(_object);
      }
      catch (NoSuchMethodException e) {
        throw new QuercusException(e);
      }
      catch (InvocationTargetException e) {
        throw new QuercusException(e.getCause());
      }
      catch (IllegalAccessException e) {
        throw new QuercusException(e);
      }
    }

    ObjectExtValue newObject
      = new ObjectExtJavaValue(env, _quercusClass, obj, _javaClassDef);

    clone(env, newObject);

    return newObject;
  }
}

