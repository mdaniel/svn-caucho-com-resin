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

package com.caucho.quercus.program;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Set;

import java.io.IOException;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.InstanceInitializer;
import com.caucho.quercus.program.Function;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents an interpreted PHP class definition.
 */
public class InterpretedClassDef extends ClassDef
  implements InstanceInitializer {
  private boolean _isAbstract;
  private boolean _isInterface;
  
  private final HashMap<String,AbstractFunction> _functionMap
    = new HashMap<String,AbstractFunction>();

  private final HashMap<String,Expr> _fieldMap
    = new LinkedHashMap<String,Expr>();

  private final HashMap<String,Expr> _staticFieldMap
    = new LinkedHashMap<String,Expr>();

  private final HashMap<String,Expr> _constMap
    = new HashMap<String,Expr>();

  private AbstractFunction _constructor;
  private AbstractFunction _destructor;
  private AbstractFunction _getField;
  private AbstractFunction _setField;
  private AbstractFunction _call;

  public InterpretedClassDef(String name,
			     String parentName,
			     String []ifaceList)
  {
    super(name, parentName, ifaceList);
  }

  /**
   * true for an abstract class.
   */
  public void setAbstract(boolean isAbstract)
  {
    _isAbstract = isAbstract;
  }

  /**
   * True for an abstract class.
   */
  public boolean isAbstract()
  {
    return _isAbstract;
  }

  /**
   * true for an interface class.
   */
  public void setInterface(boolean isInterface)
  {
    _isInterface = isInterface;
  }

  /**
   * True for an interface class.
   */
  public boolean isInterface()
  {
    return _isInterface;
  }

  /**
   * Initialize the quercus class.
   */
  public void initClass(QuercusClass cl)
  {
    if (_constructor != null)
      cl.setConstructor(_constructor);
    
    if (_getField != null)
      cl.setGet(_getField);
    
    if (_setField != null)
      cl.setSet(_setField);
    
    if (_call != null)
      cl.setCall(_call);
    
    cl.addInitializer(this);
    
    for (Map.Entry<String,AbstractFunction> entry : _functionMap.entrySet()) {
      cl.addMethod(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Sets the constructor.
   */
  public void setConstructor(AbstractFunction fun)
  {
    _constructor = fun;
  }
  
  /**
   * Adds a function.
   */
  public void addFunction(String name, Function fun)
  {
    _functionMap.put(name.intern(), fun);

    if (name.equals("__construct"))
      _constructor = fun;
    else if (name.equals("__destruct"))
      _destructor = fun;
    else if (name.equals("__get"))
      _getField = fun;
    else if (name.equals("__set"))
      _setField = fun;
    else if (name.equals("__call"))
      _call = fun;
    else if (name.equals(getName()) && _constructor == null)
      _constructor = fun;
  }

  /**
   * Adds a static value.
   */
  public void addStaticValue(Value name, Expr value)
  {
    _staticFieldMap.put(name.toString().intern(), value);
  }

  /**
   * Adds a const value.
   */
  public void addConstant(String name, Expr value)
  {
    _constMap.put(name.intern(), value);
  }

  /**
   * Return a const value.
   */
  public Expr findConstant(String name)
  {
    return _constMap.get(name);
  }

  /**
   * Adds a value.
   */
  public void addValue(Value name, Expr value)
  {
    _fieldMap.put(name.toString().intern(), value);
  }

  /**
   * Adds a value.
   */
  public Expr get(Value name)
  {
    return _fieldMap.get(name.toString().intern());
  }

  /**
   * Return true for a declared field.
   */
  public boolean isDeclaredField(String name)
  {
    return _fieldMap.get(name) != null;
  }

  /**
   * Initialize the class.
   */
  public void init(Env env)
  {
    for (Map.Entry<String,Expr> var : _staticFieldMap.entrySet()) {
      String name = getName() + "::" + var.getKey();

      env.setGlobalValue(name.intern(), var.getValue().eval(env).copy());
    }
  }

  /**
   * Initialize the fields
   */
  public void initInstance(Env env, Value value)
  {
    ObjectValue object = (ObjectValue) value;
    
    for (Map.Entry<String,Expr> entry : _fieldMap.entrySet())
      object.putFieldInit(env, entry.getKey(), entry.getValue().eval(env).copy());
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
    out.print("private ClassDef q_cl_" + getName() + " = ");
    out.print("new CompiledClassDef(\"" + getName() + "\", ");
    if (getParentName() != null)
      out.print("\"" + getParentName() + "\"");
    else
      out.print("null");

    out.print(", new String[] {");
    String []ifaceList = getInterfaces();
    for (int i = 0; i < ifaceList.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print("\"");
      out.print(ifaceList[i]);
      out.print("\"");
    }
    
    out.print("}, quercus_" + getName() + ".class);");
    
    out.println();
    out.println("public static class quercus_" + getName() + " implements InstanceInitializer {");
    out.pushDepth();

    if (false) {
      out.println();
      for (String key : _fieldMap.keySet()) {
	out.println("private final int _f_" + key + ";");
      }
    }

    out.println();
    out.println("public quercus_" + getName() + "(QuercusClass cl)");
    out.println("{");
    out.pushDepth();

    out.println("cl.addInitializer(this);");
    out.println();
    
    if (_constructor != null) {
      out.println("cl.setConstructor(__quercus_fun_" + _constructor.getName() + ");");
      out.println();
    }
    
    if (_getField != null) {
      out.println("cl.setGet(__quercus_fun_" + _getField.getName() + ");");
      out.println();
    }
    
    if (_setField != null) {
      out.println("cl.setSet(__quercus_fun_" + _setField.getName() + ");");
      out.println();
    }
    
    if (_call != null) {
      out.println("cl.setCall(__quercus_fun_" + _call.getName() + ");");
      out.println();
    }

    if (false) {
      for (String key : _fieldMap.keySet()) {
	out.print("_f_" + key + " = ");
	out.println("cl.addFieldIndex(\"" + key + "\");");
      }
    }

    out.println();

    for (String key : _functionMap.keySet()) {
      out.print("cl.addMethod(\"");
      out.printJavaString(key);
      out.println("\", __quercus_fun_" + key + ");");
    }
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public static void init(QuercusClass cl)");
    out.println("{");
    out.pushDepth();
    out.println("quercus_" + getName() + " def = new quercus_" + getName() +
		"(cl);");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void initInstance(Env env, Value valueArg)");
    //out.println("   throws Throwable");
    out.println("{");
    out.pushDepth();

    //out.println("CompiledObjectValue value = (CompiledObjectValue) valueArg;");
    out.println("ObjectValue value = (ObjectValue) valueArg;");

    for (Map.Entry<String,Expr> entry : _fieldMap.entrySet()) {
      String key = entry.getKey();
      Expr value = entry.getValue();

      if (false) {
	out.print("value._fields[_f_" + key + "] = ");
	value.generate(out);
	out.println(";");
      }
      else {
	out.print("value.putFieldInit(env, \"" + key + "\", ");
	value.generate(out);
	out.println(");");
      }
	
    }

    /*
    for (AbstractFunction fun : _functionMap.values()) {
      fun.generateInit(out);
    }
    */
    
    out.popDepth();
    out.println("}");

    for (AbstractFunction fun : _functionMap.values()) {
      fun.generate(out);
    }

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
    out.print("\", new CompiledClassDef(\"");
    out.printJavaString(getName());
    out.print("\", ");
    if (getParentName() != null) {
      out.print("\"");
      out.printJavaString(getParentName());
      out.print("\"");
    }
    else
      out.print("null");

    out.print(", new String[] {");
    String []ifaceList = getInterfaces();
    for (int i = 0; i < ifaceList.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print("\"");
      out.print(ifaceList[i]);
      out.print("\"");
    }
    
    out.println("}, quercus_" + getName() + ".class));");
  }

  public String toString()
  {
    return "Class[" + getName() + "]";
  }
  
  public Set<Map.Entry<String, Expr>> fieldSet()
  {
    return _fieldMap.entrySet();
  }
  
  public Set<Map.Entry<String, AbstractFunction>> functionSet()
  {
    return _functionMap.entrySet();
  }
}

