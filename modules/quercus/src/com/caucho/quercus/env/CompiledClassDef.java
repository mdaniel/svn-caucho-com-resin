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

import com.caucho.quercus.Location;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.util.IdentityIntMap;

import java.util.ArrayList;

/**
 * Represents a PHP class value.
 */
public class CompiledClassDef extends ClassDef {
  private final ArrayList<String> _fieldNames
    = new ArrayList<String>();

  private final IdentityIntMap _fieldMap
    = new IdentityIntMap();

  protected ArrayValue _extFields = new ArrayValueImpl();
  protected Value _parent;
  protected boolean _isFinal;

  public CompiledClassDef(String name,
                          String parent,
                          String []ifaceList)
  {
    super(null, name, parent, ifaceList, ClassDef.NULL_STRING_ARRAY);
  }

  public CompiledClassDef(String name,
                          String parent,
                          String []ifaceList,
                          String []traitList)
  {
    super(null, name, parent, ifaceList, traitList);
  }

  public CompiledClassDef(Location location,
                          String name,
                          String parent,
                          String []ifaceList)
  {
    super(location, name, parent, ifaceList, ClassDef.NULL_STRING_ARRAY);
  }

  public CompiledClassDef(Location location,
                          String name,
                          String parent,
                          String []ifaceList,
                          boolean isFinal)
  {
    this(location, name, parent, ifaceList, ClassDef.NULL_STRING_ARRAY, isFinal);
  }

  public CompiledClassDef(Location location,
                          String name,
                          String parent,
                          String []ifaceList,
                          String []traitList)
  {
    super(location, name, parent, ifaceList, traitList);
  }

  public CompiledClassDef(Location location,
                          String name,
                          String parent,
                          String []ifaceList,
                          String []traitList,
                          boolean isFinal)
  {
    super(location, name, parent, ifaceList, traitList);

    _isFinal = isFinal;
  }

  /**
   * Initialize the quercus class.
   */
  public void initClassDef()
  {
  }

  /**
   * Initialize the quercus class methods.
   */
  @Override
  public void initClassMethods(QuercusClass cl, String bindingClassName)
  {
  }

  /**
   * Initialize the quercus class fields.
   */
  @Override
  public void initClassFields(QuercusClass cl, String bindingClassName)
  {
  }
  /**
   * Returns true for a final class.
   */
  @Override
  public boolean isFinal()
  {
    return _isFinal;
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

  /**
   * Returns the field index.
   */
  protected void addFieldIndex(String name, int id)
  {
    _fieldMap.put(name, id);
    _fieldNames.add(name);
  }

  /**
   * Returns the constructor
   */
  @Override
  public AbstractFunction findConstructor()
  {
    return null;
  }

  /**
   * Creates a new instance.
   */
  public void initInstance(Env env, Value value)
  {
  }

  /**
   * Adds a value.
   */
  public Value get(Value name)
  {
    throw new UnsupportedOperationException();
    /*
    if (_extFields != null) {
      Value value = _extFields.get(name);
      return value;
    }
    else
      return NullValue.NULL;
    */
  }

  /**
   * Returns a reference to the field
   */
  public Value getRef(Value name)
  {
    throw new UnsupportedOperationException();
    /*
    if (_extFields == null)
      _extFields = new ArrayValue();

    Value ref = _extFields.getRef(name);

    return ref;
    */
  }

  /**
   * Returns a reference to the field
   */
  public Value getArgRef(Value name)
  {
    throw new UnsupportedOperationException();
    /*
    if (_extFields == null)
      _extFields = new ArrayValue();

    return _extFields.getArgRef(name);
    */
  }

  /**
   * Returns the field value, if unset, creates an array.
   */
  public Value getArray(Value name)
  {
    throw new UnsupportedOperationException();

    /*
    Value value = get(name);

    if (! value.isset()) {
      value = new ArrayValue();

      put(name, value);
    }

    return value;
    */
  }

  /**
   * Returns the field value, if unset, creates an object.
   */
  public Value getObject(Env env, Value name)
  {
    throw new UnsupportedOperationException();
    /*
    Value value = get(name);

    if (! value.isset()) {
      value = env.createObject();

      put(name, value);
    }

    return value;
    */
  }

  /**
   * Returns the field value, if unset, creates an ArgGetValue.
   */
  public Value getArg(Value name)
  {
    throw new UnsupportedOperationException();

    /*
    Value value = get(name);

    if (value.isset()) {
      return value;
    }
    else {
      // quercus/3d55
      return new ArgGetValue(this, name);
    }

    return null;
    */
  }

  /**
   * Adds a value.
   */
  public Value put(Value name, Value value)
  {
    throw new UnsupportedOperationException();

    /*
    if (_extFields == null)
      _extFields = new ArrayValue();

    _extFields.put(name, value);

    return value;
    */
  }

  /**
   * Adds a value.
   */
  public Value put(Value value)
  {
    throw new UnsupportedOperationException();

    /*
    if (_extFields == null)
      _extFields = new ArrayValue();

    _extFields.put(value);

    return value;
    */
  }

  /**
   * Adds a value.
   */
  public Value putRef()
  {
    throw new UnsupportedOperationException();

    /*
    // quercus/3d8i

    if (_extFields == null)
      _extFields = new ArrayValue();

    return _extFields.putRef();
    */
  }

  /**
   * Removes a value.
   */
  public Value remove(Value name)
  {
    throw new UnsupportedOperationException();
    /*
    // quercus/3d91

    if (_extFields != null) {
      Value value = _extFields.remove(name);
      return value;
    }
    else
      return NullValue.NULL;
    */
  }

  /**
   * Creates a new instance.
   */
  public Value newInstance()
  {
    /*
    try {
      return getClass().newInstance();
    } catch (Exception e) {
      throw new QuercusRuntimeException(e);
    }
    */
    throw new UnsupportedOperationException();
  }

  /**
   * Eval new
   */
  @Override
  public Value callNew(Env env, Expr []args)
  {
    return null;
  }

  /**
   * Eval new
   */
  @Override
  public Value callNew(Env env, Value []args)
  {
    return null;
  }
}

