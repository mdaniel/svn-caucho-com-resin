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
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a Quercus java value.
 */
public class JavaValue extends ResourceValue {
  private static final Logger log
    = Logger.getLogger(JavaValue.class.getName());
  
  private final JavaClassDef _classDef;

  private final Object _object;

  protected final Env _env;

  public JavaValue(Env env, Object object, JavaClassDef def)
  {
    _env = env;
    _classDef = def;
    _object = object;
  }

  public String getClassName()
  {
    return _classDef.getName();
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    // if (! _classDef.printRImpl(env, _object, out, depth, valueSet))
    super.printRImpl(env, out, depth, valueSet);
  }
  
  @Override
  protected void varDumpImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (!_classDef.varDumpImpl(env, _object, out, depth, valueSet))
      super.varDumpImpl(env, out, depth, valueSet);
  }
  
  @Override
  public Value get(Value name)
  {
    return _classDef.get(_env, _object, name);
  }

  @Override
  public Value put(Value index, Value value)
  {
    return _classDef.put(_env, _object, index, value);
  }

  @Override
  public Value getField(Env env, String name)
  {
    return _classDef.getField(env, _object, name);
  }

  @Override
  public Value putField(Env env,
                        String name,
                        Value value)
  {
    return _classDef.putField(env, _object, name, value);
  }

  /**
   * Returns the class name.
   */
  public String getName()
  {
    return _classDef.getName();
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return "object";
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return true;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return 1;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return toLong();
  }

  /**
   * Converts to a key.
   */
  public Value toKey()
  {
    return new LongValue(System.identityHashCode(this));
  }

  public boolean isA(String name)
  {
    return _classDef.isA(name);
  }

  /**
   * Returns the method.
   */
  public AbstractFunction findFunction(String methodName)
  {
    return _classDef.findFunction(methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Expr []args)
  {
    return _classDef.callMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value []args)
  {
    return _classDef.callMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName)
  {
    return _classDef.callMethod(env, _object, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a1)
  {
    return _classDef.callMethod(env, _object, methodName, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName, Value a1, Value a2)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethod(Env env, String methodName,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Expr []args)
  {
    return _classDef.callMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value []args)
  {
    return _classDef.callMethod(env, _object, methodName, args);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName)
  {
    return _classDef.callMethod(env, _object, methodName);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a1)
  {
    return _classDef.callMethod(env, _object, methodName, a1);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName, Value a1, Value a2)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                          Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  public Value callMethodRef(Env env, String methodName,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, _object, methodName, a1, a2, a3, a4, a5);
  }

  /**
   * Returns the iterator values.
   */
  public Value []getValueArray(Env env)
  {
    return _classDef.getValueArray(env, _object);
  }

  /**
   * Serializes the value.
   */
  public void serialize(StringBuilder sb)
  {
    log.fine("Quercus: can't serialize " + _object.getClass());

    sb.append("N;");
  }

  /**
   * Converts to a string.
   */
  public String toString()
  {
    String result = String.valueOf(_object);
    int endOfClassName = result.indexOf("@");

    if (endOfClassName > 0)
      return result.substring(0,endOfClassName);
    else
      return result;
  }


  /**
   * Converts to an object.
   */
  @Override
  public Object toJavaObject()
  {
    return _object;
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObject(Env env, Class type)
  {
    if (type.isAssignableFrom(_object.getClass())) {
      return _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
		      _object.getClass().getName(), type.getName()));
    
      return null;
    }
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Object toJavaObjectNotNull(Env env, Class type)
  {
    if (type.isAssignableFrom(_object.getClass())) {
      return _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
		      _object.getClass().getName(), type.getName()));
    
      return null;
    }
  }

  /**
   * Converts to a java object.
   */
  @Override
  public Map toJavaMap(Env env, Class type)
  {
    if (type.isAssignableFrom(_object.getClass())) {
      return (Map) _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
		      _object.getClass().getName(), type.getName()));
    
      return null;
    }
  }

  /**
   * Converts to an object.
   */
  @Override
  public InputStream toInputStream()
  {
    if (_object instanceof InputStream)
      return (InputStream) _object;
    else
      return super.toInputStream();
  }
}

