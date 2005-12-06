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

package com.caucho.php.program;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

import java.io.IOException;

import com.caucho.php.program.AbstractFunction;
import com.caucho.php.program.Function;

import com.caucho.php.env.Value;
import com.caucho.php.env.Env;

import com.caucho.php.expr.Expr;

import com.caucho.php.gen.PhpWriter;

/**
 * Represents an interpreted PHP class definition.
 */
public class InterpretedClassDef extends AbstractClassDef {
  private final HashMap<String,AbstractFunction> _functionMap
    = new HashMap<String,AbstractFunction>();

  private final HashMap<String,AbstractFunction> _functionMapLowerCase
    = new HashMap<String,AbstractFunction>();

  private final HashMap<Value,Expr> _fieldMap
    = new LinkedHashMap<Value,Expr>();

  private final HashMap<String,Expr> _staticFieldMap
    = new LinkedHashMap<String,Expr>();

  private Function _constructor;
  private AbstractFunction _destructor;

  public InterpretedClassDef(String name, String parentName)
  {
    super(name, parentName);
  }

  /**
   * Returns the map.
   */
  public HashMap<Value,Expr> getFieldMap()
  {
    return _fieldMap;
  }

  /**
   * Adds a function.
   */
  public void addFunction(String name, Function fun)
  {
    _functionMap.put(name, fun);
    _functionMapLowerCase.put(name.toLowerCase(), fun);

    if (name.equals("__construct"))
      _constructor = fun;
    else if (name.equals("__destruct"))
      _destructor = fun;
    else if (name.equals(getName()) && _constructor == null)
      _constructor = fun;
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunction(String name)
  {
    return _functionMap.get(name);
  }

  /**
   * Finds the matching function.
   */
  public AbstractFunction findFunctionLowerCase(String name)
  {
    return _functionMapLowerCase.get(name);
  }

  /**
   * Adds a static value.
   */
  public void addStaticValue(Value name, Expr value)
  {
    _staticFieldMap.put(name.toString(), value);
  }

  /**
   * Adds a value.
   */
  public void addValue(Value name, Expr value)
  {
    _fieldMap.put(name, value);
  }

  /**
   * Adds a value.
   */
  public Expr get(Value name)
  {
    return _fieldMap.get(name);
  }

  /**
   * Initialize the class.
   */
  public void init(Env env)
    throws Throwable
  {
    for (Map.Entry<String,Expr> var : _staticFieldMap.entrySet()) {
      String name = getName() + "::" + var.getKey();

      env.setGlobalValue(name, var.getValue().eval(env).copy());
    }
  }

  /**
   * Initialize the fields
   */
  public void initInstance(Env env, Value object)
    throws Throwable
  {
    for (Map.Entry<Value,Expr> entry : _fieldMap.entrySet())
      object.put(entry.getKey(), entry.getValue().eval(env).copy());
  }

  /**
   * Returns the constructor
   */
  public AbstractFunction findConstructor()
  {
    return _constructor;
  }

  /**
   * Analyzes the class
   */
  public void analyze()
  {
    for (AbstractFunction fun : _functionMap.values()) {
      fun.analyze();
    }
  }

  /**
   * Generates the class.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.addClass(this);
    
    out.println();
    out.println("public static class php_" + getName() + " extends CompiledClass {");
    out.pushDepth();

    out.println();
    out.println("public php_" + getName() + "()");
    out.println("{");
    out.pushDepth();
    out.print("super(\"");
    out.printJavaString(getName());
    out.print("\", ");
    if (getParentName() != null) {
      out.print("\"");
      out.printJavaString(getParentName());
      out.print("\"");
    }
    else {
      out.print("null");
    }
    out.println(");");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void initInstance(Env env, Value value)");
    out.println("   throws Throwable");
    out.println("{");
    out.pushDepth();
    
    for (Map.Entry<Value,Expr> entry : _fieldMap.entrySet()) {
      Value key = entry.getKey();
      Expr value = entry.getValue();

      out.print("value.put(");
      out.print(out.addValue(key));
      out.print(",");
      value.generateExpr(out);
      out.print(".eval(env)");
      out.println(");");
    }

    /*
    for (AbstractFunction fun : _functionMap.values()) {
      fun.generateInit(out);
    }
    */
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public AbstractFunction findConstructor()");
    out.println("{");

    if (_constructor != null)
      out.println("  return __php_fun_" + _constructor.getName() + ";");
    else
      out.println("  return null;");
      
    out.println("}");

    for (AbstractFunction fun : _functionMap.values()) {
      fun.generate(out);
    }

    out.println("private static final java.util.HashMap<String,AbstractFunction> _methodMap;");
    out.println("private static final java.util.HashMap<String,AbstractFunction> _methodMapLowerCase;");
    out.println();
    out.println("static {");
    out.pushDepth();

    out.println();
    out.println("_methodMap = new java.util.HashMap<String,AbstractFunction>();");
    out.println("_methodMapLowerCase = new java.util.HashMap<String,AbstractFunction>();");
    
    int index = 0;
    
    for (String key : _functionMap.keySet()) {
      out.print("_methodMap.put(\"");
      out.printJavaString(key);
      out.println("\", __php_fun_" + key + ");");
      
      out.print("_methodMapLowerCase.put(\"");
      out.printJavaString(key.toLowerCase());
      out.println("\", __php_fun_" + key + ");");
    }

    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("public AbstractFunction findFunction(String name)");
    out.println("{");
    out.pushDepth();

    out.println("return _methodMap.get(name);");
    
    out.popDepth();
    out.println("}");
    
    out.println();
    out.println("public AbstractFunction findFunctionLowerCase(String name)");
    out.println("{");
    out.pushDepth();

    out.println("return _methodMapLowerCase.get(name);");
    
    out.popDepth();
    out.println("}");

    // XXX:
    out.println();
    out.println("public void init(Env env)");
    out.println("{");
    out.pushDepth();

    for (Map.Entry<String,Expr> var : _staticFieldMap.entrySet()) {
      out.print("env.setGlobalValue(\"" + getName() + "::" + var.getKey() + "\", ");
      var.getValue().generate(out);
      out.println(");");
    }
    
    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the class initialization.
   */
  public void generateInit(PhpWriter out)
    throws IOException
  {
    out.print("addClass(\"");
    out.printJavaString(getName());
    out.println("\", __php_class_" + getName() + ");");
  }

  public String toString()
  {
    return "Class[" + getName() + "]";
  }
}

