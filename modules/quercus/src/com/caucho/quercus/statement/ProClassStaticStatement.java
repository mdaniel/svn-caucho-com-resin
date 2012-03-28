/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.ExprPro;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprType;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.expr.DummyGenerator;
import com.caucho.quercus.program.FunctionInfo;

import java.io.IOException;

/**
 * Represents a static statement in a PHP program.
 */
public class ProClassStaticStatement extends ClassStaticStatement
  implements CompilingStatement
{
  private String _varName;
  
  /**
   * Creates the static statement.
   */
  public ProClassStaticStatement(Location location,
                                 String className,
                                 VarExpr var,
                                 Expr initValue)
  {
    super(location, className, var, initValue);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      private FunctionInfo _fun;
      
      protected Location getLocation()
      {
        return ProClassStaticStatement.this.getLocation();
      }

      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
        _fun = info.getFunction();

        // php/3240
        // php/3247
        //if (_fun != null)
        //  _fun.setUsesSymbolTable(true);

        ExprGenerator varGen = ((ExprPro) _var).getGenerator();

        ExprGenerator dummyValue = new DummyGenerator();
	
        varGen.analyzeSetReference(info);
        varGen.analyzeAssign(info, dummyValue);

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
        _varName = out.createStaticVar();
	
        ExprGenerator varGen = ((ExprPro) _var).getGenerator();

        varGen.generateAssignRef(out, new StaticExpr(), true);
        out.println(";");

        if (_initValue != null) {
          ExprGenerator initGen = ((ExprPro) _initValue).getGenerator();

          out.print("if (! ");
          varGen.generate(out);
          out.println(".isset()) {");
          out.pushDepth();
          varGen.generateAssign(out, _initValue, true);
          out.println(";");
          out.popDepth();
          out.println("}");
        }
      }
      /*
      out.print("env.getStaticVar(\"");
      out.printJavaString(_var.getName());
      out.print("\")");
      */
    };
  
  class StaticExpr extends Expr implements ExprPro {
    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }
    
    public ExprGenerator getGenerator()
    {
      return new StaticGenerateExpr();
    }
  }
  
  class StaticGenerateExpr extends ExprGenerator {
    public ExprType analyze(AnalyzeInfo info)
    {
      return ExprType.VALUE;
    }

    /**
     * Returns the variable from the global environment
     *
     * @param out the writer to the Java source code.
     */
    @Override
      public void generate(PhpWriter out)
      throws IOException
    {
      // php/3248
      out.print("env.getStaticVar(env.createString(");
      
      out.print("q_this.getQuercusClass().getName()");
      out.print(" + \"");
      out.printString("::" + _varName);
      
      out.print("\"))");

      /*
	// php/3247
	if (_fun == null || _fun.isUsesSymbolTable() || _fun.isVariableVar()) {
	  out.print("env.setValue(\"" + _var.getName());
	  out.print("\", ");
	  out.print(_var.getJavaVar());
	  out.println(");");
	}
      */
    }
  }
}

