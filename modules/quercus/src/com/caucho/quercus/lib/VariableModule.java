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

package com.caucho.quercus.lib;

import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.UsesSymbolTable;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Information about PHP variables.
 */
public class VariableModule extends AbstractQuercusModule {
  private static final Logger log
    = Logger.getLogger(VariableModule.class.getName());
  private static final L10N L = new L10N(VariableModule.class);

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
  public static Value debug_zval_dump(Env env, @ReadOnly Value v)
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
  public static Value var_dump(Env env, @ReadOnly Value v)
    throws Throwable
  {
    if (v == null)
      env.getOut().print("NULL#java");
    else
      v.varDump(env, env.getOut(), 0,  new IdentityHashMap<Value,String>());

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
  public static Value resin_var_dump(Env env, @ReadOnly Value v)
    throws Throwable
  {
    WriteStream out = Vfs.openWrite("stdout:");

    v.varDump(env, out, 0, new IdentityHashMap<Value,String>());

    out.println();

    out.close();

    return NullValue.NULL;
  }

  /**
   * Defines a constant
   *
   * @param env the quercus calling environment
   * @param name the constant name
   * @param value the constant value
   */
  public static Value define(Env env,
			     String name,
			     Value value,
			     @Optional boolean isCaseInsensitive)
    throws Throwable
  {
    return env.addConstant(name, value, isCaseInsensitive);
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
  public static Value doubleval(@ReadOnly Value v)
         throws IOException
  {
    return floatval(v);
  }

  /**
   * Returns true for an empty variable.
   *
   * @param v the value to test
   *
   * @return true if the value is empty
   */
  public static boolean empty(@ReadOnly Value v)
  {
    v = v.toValue();

    if (v instanceof NullValue)
      return true;
    else if (v instanceof StringValue) {
      String s = v.toString();

      return (s.equals("") || s.equals("0"));
    }
    else if (v instanceof LongValue) {
      return v.toLong() == 0;
    }
    else if (v instanceof BooleanValue) {
      return ! v.toBoolean();
    }
    else if (v instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) v;

      return array.getSize() == 0;
    }
    else
      return false;
  }

  /**
   * Converts to a double
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static Value floatval(@ReadOnly Value v)
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

    IdentityHashMap<String,Var> map = env.getEnv();

    for (Map.Entry<String,Var> entry : map.entrySet()) {
      result.append(new StringValueImpl(entry.getKey()),
		    entry.getValue().toValue());
    }

    IdentityHashMap<String,Var> globalMap = env.getGlobalEnv();
    if (map != globalMap) {
      for (Map.Entry<String,Var> entry : globalMap.entrySet()) {
	result.append(new StringValueImpl(entry.getKey()),
		      entry.getValue().toValue());
      }
    }

    return result;
  }

  /**
   * Returns the type string for the variable
   */
  public static String gettype(@ReadOnly Value v)
  {
    return v.getType();
  }

  /**
   * Imports request variables
   *
   * @param types the variables to import
   * @param prefix the prefix
   */
  public static boolean import_request_variables(Env env,
						 String types,
						 @Optional String prefix)
  {
    if ("".equals(prefix))
      env.notice(L.l("import_request_variables should use a prefix argument"));

    for (int i = 0; i < types.length(); i++) {
      char ch = types.charAt(i);

      Value value = null;

      if (ch == 'c' || ch == 'C')
	value = env.getGlobalValue("_COOKIE");
      else if (ch == 'g' || ch == 'G')
	value = env.getGlobalValue("_GET");
      else if (ch == 'p' || ch == 'P')
	value = env.getGlobalValue("_POST");

      if (! (value instanceof ArrayValue))
	continue;

      ArrayValue array = (ArrayValue) value;

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
	String key = entry.getKey().toString();

	env.setGlobalValue(prefix + key,
			 array.getRef(entry.getKey()));
      }
    }

    return true;
  }

  /**
   * Converts to a long
   *
   * @param v the variable to convert
   * @return the double value
   */
  public static long intval(@ReadOnly Value v)
  {
    return v.toLong();
  }

  /**
   * Returns true for an array.
   *
   * @param v the value to test
   *
   * @return true for an array
   */
  public static boolean is_array(@ReadOnly Value v)
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
  public static Value is_bool(@ReadOnly Value v)
         throws IOException
  {
    return (v.toValue() instanceof BooleanValue
	    ? BooleanValue.TRUE
	    : BooleanValue.FALSE);
  }

  /**
   * Returns true for a scalar
   *
   * @param v the value to test
   *
   * @return true for a scalar
   */
  public static boolean is_scalar(@ReadOnly Value v)
         throws IOException
  {
    return v.isScalar();
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

	QuercusClass cl = env.findClass(obj.toString());
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
   * Returns true for a double
   *
   * @param v the value to test
   *
   * @return true for a double
   */
  public static boolean is_double(@ReadOnly Value v)
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
  public static boolean is_float(@ReadOnly Value v)
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
  public static Value is_int(@ReadOnly Value v)
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
  public static Value is_integer(@ReadOnly Value v)
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
  public static Value is_long(@ReadOnly Value v)
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
  public static boolean is_null(@ReadOnly Value v)
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
  public static boolean is_numeric(Env env, @ReadOnly Value v)
    throws IOException
  {
    v = v.toValue();

    if (v instanceof LongValue)
      return true;
    else if (v instanceof DoubleValue)
      return true;
    else if (v instanceof StringValue) {
      try {
	Double.parseDouble(v.toString());
	return true;
      } catch (Throwable e) {
	return false;
      }
    }
    else
      return false;
  }

  // XXX: is_object

  /**
   * Returns true for a real
   *
   * @param v the value to test
   *
   * @return true for a real
   */
  public static boolean is_real(@ReadOnly Value v)
         throws IOException
  {
    return is_float(v);
  }

  /**
   * Returns true if the value is a resource
   */
  public boolean is_resource(@ReadOnly Value value)
  {
    return (value.toValue() instanceof ResourceValue);
  }

  // XXX: is_scalar

  /**
   * Returns true if the value is a string
   */
  public boolean is_string(@ReadOnly Value value)
  {
    return (value.toValue() instanceof StringValue);
  }

  /**
   * Returns the type string for the variable
   */
  public static boolean isset(@ReadOnly Value v)
  {
    return v.isset();
  }

  /**
   * Converts to a string
   *
   * @param env the quercus calling environment
   * @param v the variable to convert
   * @return the double value
   */
  public static Value strval(Env env, @ReadOnly Value v)
    throws Throwable
  {
    if (v instanceof StringValue)
      return (StringValue) v;
    else
      return new StringValueImpl(v.toString());
  }

  /**
   * Escapes a string using C syntax.
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value print_r(Env env,
			      @ReadOnly Value v,
			      @Optional Value isRet)
    throws Throwable
  {
    // XXX: isRet is ignored

    WriteStream out = env.getOut();

    v.printR(env, out, 0, new IdentityHashMap<Value, String>());

    return BooleanValue.FALSE;
  }

  private static void printDepth(WriteStream out, int depth)
    throws IOException
  {
    for (int i = 0; i < depth; i++)
      out.print(' ');
  }

  /**
   * Serializes the value to a string.
   */
  public static String serialize(@ReadOnly Value v)
  {
    StringBuilder sb = new StringBuilder();

    v.serialize(sb);

    return sb.toString();
  }

  /**
   * Converts the variable to a specified tyep.
   */
  public static boolean settype(Env env,
				@Reference Value var,
				String type)
  {
    Value value = var.toValue();

    if ("null".equals(type)) {
      var.set(NullValue.NULL);
      return true;
    }
    else if ("boolean".equals(type) || "bool".equals(type)) {
      var.set(value.toBoolean() ? BooleanValue.TRUE : BooleanValue.TRUE);
      return true;
    }
    else if ("string".equals(type)) {
      var.set(new StringValueImpl(value.toString()));
      return true;
    }
    else if ("int".equals(type) || "integer".equals(type)) {
      var.set(new LongValue(value.toLong()));
      return true;
    }
    else if ("float".equals(type) || "double".equals(type)) {
      var.set(new DoubleValue(value.toDouble()));
      return true;
    }
    else if ("object".equals(type)) {
      var.set(value.toObject(env));
      return true;
    }
    else if ("array".equals(type)) {
      if (value.isArray())
	var.set(value);
      else
	var.set(new ArrayValueImpl().append(value));
      return true;
    }
    else
      return false;
  }

  /**
   * Unserializes the value from a string.
   */
  public static Value unserialize(Env env, String v)
  {
    try {
      UnserializeReader is = new UnserializeReader(v);

      return is.unserialize(env);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      env.notice(e.toString());

      return BooleanValue.FALSE;
    }
  }

  /**
   * Serializes the value to a string.
   */
  public static Value var_export(Env env,
				 @ReadOnly Value v,
				 @Optional boolean isReturn)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    v.varExport(sb);

    if (isReturn)
      return new StringValueImpl(sb.toString());
    else {
      env.getOut().print(sb);

      return NullValue.NULL;
    }
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
        debug_impl(env, entry.getValue(), depth + 1); // XXX: recursion
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
      out.print("string(" + v.toString() + ")");
    }
    else if (v instanceof NullValue) {
      out.print("NULL");
    }
    else {
      v.print(env);
    }
  }
}

