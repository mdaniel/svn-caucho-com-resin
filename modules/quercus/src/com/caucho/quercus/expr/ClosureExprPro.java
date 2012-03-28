/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.Arg;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.program.ProFunction;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Creates a closure.
 */
public class ClosureExprPro extends ClosureExpr
  implements ExprPro
{
  private ArrayList<VarExpr> _useArgs;
  
  public ClosureExprPro(Location loc, Function fun, ArrayList<VarExpr> useArgs)
  {
    super(loc, fun);
    
    _useArgs = useArgs;
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    //
    // Java code generation
    //
    
    public ExprType analyze(AnalyzeInfo info)
    {
      for (VarExpr var : _useArgs) {
        ((VarExprPro) var).getGenerator().analyze(info);
      }
      
      return ExprType.VALUE;
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generate(PhpWriter out)
    throws IOException
    {
      Function fun = getFunction();
      
      out.print("new fun_" + fun.getCompilationName() + "(");
      
      Arg []args = fun.getClosureUseArgs();
      for (int i = 0; i < args.length; i++) {
        if (i != 0)
          out.print(", ");
        
        VarExprPro var = (VarExprPro) _useArgs.get(i);
        
        if (args[i].isReference())
          var.getGenerator().generateRef(out);
        else
          var.getGenerator().generate(out);
      }
      
      out.print(")");
    }
  };
}

