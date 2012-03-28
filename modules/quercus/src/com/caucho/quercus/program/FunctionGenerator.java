/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Represents a function
 */
abstract public class FunctionGenerator {
  private static final L10N L = new L10N(FunctionGenerator.class);

  private static final Arg []NULL_ARGS = new Arg[0];
  private static final Value []NULL_ARG_VALUES = new Value[0];

  private final Location _location;

  private boolean _isGlobal = true;

  protected FunctionGenerator()
  {
    // XXX:
    _location = Location.UNKNOWN;
  }

  protected FunctionGenerator(Location location)
  {
    _location = location;
  }

  public String getName()
  {
    return "unknown";
  }

  /**
   * Returns true for a global function.
   */
  public final boolean isGlobal()
  {
    return _isGlobal;
  }

  /**
   * Returns true for an abstract function.
   */
  public boolean isAbstract()
  {
    return false;
  }

  public final Location getLocation()
  {
    return _location;
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
   * Returns true if the function uses/modifies the local symbol table
   */
  public boolean isCallReplacesSymbolTable()
  {
    return isCallUsesSymbolTable();
  }

  /**
   * True for a returns reference.
   */
  public boolean isReturnsReference()
  {
    return true;
  }
  
  /**
   * True if the return type is void.
   */
  public boolean isVoidReturn()
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
   * Analyzes the function.
   */
  public void analyze(QuercusProgram program)
  {
  }

  /**
   * Analyzes the arguments for read-only and reference.
   */
  public void analyzeArguments(Expr []args, AnalyzeInfo info)
  {
    for (int i = 0; i < args.length; i++) {
      ExprPro arg = (ExprPro) args[i];
      
      arg.getGenerator().analyzeSetModified(info);
      arg.getGenerator().analyzeSetReference(info);
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
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  abstract public void generate(PhpWriter out,
				ExprGenerator funExpr,
				Expr []args)
    throws IOException;

  /**
   * Generates code to calluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateRef(PhpWriter out,
			  ExprGenerator funExpr,
			  Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
  }
  
  /**
   * Generates code to calluate as a top-level expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateTop(PhpWriter out,
			  ExprGenerator funExpr,
			  Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
  }
  
  /**
   * Generates code to calluate as a boolean expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateBoolean(PhpWriter out,
			      ExprGenerator funExpr,
			      Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toBoolean()");
  }
  
  /**
   * Generates code to calluate as a string expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateString(PhpWriter out,
			     ExprGenerator funExpr,
			     Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toString()");
  }
  
  /**
   * Generates code to calluate as a long expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateLong(PhpWriter out,
			   ExprGenerator funExpr,
			   Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toLong()");
  }
  
  /**
   * Generates code to calluate as a double expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateDouble(PhpWriter out,
			     ExprGenerator funExpr,
			     Expr []args)
    throws IOException
  {
    generate(out, funExpr, args);
    out.print(".toDouble()");
  }
  
  /**
   * Generates code to calluate as a double expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateCopy(PhpWriter out,
			   ExprGenerator funExpr,
			   Expr []args)
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

