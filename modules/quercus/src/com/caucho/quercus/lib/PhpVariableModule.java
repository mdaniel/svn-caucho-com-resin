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

package com.caucho.quercus.lib;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;

import com.caucho.util.L10N;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.ReadStream;

import com.caucho.quercus.module.AbstractPhpModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.UsesSymbolTable;

import com.caucho.quercus.env.*;

import com.caucho.vfs.WriteStream;

/**
 * Information about PHP variables.
 */
public class PhpVariableModule extends AbstractPhpModule {
  private static final L10N L = new L10N(PhpVariableModule.class);

  private static final HashMap<String,Value> _constMap
    = new HashMap<String,Value>();

  /**
   * Adds the constant to the PHP engine's constant map.
   *
   * @return the new constant chain
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }

  /**
   * Returns a constant
   *
   * @param env the quercus calling environment
   * @param name the constant name
   */
  public static Value constant(Env env, String name)
    throws Throwable
  {
    return env.getConstant(name);
  }

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value debug_zval_dump(Env env, Value v)
    throws Throwable
  {
    debug_impl(env, v, 0);

    return NullValue.NULL;
  }

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value var_dump(Env env, Value v)
    throws Throwable
  {
    if (v instanceof RefVar)
      env.getOut().print("&");
    
    var_dump_impl(env, env.getOut(), v.toValue(), 0,
		  new IdentityHashMap<Value,String>());

    env.getOut().println();

    return NullValue.NULL;
  }

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value debug_var_dump(Env env, Value v)
    throws Throwable
  {
    WriteStream out = Vfs.openWrite("stdout:");
    
    if (v instanceof RefVar)
      out.print("&");
    
    var_dump_impl(env, out, v.toValue(), 0,
		  new IdentityHashMap<Value,String>());

    out.println();

    out.close();

    return NullValue.NULL;
  }

  /**
   * Returns the type string for the variable
   */
  public static String gettype(Value v)
  {
    return v.getType();
  }

  /**
   * Returns the type string for the variable
   */
  public static boolean is_callable(Env env,
				    Value v,
				    @Optional boolean isSyntaxOnly,
				    @Optional @Reference Value nameRef)
  {
    if (v instanceof StringValue) {
      if (nameRef != null)
	nameRef.set(v);
      
      if (isSyntaxOnly)
	return true;
      else
	return env.findFunction(v.toString()) != null;
    }
    else if (v instanceof ArrayValue) {
      Value obj = v.get(LongValue.ZERO);
      Value name = v.get(LongValue.ONE);
      
      if (! (name instanceof StringValue))
	return false;

      if (nameRef != null)
	nameRef.set(name);
      
      if (obj instanceof StringValue) {
	if (isSyntaxOnly)
	  return true;
	
	PhpClass cl = env.findClass(obj.toString());
	if (cl == null)
	  return false;

	return (cl.findFunction(name.toString()) != null);
      }
      else if (obj.isObject()) {
	if (isSyntaxOnly)
	  return true;

	return obj.findFunction(name.toString()) != null;
      }
      else
	return false;
    }
    else
      return false;
  }

  /**
   * Returns the type string for the variable
   */
  public static boolean isset(Value v)
  {
    return v.isset();
  }

  /**
   * Returns true for an empty variable.
   *
   * @param env the calling environment
   * @param v the value to test
   * @return the escaped stringPhp
   */
  public static Value empty(Env env, Value v)
    throws Throwable
  {
    v = v.toValue();

    if (v instanceof NullValue)
      return BooleanValue.TRUE;
    else if (v instanceof StringValue) {
      String s = v.toString(env);
      
      return (s.equals("") || s.equals("0")
	      ? BooleanValue.TRUE
	      : BooleanValue.FALSE);
    }
    else if (v instanceof LongValue) {
      return v.toLong() == 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else if (v instanceof BooleanValue) {
      return v.toBoolean() ? BooleanValue.FALSE : BooleanValue.TRUE;
    }
    else if (v instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) v;

      return array.getSize() == 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Defines a constant
   *
   * @param env the quercus calling environment
   * @param nameV the constant name
   * @param value the constant value
   */
  public static Value define(Env env, String name, Value value)
    throws Throwable
  {
    return env.addConstant(name, value);
  }

  /**
   * Returns true if the constant is defined.
   *
   * @param env the quercus calling environment
   * @param name the constant name
   */
  public static boolean defined(Env env, String name)
    throws Throwable
  {
    return env.isDefined(name);
  }

  /**
   * Converts to a double
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value doubleval(Value v)
         throws IOException
  {
    return floatval(v);
  }

  /**
   * Converts to a double
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value floatval(Value v)
         throws IOException
  {
    return new DoubleValue(v.toDouble());
  }

  /**
   * Returns the defined variables in the current scope.
   */
  @UsesSymbolTable
  public static Value get_defined_vars(Env env)
  {
    ArrayValue result = new ArrayValueImpl();

    HashMap<String,Var> map = env.getEnv();

    for (Map.Entry<String,Var> entry : map.entrySet()) {
      result.append(new StringValue(entry.getKey()),
		    entry.getValue().toValue());
    }

    HashMap<String,Var> globalMap = env.getGlobalEnv();
    if (map != globalMap) {
      for (Map.Entry<String,Var> entry : globalMap.entrySet()) {
	result.append(new StringValue(entry.getKey()),
		      entry.getValue().toValue());
      }
    }

    return result;
  }

  /**
   * Converts to a long
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value intval(Value v)
         throws IOException
  {
    return new LongValue(v.toLong());
  }

  /**
   * Returns true for an array.
   *
   * @param v the value to test
   *
   * @return true for an array
   */
  public static boolean is_array(Value v)
         throws IOException
  {
    return v.isArray();
  }

  /**
   * Returns true for a boolean
   *
   * @param v the value to test
   *
   * @return true for a boolean
   */
  public static Value is_bool(Value v)
         throws IOException
  {
    return (v.toValue() instanceof BooleanValue
	    ? BooleanValue.TRUE
	    : BooleanValue.FALSE);
  }

  /**
   * Returns true for a double
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static boolean is_double(Value v)
         throws IOException
  {
    return is_float(v);
  }

  /**
   * Returns true for a double
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static boolean is_float(Value v)
         throws IOException
  {
    return (v.toValue() instanceof DoubleValue);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_int(Value v)
         throws IOException
  {
    return (v.toValue() instanceof LongValue
	    ? BooleanValue.TRUE
	    : BooleanValue.FALSE);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_integer(Value v)
         throws IOException
  {
    return is_int(v);
  }

  /**
   * Returns true for an integer
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static Value is_long(Value v)
         throws IOException
  {
    return is_int(v);
  }

  /**
   * Returns true for null
   *
   * @param v the value to test
   *
   * @return true for null
   */
  public static boolean is_null(Value v)
         throws IOException
  {
    return v.isNull();
  }

  /**
   * Returns true for numeric
   *
   * @param env the calling environment
   * @param v the value to test
   *
   * @return true for numeric
   */
  public static boolean is_numeric(Env env, Value v)
         throws IOException
  {
    v = v.toValue();

    if (v instanceof LongValue)
      return true;
    else if (v instanceof DoubleValue)
      return true;
    else if (v instanceof StringValue) {
      try {
	Double.parseDouble(v.toString(env));
	return true;
      } catch (Throwable e) {
	return false;
      }
    }
    else
      return false;
  }

  /**
   * Returns true for a real
   *
   * @param v the value to test
   *
   * @return true for a real
   */
  public static boolean is_real(Value v)
         throws IOException
  {
    return is_float(v);
  }

  /**
   * Converts to a string
   *
   * @param env the quercus calling environment
   * @param v the variable to convert
   * @return the double value
   */
  public static Value strval(Env env, Value v)
    throws Throwable
  {
    return new StringValue(v.toString(env));
  }

  /**
   * Returns true if the value is a resource
   */
  public boolean is_resource(Value value)
  {
    return (value.toValue() instanceof ResourceValue);
  }

  /**
   * Returns true if the value is a string
   */
  public boolean is_string(Value value)
  {
    return (value.toValue() instanceof StringValue);
  }

  /**
   * Escapes a string using C syntax.
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value print_r(Env env, Value v, @Optional Value isRet)
    throws Throwable
  {
    print_r_impl(env, v, 0);

    env.getOut().println();

    return BooleanValue.FALSE;
  }

  private static void print_r_impl(Env env, Value v, int depth)
    throws Throwable
  {
    WriteStream out = env.getOut();

    v = v.toValue();

    if (v instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) v;

      out.println("Array");
      printDepth(out, 8 * depth);
      out.println("(");

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
        printDepth(out, 8 * depth);
        out.print("    [");
        out.print(entry.getKey());
        out.print("] => ");
        print_r_impl(env, entry.getValue().toValue(), depth + 1); // XXX: recursion
	out.println();
      }
      printDepth(out, 8 * depth);
      out.print(")");
    }
    else {
      v.print(env);
    }
  }

  /**
   * Serializes the value to a string.
   */
  public static String serialize(Value v)
  {
    StringBuilder sb = new StringBuilder();

    v.serialize(sb);

    return sb.toString();
  }

  /**
   * Unserializes the value from a string.
   */
  public static Value unserialize(String v)
    throws IOException
  {
    UnserializeReader is = new UnserializeReader(v);

    return unserialize(is);
  }

  private static Value unserialize(UnserializeReader is)
    throws IOException
  {
    int ch = is.read();

    switch (ch) {
    case 'b':
      {
	unserializeExpect(is, ':');
	long v = unserializeInt(is);
	unserializeExpect(is, ';');

	return v == 0 ? BooleanValue.FALSE : BooleanValue.TRUE;
      }
      
    case 's':
      {
	unserializeExpect(is, ':');
	int len = (int) unserializeInt(is);
	unserializeExpect(is, ':');
	unserializeExpect(is, '"');

	char []buf = new char[len];

	is.read(buf, 0, len);

	unserializeExpect(is, '"');
	unserializeExpect(is, ';');

	return new StringValue(new String(buf));
      }
      
    case 'i':
      {
	unserializeExpect(is, ':');
	
	long value = unserializeInt(is);
	
	unserializeExpect(is, ';');

	return new LongValue(value);
      }
      
    case 'd':
      {
	unserializeExpect(is, ':');

	StringBuilder sb = new StringBuilder();
	for (ch = is.read(); ch >= 0 && ch != ';'; ch = is.read()) {
	  sb.append((char) ch);
	}

	if (ch != ';')
	  throw new IOException(L.l("expected ';'"));

	return new DoubleValue(Double.parseDouble(sb.toString()));
      }
      
    case 'a':
      {
	unserializeExpect(is, ':');
	long len = unserializeInt(is);
	unserializeExpect(is, ':');
	unserializeExpect(is, '{');

	ArrayValue array = new ArrayValueImpl();
	for (int i = 0; i < len; i++) {
	  Value key = unserialize(is);
	  Value value = unserialize(is);

	  array.append(key, value);
	}

	unserializeExpect(is, '}');
	unserializeExpect(is, ';');

	return array;
      }
      
    case 'N':
      {
	unserializeExpect(is, ';');

	return NullValue.NULL;
      }
      
    default:
      return NullValue.NULL;
    }
  }

  private static void unserializeExpect(UnserializeReader is, int expectCh)
    throws IOException
  {
    int ch = is.read();

    if (ch < 0)
      throw new IOException(L.l("expected '{0}' at end of string",
				String.valueOf((char) expectCh)));
    else if (ch != expectCh)
      throw new IOException(L.l("expected '{0}' at '{1}'",
				String.valueOf((char) expectCh),
				String.valueOf((char) ch)));
  }

  private static long unserializeInt(UnserializeReader is)
    throws IOException
  {
    int ch = is.read();
    long sign = 1;
    long value = 0;

    if (ch == '-') {
      sign = -1;
      ch = is.read();
    }
    else if (ch == '+') {
      ch = is.read();
    }

    for (; '0' <= ch && ch <= '9'; ch = is.read()) {
      value = 10 * value + ch - '0';
    }

    is.unread();
    
    return sign * value;
  }

  private static void debug_impl(Env env, Value v, int depth)
    throws Throwable
  {
    WriteStream out = env.getOut();

    if (v instanceof Var)
      out.print("&");
    
    v = v.toValue();

    if (v instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) v;

      out.println("Array");
      printDepth(out, 2 * depth);
      out.println("(");

      for (Map.Entry<Value,Value> mapEntry : array.entrySet()) {
	ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;
	
        printDepth(out, 2 * depth);
        out.print("    [");
        out.print(entry.getKey());
        out.print("] => ");
        debug_impl(env, entry.getRawValue(), depth + 1); // XXX: recursion
      }
      printDepth(out, 2 * depth);
      out.println(")");
    }
    else if (v instanceof BooleanValue) {
      if (v.toBoolean())
	out.print("bool(true)");
      else
	out.print("bool(false)");
    }
    else if (v instanceof LongValue) {
      out.print("int(" + v.toLong() + ")");
    }
    else if (v instanceof DoubleValue) {
      out.print("float(" + v.toDouble() + ")");
    }
    else if (v instanceof StringValue) {
      out.print("string(" + v.toString(env) + ")");
    }
    else if (v instanceof NullValue) {
      out.print("NULL");
    }
    else {
      v.print(env);
    }
  }

  private static void var_dump_impl(Env env, WriteStream out,
				    Value v, int depth,
				    IdentityHashMap<Value,String> valueSet)
    throws Throwable
  {
    if (v instanceof Var)
      out.print("&");
    
    v = v.toValue();
    
    if (valueSet.get(v) != null) {
      out.print("#recursion#");
      return;
    }

    try {
      valueSet.put(v, "printing");

      if (v instanceof ArrayValue) {
	ArrayValue array = (ArrayValue) v;

	out.println("array(" + array.getSize() + ") {");

	for (Map.Entry<Value,Value> mapEntry : array.entrySet()) {
	  ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;
	  
	  printDepth(out, 2 * depth + 2);
	  out.print("[");

	  Value key = entry.getKey();
	  if (key instanceof StringValue)
	    out.print("\"" + key + "\"");
	  else
	    out.print(key);
	  out.println("]=>");
	  printDepth(out, 2 * depth + 2);
	  var_dump_impl(env, out, entry.getRawValue(), depth + 1, valueSet);
	  out.println();
	}
	printDepth(out, 2 * depth);
	out.print("}");
      }
      else if (v instanceof ObjectValue) {
	ObjectValue object = (ObjectValue) v;

	out.println("object(" + object.getName() + ") (" + object.getSize() + ") {");

	for (Map.Entry<Value,Value> mapEntry : object.entrySet()) {
	  ArrayValue.Entry entry = (ArrayValue.Entry) mapEntry;
	  
	  printDepth(out, 2 * depth + 2);
	  out.print("[");
	  out.print("\"" + entry.getKey().toString(env) + "\"");
	  out.println("]=>");
	  printDepth(out, 2 * depth + 2);
	  var_dump_impl(env, out, entry.getRawValue(), depth + 1, valueSet);
	  out.println();
	}
	printDepth(out, 2 * depth);
	out.print("}");
      }
      else if (v instanceof BooleanValue) {
	if (v.toBoolean())
	  out.print("bool(true)");
	else
	  out.print("bool(false)");
      }
      else if (v instanceof LongValue) {
	out.print("int(" + v.toLong() + ")");
      }
      else if (v instanceof DoubleValue) {
	out.print("float(" + v.toDouble() + ")");
      }
      else if (v instanceof StringValue) {
	String s = v.toString();
      
	out.print("string(" + s.length() + ") \"" + s + "\"");
      }
      else if (v instanceof NullValue) {
	out.print("NULL");
      }
      else {
	v.print(env);
      }
    } finally {
      valueSet.remove(v);
    }
  }

  private static void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }

  static class UnserializeReader {
    private final char []_buffer;
    private final int _length;
    
    private int _index;

    UnserializeReader(String s)
    {
      _buffer = s.toCharArray();
      _length = _buffer.length;
    }
    
    public int read()
    {
      if (_index < _length)
	return _buffer[_index++];
      else
	return -1;
    }
    
    public int read(char []buffer, int offset, int length)
    {
      System.arraycopy(_buffer, _index, buffer, offset, length);

      _index += length;

      return length;
    }

    public void unread()
    {
      _index--;
    }
  }
}

