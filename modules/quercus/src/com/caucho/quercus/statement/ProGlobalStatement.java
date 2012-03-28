/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExprPro;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.expr.DummyGenerator;
import com.caucho.quercus.expr.ExprType;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.FunctionInfo;
import com.caucho.quercus.statement.GlobalStatement;

import java.io.IOException;

/**
 * Represents a global statement in a PHP program.
 */
public class ProGlobalStatement extends GlobalStatement
  implements CompilingStatement
{
  /**
   * Creates the echo statement.
   */
  public ProGlobalStatement(Location location, VarExpr var)
  {
    super(location, var);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProGlobalStatement.this.getLocation();
      }

      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	ExprGenerator varGen = ((ExprPro) _var).getGenerator();

	VarExprPro varInfo = info.getVar(_var.getName());

        // php/323c
	varGen.analyzeSetReference(info);

	ExprGenerator dummyValue = new DummyGenerator();
	
        varGen.analyzeAssign(info, dummyValue);
	if (varInfo == null && info.isInitialBlock()) {
	  ((VarExprPro) _var).setInitializedVar(true);
	}
	

        return true;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
	throws IOException
      {
	ExprGenerator varGen = ((ExprPro) _var).getGenerator();

	varGen.generateAssignRef(out, new GlobalExpr(), true);

	out.println(";");
	
	/*
	out.print(_var.getJavaVar());
	out.print(" = env.getGlobalVar(\"");
	out.printJavaString(_var.getName());
	out.println("\");");

	FunctionInfo funInfo = _var.getVarInfo().getFunction();
	if ((funInfo.isVariableVar() || funInfo.isUsesSymbolTable())) {
	  // php/3a84, php/3235
	  // php/3b29
	  out.print("if (! env.isGlobalEnv()) ");
	  out.print("env.setVar(\"");
	  out.printJavaString(_var.getName());
	  out.print("\", (Var) ");
	  out.print(_var.getJavaVar());
	  out.println(");");
	}
	*/
      }
    };
  
  class GlobalExpr extends Expr implements ExprPro {
    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }
    
    public ExprGenerator getGenerator()
    {
      return new GlobalGenerateExpr();
    }
  }
  
  class GlobalGenerateExpr extends ExprGenerator {
    public ExprType analyze(AnalyzeInfo info)
    {
      return ExprType.VALUE;
    }
    
    @Override
    public boolean isVar()
    {
      return true;
    }

    /**
     * Returns the variable from the global environment
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateVar(PhpWriter out)
      throws IOException
    {
      out.print("env.getGlobalVar(");
      out.printString(_var.getName());
      out.print(")");
    }

    @Override
    public void generateRef(PhpWriter out) 
      throws IOException 
    {
      // php/323f
      out.print("env.getGlobalVar(");
      out.printString(_var.getName());
      out.print(")");
    }
  }
}

