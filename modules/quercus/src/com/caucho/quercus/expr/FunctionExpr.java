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

package com.caucho.quercus.expr;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.parser.*;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.AnalyzeInfo;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class FunctionExpr extends Expr {
  private static final L10N L = new L10N(FunctionExpr.class);
  
  protected final String _name;
  protected final Expr []_args;

  public FunctionExpr(Location location, String name, ArrayList<Expr> args)
  {
    // quercus/120o
    super(location);
    _name = name.intern();

    _args = new Expr[args.size()];
    args.toArray(_args);
  }

  public FunctionExpr(Location location, String name, Expr []args)
  {
    // quercus/120o
    super(location);
    _name = name.intern();

    _args = args;
  }

  public FunctionExpr(String name, ArrayList<Expr> args)
  {
    this(Location.UNKNOWN, name, args);
  }

  public FunctionExpr(String name, Expr []args)
  {
    this(Location.UNKNOWN, name, args);
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Returns the location if known.
   */
  public String getFunctionLocation()
  {
    return " [" + _name + "]";
  }

  /**
   * Returns the reference of the value.
   * @param location
   */
  @Override
  public Expr createRef(QuercusParser parser)
  {
    return parser.getFactory().createRef(this);
  }

  /**
   * Returns the copy of the value.
   * @param location
   */
  @Override
  public Expr createCopy(ExprFactory factory)
  {
    return this;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    return evalImpl(env, false, false);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalRef(Env env)
  {
    return evalImpl(env, true, false);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value evalCopy(Env env)
  {
    return evalImpl(env, false, true);
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  private Value evalImpl(Env env, boolean isRef, boolean isCopy)
  {
    AbstractFunction fun = env.findFunction(_name);

    if (fun == null) {
      env.error(getLocationLine(), L.l("'{0}' is an unknown function.", _name));

      return NullValue.NULL;
    }

    Value []args = fun.evalArguments(env, this, _args);

    env.pushCall(this, NullValue.NULL);
    try {
      env.checkTimeout();
	
      if (isRef)
	return fun.callRef(env, args);
      else if (isCopy)
	return fun.callCopy(env, args);
      else
	return fun.call(env, args);
    } finally {
      env.popCall();
    }
  }

  //
  // Java code generation
  //
  
  /**
   * Analyzes the function.
   */
  public void analyze(AnalyzeInfo info)
  {
    Quercus quercus = info.getFunction().getPhp();
    AbstractFunction fun = quercus.findFunction(_name);

    if (fun != null && fun.isCallUsesVariableArgs())
      info.getFunction().setVariableArgs(true);
    
    if (fun != null && fun.isCallUsesSymbolTable()) {
      // php/1729
      info.getFunction().setUsesSymbolTable(true);
      info.clear();
    }
    
    for (int i = 0; i < _args.length; i++) {
      _args[i].analyze(info);
    }

    // check for read-only and refs

    if (fun != null) {
      fun.analyzeArguments(_args, info);
    }
    else {
      for (int i = 0; i < _args.length; i++) {
	_args[i].analyzeSetModified(info);
	_args[i].analyzeSetReference(info);
      }
    }
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    generateImpl(out, Value.class, false, false);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generateImpl(out, Value.class, false, true);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out)
    throws IOException
  {
    generateImpl(out, Value.class, true, false);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out)
    throws IOException
  {
    generateImpl(out, void.class, true, false);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out)
    throws IOException
  {
    generateImpl(out, boolean.class, false, false);
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generate(out);  // php/3c4k
    out.print(".toValue()");
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateImpl(PhpWriter out, Class retType,
			   boolean isRef, boolean isCopy)
    throws IOException
  {
    // Quercus quercus = out.getPhp();

    // StaticFunction fun = quercus.findFunction(_name);

    QuercusProgram program = out.getProgram();

    AbstractFunction fun = program.findFunction(_name);
    
    if (fun == null || ! fun.isGlobal())
      fun = program.getPhp().findFunction(_name);

    if (fun != null && fun.canGenerateCall(_args)) {
      if (void.class.equals(retType))
	fun.generateTop(out, this, _args);
      else if (boolean.class.equals(retType))
	fun.generateBoolean(out, this, _args);
      else if (isRef && fun.isReturnsReference()) // php/3442
	fun.generateRef(out, this, _args);
      else if (isCopy || isRef) // php/3442
	fun.generateCopy(out, this, _args);
      else
	fun.generate(out, this, _args);
    }
    else {
      if (void.class.equals(retType))
	isRef = true;
      else if (boolean.class.equals(retType))
	isRef = true;
      else if (! isCopy)
	isRef = true;
      
      // super.generate(out);

      // XXX: need to check where it's from

      out.print("env.getFunction(\"");
      out.printJavaString(_name);

      if (isRef)
	out.print("\").callRef(env");
      else
	out.print("\").call(env");

      if (_args.length <= COMPILE_ARG_MAX) {
	for (int i = 0; i < _args.length; i++) {
	  out.print(", ");

	  _args[i].generateArg(out);
	}
      }
      else {
	out.print(", new Value[] {");

	for (int i = 0; i < _args.length; i++) {
	  if (i != 0)
	    out.print(", ");

	  _args[i].generateArg(out);
	}

	out.print("}");
      }
      
      out.print(")");
    
      if (boolean.class.equals(retType))
	out.print(".toBoolean()");
      /*
      else if (isCopy)
	out.print(".copyReturn()");
      */
    }
  }

  /**
   * Generates code to recreate the expression.  Used for default values.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new FunctionExpr(\"");
    out.printJavaString(_name);
    out.print("\", new Expr[] {");
    
    for (int i = 0; i < _args.length; i++) {
      _args[i].generateExpr(out);
      out.print(", ");
    }

    out.print("})");
  }
  
  public String toString()
  {
    return _name + "()";
  }
}

