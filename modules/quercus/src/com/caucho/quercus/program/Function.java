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

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import java.util.logging.Logger;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.VarInfo;
import com.caucho.quercus.expr.VarState;
import com.caucho.quercus.expr.NullLiteralExpr;

import com.caucho.util.L10N;

import com.caucho.quercus.env.Var;
import com.caucho.quercus.env.NullValue;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

/**
 * Represents sequence of statements.
 */
public class Function extends AbstractFunction {
  private static final Logger log = Logger.getLogger(Function.class.getName());
  private static final L10N L = new L10N(Function.class);

  private final FunctionInfo _info;
  private final boolean _isReturnsReference;

  private final String _name;
  private final Arg []_args;
  private final Statement _statement;

  private boolean _isStatic = true;

  private boolean _hasReturn;

  Function(Location location,
           String name,
           FunctionInfo info,
           Arg []args,
           Statement []statements)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();
    _args = args;
    _statement = new BlockStatement(location, statements);

    setGlobal(info.isPageStatic());
  }

  public Function(Location location,
                  String name,
                  FunctionInfo info,
                  ArrayList<Arg> argList,
                  ArrayList<Statement> statementList)
  {
    super(location);
    
    _name = name.intern();
    _info = info;
    _info.setFunction(this);
    _isReturnsReference = info.isReturnsReference();

    _args = new Arg[argList.size()];
    argList.toArray(_args);

    Statement []statements = new Statement[statementList.size()];
    statementList.toArray(statements);

    _statement = new BlockStatement(location, statements);

    setGlobal(info.isPageStatic());
  }

  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the args.
   */
  public Arg []getArgs()
  {
    return _args;
  }

  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  public boolean isStatic()
  {
    return _isStatic;
  }

  public boolean isObjectMethod()
  {
    return false;
  }

  /**
   * True for a returns reference.
   */
  public boolean isReturnsReference()
  {
    return _isReturnsReference;
  }

  public String getClassName()
  {
    throw new UnsupportedOperationException();
  }

  public Value execute(Env env)
  {
    return null;
  }

  /**
   * Evaluates a function's argument, handling ref vs non-ref
   */
  @Override
  public Value []evalArguments(Env env, Expr fun, Expr []args)
  {
    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length)
        arg = _args[i];

      if (arg == null)
        values[i] = args[i].eval(env).copy();
      else if (arg.isReference())
        values[i] = args[i].evalRef(env);
      else {
        // php/0d04
        values[i] = args[i].eval(env);
      }
    }

    return values;
  }

  public Value call(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callCopy(Env env, Expr []args)
  {
    return callImpl(env, args, false);
  }

  public Value callRef(Env env, Expr []args)
  {
    return callImpl(env, args, true);
  }

  private Value callImpl(Env env, Expr []args, boolean isRef)
  {
    HashMap<String,Var> map = new HashMap<String,Var>();

    Value []values = new Value[args.length];

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
        values[i] = args[i].eval(env).copy();
      }
      else if (arg.isReference()) {
        values[i] = args[i].evalRef(env);

        map.put(arg.getName(), values[i].toRefVar());
      }
      else {
        // php/0d04
        values[i] = args[i].eval(env);

        Var var = values[i].toVar();

        map.put(arg.getName(), var);

        values[i] = var.toValue();
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(arg.getName(), defaultExpr.evalRef(env).toVar());
      else {
        map.put(arg.getName(), defaultExpr.eval(env).copy().toVar());
      }
    }

    HashMap<String,Var> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(values); // php/0476

    try {
      Value value = _statement.execute(env);

      if (value == null)
        return NullValue.NULL;
      else if (_isReturnsReference && isRef)
        return value;
      else
        return value.copyReturn();
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
    }
  }

  public Value call(Env env, Value []args)
  {
    return callImpl(env, args, false);
  }

  public Value callCopy(Env env, Value []args)
  {
    return callImpl(env, args, false);
  }

  public Value callRef(Env env, Value []args)
  {
    return callImpl(env, args, true);
  }

  private Value callImpl(Env env, Value []args, boolean isRef)
  {
    HashMap<String,Var> map = new HashMap<String,Var>();

    for (int i = 0; i < args.length; i++) {
      Arg arg = null;

      if (i < _args.length) {
        arg = _args[i];
      }

      if (arg == null) {
      }
      else if (arg.isReference())
        map.put(arg.getName(), args[i].toRefVar());
      else {
        // quercus/0d04
        map.put(arg.getName(), args[i].copy().toVar());
      }
    }

    for (int i = args.length; i < _args.length; i++) {
      Arg arg = _args[i];

      Expr defaultExpr = arg.getDefault();

      if (defaultExpr == null)
        return env.error("expected default expression");
      else if (arg.isReference())
        map.put(arg.getName(), defaultExpr.evalRef(env).toVar());
      else {
        map.put(arg.getName(), defaultExpr.eval(env).copy().toVar());
      }
    }

    HashMap<String,Var> oldMap = env.pushEnv(map);
    Value []oldArgs = env.setFunctionArgs(args);

    try {
      Value value = _statement.execute(env);

      if (value == null)
        return NullValue.NULL;
      else if (_isReturnsReference && isRef)
        return value;
      else
        return value.copyReturn();
    } finally {
      env.restoreFunctionArgs(oldArgs);
      env.popEnv(oldMap);
    }
  }

  //
  // Java generation code.
  //

  /**
   * Analyzes the function.
   */
  public void analyze()
  {
    AnalyzeInfo info = new AnalyzeInfo(_info);

    for (int i = 0; i < _args.length; i++) {
      Arg arg = _args[i];

      VarExpr var = new VarExpr(getLocation(), _info.createVar(arg.getName()));
      var.setVarState(VarState.VALID);

      info.addVar(var);
    }

    _hasReturn = ! _statement.analyze(info);
  }

  /**
   * Returns true if the function can generate the call directly.
   */
  public boolean canGenerateCall(Expr []args)
  {
    // can only handle case where the arg length matches
    return args.length <= _args.length;
  }

  /**
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generateImpl(out, funExpr, args, false);
  }

  /**
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generateImpl(out, funExpr, args, true);
  }

  /**
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  private void generateImpl(PhpWriter out, Expr funExpr,
                            Expr []args, boolean isRef)
    throws IOException
  {
    String ref;

    if (! _isReturnsReference)
      ref = "";
    else
      ref = "Ref";
    
    out.print("fun_" + _name + ".call" + ref + "(env");

    if (! isVariableArgs()) {
    }
    else if (args.length == 0 && _args.length == 0)
      out.print(", Value.NULL_ARGS");
    else
      out.print(", new Value[] {");

    for (int i = 0; i < _args.length; i++) {
      if (i != 0 || ! isVariableArgs())
        out.print(", ");

      if (i < args.length) {
        Expr arg = args[i];

        if (_args[i].isReference()) {
          arg.generateRef(out);
        }
        else {
          arg.generateArg(out);
        }
      }
      else if (_args[i].getDefault() != null)
        _args[i].getDefault().generateArg(out);
      else
        out.print("NullValue.NULL");
    }

    for (int i = _args.length; i < args.length; i++) {
      if (i != 0)
        out.print(", ");
      args[i].generateArg(out);
    }

    if (isVariableArgs() && (_args.length > 0 || args.length > 0))
      out.print("}");

    out.print(")");

    //if (_isReturnsReference && ! isRef)
    //  out.print(".toValue()");
    
    /* php/3a70 - should only be generated from generateCopy
    if (! (isRef && _isReturnsReference))
      out.print(".copyReturn()");
    */
  }

  private boolean isVariableArgs()
  {
    return _info.isVariableArgs() || _args.length > 5;
  }

  private boolean isVariableMap()
  {
    // return _info.isVariableVar();
    // php/3254
    return _info.isUsesSymbolTable() || _info.isVariableVar();
  }

  /**
   * Generates the code for the class component.
   *
   * @param out the writer to the output stream.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    if (isVariableArgs())
      generateVariableArgs(out);
    else
      generateFixedArgs(out);
  }

  /**
   * Generates the code for the class component.
   *
   * @param out the writer to the output stream.
   */
  public void generateFixedArgs(PhpWriter out)
    throws IOException
  {
    out.println();
    out.print("public static final fun_" + _name + " fun_" + _name);
    out.println(" = new fun_" + _name + "();");

    String ref = _isReturnsReference ? "Ref" : "";

    out.println();
    out.print("final static class fun_" + _name + " extends ");
    
    if (isStatic())
      out.print("CompiledFunction" + ref + "_" + _args.length);
    else
      out.print("CompiledMethod" + ref + "_" + _args.length);

    out.println(" {");
    out.pushDepth();
    
    out.println("fun_" + _name + "()");
    out.println("{");
    out.print("  super(\"");
    out.printJavaString(_name);
    out.print("\"");

    for (int i = 0; i < _args.length; i++) {
      out.print(", ");

      Expr defaultExpr = _args[i].getDefault();

      defaultExpr.generateExpr(out);
    }
    out.println(");");
    out.println("}");
        
    out.println();
    if (isStatic())
      out.print("public final Value call" + ref + "(Env env");
    else
      out.print("public final Value callMethod" + ref + "(Env env, Value q_this");

    for (int i = 0; i < _args.length; i++) {
      out.print(", ");

      out.print("Value v_");
      out.print(_args[i].getName());
    }
    out.println(")");

    out.println("{");

    out.pushDepth();

    generateBody(out);

    out.popDepth();

    out.println("}");
    out.popDepth();
    out.println("}");

    if (! isGlobal()) {
      out.println();
      out.println("static { fun_" + _name + ".setGlobal(false); }");
    }
  }

  /**
   * Generates the code for the class component.
   *
   * @param out the writer to the output stream.
   */
  public void generateVariableArgs(PhpWriter out)
    throws IOException
  {
    out.println();
    out.print("public static final fun_" + _name + " fun_" + _name);
    out.println(" = new fun_" + _name + "();");
    
    out.println();
    out.print("public static final class fun_" + _name + " extends ");

    String ref = _isReturnsReference ? "Ref" : "";

    if (isStatic())
      out.println(" CompiledFunction" + ref + "_N {");
    else
      out.println(" CompiledMethod" + ref + "_N {");

    out.pushDepth();
    out.println("fun_" + _name + "()");
    out.println("{");
    out.print("  super(\"");
    out.printJavaString(_name);
    out.print("\", new Expr[] {");

    for (int i = 0; i < _args.length; i++) {
      if (i != 0)
        out.print(", ");

      Expr defaultExpr = _args[i].getDefault();

      if (defaultExpr != null)
        defaultExpr.generateExpr(out);
      else
        out.print("null");
    }
    out.println("});");
    out.println("}");
    

    out.println();
    if (isStatic())
      out.println("public Value call" + ref + "Impl(Env env, Value []args)");
    else
      out.println("public Value callMethod" + ref + "Impl(Env env, Value q_this, Value []args)");

    out.println("{");
    out.pushDepth();

    if (_info.isVariableArgs()) {
      out.println("Value []quercus_oldArgs = env.setFunctionArgs(args);");
      out.println("try {");
      out.pushDepth();
    }

    generateBody(out);

    if (_info.isVariableArgs()) {
      out.popDepth();
      out.println("} finally {");
      out.pushDepth();
      out.println("env.restoreFunctionArgs(quercus_oldArgs);");
      out.popDepth();
      out.println("}");
    }

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  private void generateBody(PhpWriter out)
    throws IOException
  {
    out.println("env.checkTimeout();");
    out.println();

    for (VarInfo var : _info.getVariables()) {
      String varName = "v_" + var.getName();
      String argName = varName;

      if (! var.isArgument())
        out.print("Value ");
      else if (isVariableArgs()) {
        out.print("Value ");

        argName = "args[" + var.getArgumentIndex() + "]";
      }

      if (var.isArgument()) {
        if (isVariableMap()) {
	  if (var.isRefArgument()) {
	    // php/3214
	    out.println(varName + " = " + argName + ".toRefVar();");
	  }
	  else
	    out.println(varName + " = " + argName + ".toVar();");
        }
        else if (var.isReadOnly() && var.isValue()) {
          // php/3a70, php/343k
          out.println(varName + " = " + argName + ".toArgValue();");
        }
        else if (var.isReadOnly()) {
          // php/3783
          out.println(varName + " = " + argName + ".toRefValue();");
        }
        else if (var.isRefArgument()) {
          // php/344r, 3a57
          out.println(varName + " = " + argName + ".toRefVar();");
        }
        else if (var.isAssigned() && var.isReference()) {
          out.println(varName + " = " + argName + ".toVar();");
        }
        else {
          // php/399j
          out.println(varName + " = " + argName + ".toArgValue();");
        }
      }
      else {
        // local variable, i.e. not argument

        if (var.isValue())
          out.println(varName + " = NullValue.NULL;");
        else
          out.println(varName + " = null;");
      }
    }

    for (String var : _info.getTempVariables()) {
      out.println("Value " + var + ";");
    }

    if (isVariableMap()) {
      out.println("java.util.HashMap<String,Var> _quercus_map = new java.util.HashMap<String,Var>();");
      out.println("java.util.HashMap<String,Var> _quercus_oldMap = env.pushEnv(_quercus_map);");

      if (! isStatic())
	out.println("Value q_oldThis = env.setThis(q_this);");
      
      out.println("try {");
      out.pushDepth();

      for (int i = 0; i < _args.length; i++) {
        out.print("_quercus_map.put(\"");
        out.printJavaString(_args[i].getName());
        out.println("\", (Var) v_" + _args[i].getName() + ");");
      }
    }

    _statement.generate(out);

    if (isVariableMap()) {
      out.popDepth();
      out.println("} finally {");
      out.pushDepth();
      // out.println("env.setFunctionArgs(_quercus_oldArgs);");
      out.println("env.popEnv(_quercus_oldMap);");

      if (! isStatic())
	out.println("env.setThis(q_oldThis);");
      
      out.popDepth();
      out.println("}");
    }

    if (_statement.fallThrough() != Statement.RETURN)
      out.println("return com.caucho.quercus.env.NullValue.NULL;");
  }

  /**
   * Generates the code for the initialization component.
   *
   * @param out the writer to the output stream.
   */
  public void generateInit(PhpWriter out)
    throws IOException
  {
    out.println();
    out.print("addFunction(\"");
    out.printJavaString(_name.toLowerCase());
    out.println("\", fun_" + _name + ");");
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    out.println("function " + _name + "()");
    _statement.debug(out);
  }

  public String toString()
  {
    return "Function[" + _name + "]";
  }
}

