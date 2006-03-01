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

package com.caucho.quercus.module;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.env.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.io.IOException;

/**
 * Code for marshalling arguments.
 */
abstract public class Marshall {
  private static final L10N L = new L10N(Marshall.class);

  public static Marshall create(Quercus quercus,
                                Class argType)
  {
    return create(quercus, argType, false);
  }

  public static Marshall create(Quercus quercus,
                                Class argType,
                                boolean isNotNull)
  {
    if (argType.isAssignableFrom(Value.class)) {
      return MARSHALL_VALUE;
    }
    else if (String.class.equals(argType)) {
      return MARSHALL_STRING;
    }
    else if (boolean.class.equals(argType)) {
      return MARSHALL_BOOLEAN;
    }
    else if (Boolean.class.equals(argType)) {
      return MARSHALL_BOOLEAN_OBJECT;
    }
    else if (byte.class.equals(argType)) {
      return MARSHALL_BYTE;
    }
    else if (Byte.class.equals(argType)) {
      return MARSHALL_BYTE_OBJECT;
    }
    else if (short.class.equals(argType)) {
      return MARSHALL_SHORT;
    }
    else if (Short.class.equals(argType)) {
      return MARSHALL_SHORT_OBJECT;
    }
    else if (int.class.equals(argType)) {
      return MARSHALL_INTEGER;
    }
    else if (Integer.class.equals(argType)) {
      return MARSHALL_INTEGER_OBJECT;
    }
    else if (long.class.equals(argType)) {
      return MARSHALL_LONG;
    }
    else if (Long.class.equals(argType)) {
      return MARSHALL_LONG_OBJECT;
    }
    else if (float.class.equals(argType)) {
      return MARSHALL_FLOAT;
    }
    else if (Float.class.equals(argType)) {
      return MARSHALL_FLOAT_OBJECT;
    }
    else if (double.class.equals(argType)) {
      return MARSHALL_DOUBLE;
    }
    else if (Double.class.equals(argType)) {
      return MARSHALL_DOUBLE_OBJECT;
    }
    else if (char.class.equals(argType)) {
      return MARSHALL_CHARACTER;
    }
    else if (Character.class.equals(argType)) {
      return MARSHALL_CHARACTER_OBJECT;
    }
    else if (Path.class.equals(argType)) {
      return MARSHALL_PATH;
    }
    else if (Callback.class.equals(argType)) {
      return MARSHALL_CALLBACK;
    }
    else if (Value.class.equals(argType)) {
      return MARSHALL_VALUE;
    }
    else if (Value.class.isAssignableFrom(argType)) {
      return MARSHALL_EXT_VALUE;
    }
    else if (void.class.equals(argType)) {
      return MARSHALL_VOID;
    }
    else {
      return new JavaMarshall(quercus.getJavaClassDefinition(argType.getName()),
                              isNotNull);
    }
  }

  /**
   * Returns true if the result is a primitive boolean.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true if the result is a string.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true if the result is a long.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true if the result is a double.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Return true for read-only.
   */
  public boolean isReadOnly()
  {
    return true;
  }

  abstract public Object marshall(Env env, Expr expr, Class argClass)
    throws Throwable;

  public Object marshall(Env env, Value value, Class argClass)
    throws Throwable
  {
    return value;
  }

  abstract public void generate(PhpWriter out, Expr expr, Class argClass)
    throws IOException;

  public Value unmarshall(Env env, Object value)
    throws Throwable
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void generateResultStart(PhpWriter out)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  static final Marshall MARSHALL_VALUE = new Marshall() {
    public boolean isReadOnly()
    {
      return false;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return expr.eval(env);
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return value;
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      return (Value) value;
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      // php/3a70 vs php/3783 - see generated code
      // generate vs generateValue
      expr.generateValue(out);
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("((Value) ");
    }
  };

  static final Marshall MARSHALL_EXT_VALUE = new Marshall() {
    public boolean isReadOnly()
    {
      return false;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return marshall(env, expr.eval(env), expectedClass);
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      if (value == null || ! value.isset())
        return null;

      // XXX: need QA, added for mantis view bug page
      value = value.toValue();

      if (expectedClass.isAssignableFrom(value.getClass()))
        return value;
      else {
        String className = expectedClass.getName();
        int p = className.lastIndexOf('.');
        className = className.substring(p + 1);

        String valueClassName = value.getClass().getName();
        p = valueClassName.lastIndexOf('.');
        valueClassName = valueClassName.substring(p + 1);

        env.warning(L.l("'{0}' is an unexpected argument, expected {1}",
                        value, className));

        return null;
      }
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      return (Value) value;
    }

    public void generate(PhpWriter out, Expr expr, Class expectedClass)
      throws IOException
    {
      String className = expectedClass.getName();

      out.print("((" + className + ") env.cast(" +
                className + ".class, ");

      expr.generateValue(out);
      out.print("))");
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("((Value) ");
    }
  };

  static final Marshall MARSHALL_STRING = new Marshall() {
    public boolean isString()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return expr.evalString(env);
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return value.toString();
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return NullValue.NULL;
      else
        return new StringValueImpl((String) value);
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateString(out);
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("StringValue.create(");
    }
  };

  static final Marshall MARSHALL_BOOLEAN = new Marshall() {
    public boolean isBoolean()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return expr.evalBoolean(env) ? Boolean.TRUE : Boolean.FALSE;
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return value.toBoolean() ? Boolean.TRUE : Boolean.FALSE;
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateBoolean(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return NullValue.NULL;
      else
        return Boolean.TRUE.equals(value)
               ? BooleanValue.TRUE
               : BooleanValue.FALSE;
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("BooleanValue.create(");
    }
  };

  static final Marshall MARSHALL_BOOLEAN_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return expr.evalBoolean(env) ? Boolean.TRUE : Boolean.FALSE;
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return value.toBoolean() ? Boolean.TRUE : Boolean.FALSE;
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("(");
      expr.generateBoolean(out);
      out.print("? Boolean.TRUE : Boolean.FALSE)");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return NullValue.NULL;
      else
        return Boolean.TRUE.equals(value)
               ? BooleanValue.TRUE
               : BooleanValue.FALSE;
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("BooleanValue.create(");
    }
  };

  static final Marshall MARSHALL_BYTE = new Marshall() {
    public boolean isLong()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Byte((byte) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Byte((byte) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("(byte) ");
      expr.generateLong(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_BYTE_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Byte((byte) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Byte((byte) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Byte((byte) ");
      expr.generateLong(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_SHORT = new Marshall() {
    public boolean isLong()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Short((short) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Short((short) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("(short) ");
      expr.generateLong(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_SHORT_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Short((short) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Short((short) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Short((short) ");
      expr.generateLong(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_INTEGER = new Marshall() {
    public boolean isLong()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Integer((int) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Integer((int) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("(int) ");
      expr.generateLong(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_INTEGER_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Integer((int) expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Integer((int) value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Integer((int) ");
      expr.generateLong(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_LONG = new Marshall() {
    public boolean isLong()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Long(expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Long(value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateLong(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_LONG_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Long(expr.evalLong(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Long(value.toLong());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Long(");
      expr.generateLong(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return LongValue.ZERO;
      else
        return new LongValue(((Number) value).longValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("LongValue.create(");
    }
  };

  static final Marshall MARSHALL_FLOAT = new Marshall() {
    public boolean isDouble()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Float((float) expr.evalDouble(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Float((float) value.toDouble());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("(float) ");
      expr.generateDouble(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return DoubleValue.ZERO;
      else
        return new DoubleValue(((Number) value).doubleValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("DoubleValue.create(");
    }
  };

  static final Marshall MARSHALL_FLOAT_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Float((float) expr.evalDouble(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Float((float) value.toDouble());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Float((float) ");
      expr.generateDouble(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return DoubleValue.ZERO;
      else
        return new DoubleValue(((Number) value).doubleValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("DoubleValue.create(");
    }
  };

  static final Marshall MARSHALL_DOUBLE = new Marshall() {
    public boolean isDouble()
    {
      return true;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Double(expr.evalDouble(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Double(value.toDouble());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateDouble(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return DoubleValue.ZERO;
      else
        return new DoubleValue(((Number) value).doubleValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("DoubleValue.create(");
    }
  };

  static final Marshall MARSHALL_DOUBLE_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Double(expr.evalDouble(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Double(value.toDouble());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Double(");
      expr.generateDouble(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return DoubleValue.ZERO;
      else
        return new DoubleValue(((Number) value).doubleValue());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("DoubleValue.create(");
    }
  };

  static final Marshall MARSHALL_CHARACTER = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Character(expr.evalChar(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Character(value.toChar());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateChar(out);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return NullValue.NULL;
      else
        return new StringValueImpl(value.toString());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("StringValue.create(");
    }
  };

  static final Marshall MARSHALL_CHARACTER_OBJECT = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return new Character(expr.evalChar(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return new Character(value.toChar());
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("new Character(");
      expr.generateChar(out);
      out.print(")");
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      if (value == null)
        return NullValue.NULL;
      else
        return new StringValueImpl(value.toString());
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      out.print("StringValue.create(");
    }
  };

  public static final Marshall MARSHALL_REFERENCE = new Marshall() {
    public boolean isReadOnly()
    {
      return false;
    }

    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      // quercus/0d1k
      return expr.evalRef(env);
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      throw new UnsupportedOperationException();
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      expr.generateRef(out);
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }
  };

  static final Marshall MARSHALL_PATH = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return env.getPwd().lookup(expr.evalString(env));
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      return env.getPwd().lookup(value.toString());
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      throw new UnsupportedOperationException();
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("env.getPwd().lookup(");
      expr.generateString(out);
      out.print(")");
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }
  };

  static final Marshall MARSHALL_CALLBACK = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      return marshall(env, expr.eval(env), expectedClass);
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      Callback cb = env.createCallback(value);

     // return env.createCallback(value);
      
     if (cb != null)
        return cb;
     else if (value instanceof DefaultValue)
        return null;
     else
        return new CallbackFunction(env, value.toString()); //null;
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      throw new UnsupportedOperationException();
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      out.print("env.createCallback(");
      expr.generate(out);
      out.print(")");
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }
  };

  static final Marshall MARSHALL_VOID = new Marshall() {
    public Object marshall(Env env, Expr expr, Class expectedClass)
      throws Throwable
    {
      throw new UnsupportedOperationException();
    }

    public Object marshall(Env env, Value value, Class expectedClass)
      throws Throwable
    {
      throw new UnsupportedOperationException();
    }

    public Value unmarshall(Env env, Object value)
      throws Throwable
    {
      return NullValue.NULL;
    }

    public void generate(PhpWriter out, Expr expr, Class argClass)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }

    public void generateResultStart(PhpWriter out)
      throws IOException
    {
      throw new UnsupportedOperationException();
    }
  };
}

