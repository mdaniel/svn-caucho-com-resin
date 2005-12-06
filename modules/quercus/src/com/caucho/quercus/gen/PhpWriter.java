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

package com.caucho.quercus.gen;

import java.io.IOException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.JavaWriterWrapper;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.module.PhpModule;

import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.PhpProgram;
  
/**
 * Writer which gathers additional info.
 */
public class PhpWriter extends JavaWriterWrapper {
  private PhpProgram _program;
  
  private HashMap<Value,String> _valueMap
    = new HashMap<Value,String>();
  
  private HashMap<Expr,String> _exprMap
    = new HashMap<Expr,String>();
  
  private HashMap<Expr[],String> _exprArrayMap
    = new HashMap<Expr[],String>();
  
  private HashMap<PhpModule,String> _moduleMap
    = new HashMap<PhpModule,String>();

  private ArrayList<InterpretedClassDef> _classList
    = new ArrayList<InterpretedClassDef>();

  private ArrayList<String> _staticVarList = new ArrayList<String>();

  private boolean _isSwitch;
  private String _breakVar;
  
  public PhpWriter(JavaWriter writer, PhpProgram program)
  {
    super(writer);

    _program = program;
  }
  
  /**
   * Returns the engine.
   */
  public Quercus getPhp()
  {
    return _program.getPhp();
  }
  
  /**
   * Returns the program
   */
  public PhpProgram getProgram()
  {
    return _program;
  }

  /**
   * Returns true if in a switch.
   */
  public boolean isSwitch()
  {
    return _isSwitch;
  }

  /**
   * Sets the switch value.
   */
  public boolean setSwitch(boolean isSwitch)
  {
    boolean oldSwitch = _isSwitch;

    _isSwitch = isSwitch;

    return oldSwitch;
  }

  /**
   * Sets the switch value.
   */
  public String setBreakVar(String breakVar)
  {
    String oldBreakVar = _breakVar;

    _breakVar = breakVar;

    return oldBreakVar;
  }

  /**
   * Gets the switch value.
   */
  public String getBreakVar()
  {
    return _breakVar;
  }

  /**
   * Prints a contstant.
   */
  public void print(Value value)
    throws IOException
  {
    print(addValue(value));
  }

  /**
   * Adds a constant value.
   *
   * @return the generated id for the value
   */
  public String addValue(Value value)
  {
    String var = _valueMap.get(value);

    if (var == null) {
      var = "_quercus_value_" + generateId();

      _valueMap.put(value, var);
    }

    return var;
  }

  /**
   * Adds a constant value.
   *
   * @return the generated id for the value
   */
  public void addClass(InterpretedClassDef cl)
  {
    _classList.add(cl);
  }

  /**
   * Adds an expression
   *
   * @return the generated id for the expression
   */
  public String addExpr(Expr expr)
  {
    String var = _exprMap.get(expr);

    if (var == null) {
      var = "_quercus_expr_" + generateId();

      _exprMap.put(expr, var);
    }

    return var;
  }

  /**
   * Adds an expression
   *
   * @return the generated id for the expression
   */
  public String addExprArray(Expr []exprArray)
  {
    String var = _exprArrayMap.get(exprArray);

    if (var == null) {
      var = "_quercus_expr_" + generateId();

      _exprArrayMap.put(exprArray, var);
    }

    return var;
  }

  /**
   * Adds a module
   *
   * @return the generated id for the expression
   */
  public String addModule(PhpModule module)
  {
    String var = _moduleMap.get(module);

    if (var == null) {
      var = "_quercus_module_" + generateId();

      _moduleMap.put(module, var);
    }

    return var;
  }

  /**
   * Returns a static variable name.
   */
  public String createStaticVar()
  {
    String varName = "__quercus_static_" + _staticVarList.size();
    
    _staticVarList.add(varName);

    return varName;
  }

  /**
   * Generates the tail.
   */
  public void generateCoda()
    throws IOException
  {
    if (! _exprMap.isEmpty())
      println();
    
    for (Map.Entry<Expr,String> entry : _exprMap.entrySet()) {
      Expr expr = entry.getKey();
      String var = entry.getValue();

      println("private static final com.caucho.quercus.expr.Expr " + var);
      print("  = ");
      expr.generateExpr(this);
      println(";");
    }
    
    if (! _exprArrayMap.isEmpty())
      println();
    
    for (Map.Entry<Expr[],String> entry : _exprArrayMap.entrySet()) {
      Expr []exprArray = entry.getKey();
      String var = entry.getValue();

      println("private static final com.caucho.quercus.expr.Expr []" + var);
      print("  = new Expr[] {");

      for (int i = 0; i < exprArray.length; i++) {
	if (i != 0)
	  print(", ");

	exprArray[i].generateExpr(this);
      }
      println("};");
    }
    
    if (! _valueMap.isEmpty())
      println();
    
    for (Map.Entry<Value,String> entry : _valueMap.entrySet()) {
      Value value = entry.getKey();
      String var = entry.getValue();

      println("private static final com.caucho.quercus.env.Value " + var);
      print("  = ");
      value.generate(this);
      println(";");
    }
    
    if (! _moduleMap.isEmpty())
      println();
    
    for (Map.Entry<PhpModule,String> entry : _moduleMap.entrySet()) {
      PhpModule module = entry.getKey();
      String var = entry.getValue();

      String moduleClass = module.getClass().getName();

      println("private static " + moduleClass + " " + var + ";");
    }
    
    for (InterpretedClassDef cl : _classList) {
      String name = cl.getName();

      println();
      println("static final quercus_" + name + " __php_class_" + name + " = new php_" + name + "();");
    }

    for (int i = 0; i < _staticVarList.size(); i++) {
      println("static String " + _staticVarList.get(i) + ";");
    }

    println();
    println("public void init(com.caucho.quercus.Quercus php)");
    println("{");
    pushDepth();
    
    for (Map.Entry<PhpModule,String> entry : _moduleMap.entrySet()) {
      PhpModule module = entry.getKey();
      String var = entry.getValue();

      String moduleClass = module.getClass().getName();

      println(var + " = (" + moduleClass + ") quercus.findModule(\"" + moduleClass + "\");");
    }

    for (int i = 0; i < _staticVarList.size(); i++) {
      println(_staticVarList.get(i) + " = quercus.createStaticName();");
    }

    println("initFunctions();");

    popDepth();
    println("}");
  }
}

