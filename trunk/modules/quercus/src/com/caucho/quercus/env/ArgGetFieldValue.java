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

/**
 * Represents an field-get argument which might be a call to a reference.
 */
public class ArgGetFieldValue extends ArgValue {
  private final Env _env;
  private final Value _obj;
  private final StringValue _name;

  public ArgGetFieldValue(Env env, Value obj, StringValue name)
  {
    _env = env;
    _obj = obj;
    _name = name;
  }

  /**
   * Creates an argument which may create the given field.
   */
  @Override
  public Value getArg(Value name, boolean isTop)
  {
    // php/3d1q
    return new ArgGetValue(this, name);
  }

  /**
   * Creates an argument which may create the given field.
   */
  @Override
  public Value getFieldArg(Env env, StringValue name, boolean isTop)
  {
    // php/3d2q
    return new ArgGetFieldValue(env, this, name);
  }

  /**
   * Converts to a reference variable.
   */
  @Override
  public Var toLocalVarDeclAsRef()
  {
    // php/3d2t
    return _obj.toAutoObject(_env).getFieldVar(_env, _name).toLocalVarDeclAsRef();
  }

  /**
   * Converts to a value.
   */
  @Override
  public Value toValue()
  {
    return _obj.getField(_env, _name);
  }

  /**
   * Converts to a read-only function argument.
   */
  @Override
  public Value toLocalValueReadOnly()
  {
    return toValue();
  }

  /**
   * Converts to a function argument.
   */
  @Override
  public Value toLocalValue()
  {
    return toValue();
  }

  /**
   * Converts to a reference variable.
   */
  @Override
  public Value toLocalRef()
  {
    return _obj.getField(_env, _name);
  }

  @Override
  public Value toAutoArray()
  {
    Value parent = _obj.toAutoObject(_env);
    Value value = parent.getField(_env, _name);

    Value array = value.toAutoArray();

    if (array != value) {
      parent.putField(_env, _name, array);

      value = array;
    }

    return value;
  }

  @Override
  public Value toAutoObject(Env env)
  {
    Value parent = _obj.toAutoObject(env);
    Value value = parent.getField(env, _name);

    if (value.isNull()) {
      value = env.createObject();

      parent.putField(env, _name, value);
    }
    else {
      Value obj = value.toAutoObject(env);

      if (obj != value) {
        parent.putField(env, _name, obj);
      }

      value = obj;
    }

    return value;
  }

  /**
   * Converts to a reference variable.
   */
  @Override
  public Value toRefValue()
  {
    return _obj.getFieldVar(_env, _name);
  }

  /**
   * Converts to a variable.
   */
  public Var toVar()
  {
    return new Var(toValue());
  }

  /**
   * Converts to a reference variable.
   */
  @Override
  public Var getVar(Value index)
  {
    return _obj.getFieldArray(_env, _name).getVar(index);
  }

  /**
   * Converts to a reference variable.
   */
  @Override
  public Var getFieldVar(Env env, StringValue name)
  {
    // php/3d2q
    return _obj.getFieldObject(_env, _name).getFieldVar(_env, name);
  }

  @Override
  public StringValue toStringValue()
  {
    return toValue().toStringValue();
  }

  @Override
  public String toJavaString()
  {
    return toValue().toJavaString();
  }
}

