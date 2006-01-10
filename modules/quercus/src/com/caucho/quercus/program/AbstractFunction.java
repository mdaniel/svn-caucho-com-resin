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

package com.caucho.quercus.program;

import java.io.IOException;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.FunctionExpr;
import com.caucho.quercus.expr.NullLiteralExpr;
import com.caucho.quercus.expr.DefaultExpr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.util.L10N;

/**
 * Represents a function
 */
abstract public class AbstractFunction {
  private static final L10N L = new L10N(AbstractFunction.class);

  private static final Arg []NULL_ARGS = new Arg[0];
  private static final Value []NULL_ARG_VALUES = new Value[0];

  private boolean _isGlobal = true;

  /**
   * Returns true for a global function.
   */
  public final boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * Returns true for a global function.
   */
  public final void setGlobal(boolean isGlobal)
  {
    _isGlobal = isGlobal;
  }

  /**
   * Returns true for a boolean function.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true for a string function.
   */
  public boolean isString()
  {
    return false;
  }

  /**
   * Returns true for a long function.
   */
  public boolean isLong()
  {
    return false;
  }

  /**
   * Returns true for a double function.
   */
  public boolean isDouble()
  {
    return false;
  }

  /**
   * Returns true if the function uses variable args.
   */
  public boolean isCallUsesVariableArgs()
  {
    return false;
  }

  /**
   * Returns true if the function uses/modifies the local symbol table
   */
  public boolean isCallUsesSymbolTable()
  {
    return false;
  }

  /**
   * Returns the args.
   */
  public Arg []getArgs()
  {
    return NULL_ARGS;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  abstract public Expr []bindArguments(Env env, Expr fun, Expr []args)
    throws Exception;

  /**
   * Evaluates the function.
   */
  abstract public Value eval(Env env, Value []args)
    throws Throwable;

  /**
   * Evaluates the function, returning a reference.
   */
  public Value evalRef(Env env, Value []args)
    throws Throwable
  {
    return eval(env, args);
  }

  /**
   * Evaluates the function, returning a copy
   */
  public Value evalCopy(Env env, Value []args)
    throws Throwable
  {
    return eval(env, args).copy();
  }
  
  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj, Value []args)
    throws Throwable
  {
    Value oldThis = env.getThis();

    try {
      env.setThis(obj);

      return eval(env, args);
    } finally {
      env.setThis(oldThis);
    }
  }
  
  /**
   * Evaluates the function as a method call, returning a reference.
   */
  public Value evalMethodRef(Env env, Value obj, Value []args)
    throws Throwable
  {
    Value oldThis = env.getThis();

    try {
      env.setThis(obj);

      return evalRef(env, args);
    } finally {
      env.setThis(oldThis);
    }
  }

  /**
   * Evaluates the function.
   */
  public Value eval(Env env)
    throws Throwable
  {
    return eval(env, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function with an argument .
   */
  public Value eval(Env env, Value a1)
    throws Throwable
  {
    return eval(env, new Value[] { a1 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value eval(Env env, Value a1, Value a2)
    throws Throwable
  {
    return eval(env, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value eval(Env env, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return eval(env, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value eval(Env env, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return eval(env, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value eval(Env env, Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return eval(env, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value eval(Env env, Expr []exprs)
    throws Throwable
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      // quercus/0d19
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return eval(env, argValues);
  }

  /**
   * Evaluates the function.
   */
  public Value evalCopy(Env env, Expr []exprs)
    throws Throwable
  {
    return eval(env, exprs).copy();
  }

  /**
   * Evaluates the function.
   */
  public Value evalRef(Env env)
    throws Throwable
  {
    return evalRef(env, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function with an argument .
   */
  public Value evalRef(Env env, Value a1)
    throws Throwable
  {
    return evalRef(env, new Value[] { a1 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value evalRef(Env env, Value a1, Value a2)
    throws Throwable
  {
    return evalRef(env, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value evalRef(Env env, Value a1, Value a2, Value a3)
    throws Throwable
  {
    return evalRef(env, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value evalRef(Env env, Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return evalRef(env, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function with arguments
   */
  public Value evalRef(Env env,
		       Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return evalRef(env, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value evalRef(Env env, Expr []exprs)
    throws Throwable
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      // quercus/0d19
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return evalRef(env, argValues);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj)
    throws Throwable
  {
    return evalMethod(env, obj, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj, Value a1)
    throws Throwable
  {
    return evalMethod(env, obj, new Value[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj, Value a1, Value a2)
    throws Throwable
  {
    return evalMethod(env, obj, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3)
    throws Throwable
  {
    return evalMethod(env, obj, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return evalMethod(env, obj, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethod(Env env, Value obj,
			  Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return evalMethod(env, obj, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value evalMethod(Env env, Value obj, Expr []exprs)
    throws Throwable
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference()) {
	argValues[i] = exprs[i].evalArg(env);
      }
      else
	argValues[i] = exprs[i].eval(env);
    }

    return evalMethod(env, obj, argValues);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj)
    throws Throwable
  {
    return evalMethodRef(env, obj, NULL_ARG_VALUES);
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj, Value a1)
    throws Throwable
  {
    return evalMethodRef(env, obj, new Value[] { a1 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj, Value a1, Value a2)
    throws Throwable
  {
    return evalMethodRef(env, obj, new Value[] { a1, a2 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3)
    throws Throwable
  {
    return evalMethodRef(env, obj, new Value[] { a1, a2, a3 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3, Value a4)
    throws Throwable
  {
    return evalMethodRef(env, obj, new Value[] { a1, a2, a3, a4 });
  }

  /**
   * Evaluates the function as a method call.
   */
  public Value evalMethodRef(Env env, Value obj,
			     Value a1, Value a2, Value a3, Value a4, Value a5)
    throws Throwable
  {
    return evalMethodRef(env, obj, new Value[] { a1, a2, a3, a4, a5 });
  }

  /**
   * Evaluates the function.
   */
  public Value evalMethodRef(Env env, Value obj, Expr []exprs)
    throws Throwable
  {
    Value []argValues = new Value[exprs.length];
    Arg []args = getArgs();

    for (int i = 0; i < exprs.length; i++) {
      if (i < args.length && args[i].isReference())
	argValues[i] = exprs[i].evalArg(env);
      else
	argValues[i] = exprs[i].eval(env);
    }

    return evalMethodRef(env, obj, argValues);
  }

  /**
   * Analyzes the function.
   */
  public void analyze()
  {
  }

  /**
   * Analyzes the arguments for read-only and reference.
   */
  public void analyzeArguments(Expr []args, AnalyzeInfo info)
  {
    for (int i = 0; i < args.length; i++) {
      args[i].analyzeSetModified(info);
      args[i].analyzeSetReference(info);
    }
  }

  /**
   * Returns true if the function can generate the call directly.
   */
  public boolean canGenerateCall(Expr []args)
  {
    return true;
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  abstract public void generate(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException;

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
  }
  
  /**
   * Generates code to evaluate as a top-level expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
  }
  
  /**
   * Generates code to evaluate as a boolean expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toBoolean()");
  }
  
  /**
   * Generates code to evaluate as a string expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toString()");
  }
  
  /**
   * Generates code to evaluate as a long expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toLong()");
  }
  
  /**
   * Generates code to evaluate as a double expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toDouble()");
  }
  
  /**
   * Generates code to evaluate as a double expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out, Expr funExpr, Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".copyReturn()");
  }
  
  /**
   * Generates the code for the class component.
   *
   * @param out the writer to the output stream.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
  }
  
  /**
   * Generates the code for the initialization component.
   *
   * @param out the writer to the output stream.
   */
  public void generateInit(PhpWriter out)
    throws IOException
  {
  }
}

