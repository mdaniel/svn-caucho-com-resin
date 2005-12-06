/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.io.IOException;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AbstractClassDef;
import com.caucho.quercus.program.Function;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a PHP class value.
 */
abstract public class CompiledClass extends AbstractClassDef {
  protected ArrayValue _extFields = new ArrayValueImpl();
  protected Value _parent;
  
  public CompiledClass(String name, String parent)
  {
    super(name, parent);
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
   * Returns the field value, if unset, creates an ArgObjectGetValue.
   */
  public Value getArgObject(Env env, Value name)
  {
    throw new UnsupportedOperationException();
    
    /*
    Value value = get(name);

    if (value.isset()) {
      return value;
    }
    else {
      return new ArgObjectGetValue(env, this, name);
    }
    */
  }

  /**
   * Returns the field value, if unset, creates an ArgArrayGetValue
   */
  public Value getArgArray(Env env, Value name)
  {
    throw new UnsupportedOperationException();

    /*
    Value value = get(name);

    if (value.isset()) {
      return value;
    }
    else {
      return new ArgObjectGetValue(env, this, name);
    }
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
  public Value evalNew(Env env, Expr []args)
    throws Throwable
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Eval new
   */
  public Value evalNew(Env env, Value []args)
    throws Throwable
  {
    throw new UnsupportedOperationException();
    // return this;
  }
}

