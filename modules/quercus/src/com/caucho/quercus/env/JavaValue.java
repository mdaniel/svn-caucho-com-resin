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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents a Quercus java value.
 */
public class JavaValue
  extends ObjectValue
  implements Serializable
{
  private static final Logger log
    = Logger.getLogger(JavaValue.class.getName());

  private JavaClassDef _classDef;

  private Object _object;

  protected Env _env;

  public JavaValue(Env env, Object object, JavaClassDef def)
  {
    super(env.createQuercusClass(def, null));

    _env = env;
    _classDef = def;
    _object = object;
  }

  @Override
  public String getClassName()
  {
    return _classDef.getName();
  }

  public Set<Map.Entry<String, Value>> entrySet()
  {
    // XXX:: remove entrySet() method from ObjectValue
    throw new UnsupportedOperationException("unimplementated");
  }

  @Override
  protected void printRImpl(Env env,
                            WriteStream out,
                            int depth,
                            IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    if (! _classDef.printRImpl(env, _object, out, depth, valueSet))
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
  public Value getField(Env env, String name, boolean create)
  {
    return _classDef.getField(env, _object, name, create);
  }

  @Override
  public Value putField(Env env,
                        String name,
                        Value value)
  {
    return _classDef.putField(env, _object, name, value);
  }

  /**
   * Converts to a key.
   */
  @Override
  public Value toKey()
  {
    return new LongValue(System.identityHashCode(this));
  }

  /**
   * Returns the method.
   */
  @Override
  public AbstractFunction findFunction(String methodName)
  {
    return _classDef.findFunction(methodName);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Expr []args)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen,
                                args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env,
                          int hash, char []name, int nameLen,
                          Value []args)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a1)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a1, Value a2)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen,
                                a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethod(Env env, int hash, char []name, int nameLen,
                          Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen,
                                a1, a2, a3, a4, a5);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Expr []args)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value []args)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, args);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a1)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a1, Value a2)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1, a2);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen, a1, a2, a3);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen,
                                a1, a2, a3, a4);
  }

  /**
   * Evaluates a method.
   */
  @Override
  public Value callMethodRef(Env env, int hash, char []name, int nameLen,
                             Value a1, Value a2, Value a3, Value a4, Value a5)
  {
    return _classDef.callMethod(env, _object, hash, name, nameLen,
                                a1, a2, a3, a4, a5);
  }

  /**
   * Serializes the value.
   */
  @Override
  public void serialize(StringBuilder sb)
  {
    log.fine("Quercus: can't serialize " + _object.getClass());

    sb.append("N;");
  }

  /**
   * Converts to a string.
   */
  @Override
  public String toString()
  {
    // php/1x0b
    return String.valueOf(_object);
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
    Class objClass = _object.getClass();
    
    if (objClass == type || type.isAssignableFrom(objClass)) {
      return _object;
    } else {
      env.warning(L.l("Can't assign {0} to {1}",
		      objClass.getName(), type.getName()));
    
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

  //
  // Java Serialization
  //

  private void writeObject(ObjectOutputStream out)
    throws IOException
  {
    out.writeObject(_classDef.getType().getCanonicalName());
    
    out.writeObject(_object);
  }

  private void readObject(ObjectInputStream in)
    throws ClassNotFoundException, IOException
  {
    _env = Env.getInstance();
    
    _classDef = _env.getJavaClassDefinition((String) in.readObject());
    
    _object = in.readObject();
  }

}

