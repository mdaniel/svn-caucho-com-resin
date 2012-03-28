/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents the exit expression
 */
public class FunExitExprPro extends FunExitExpr
  implements ExprPro
{
  public FunExitExprPro(Expr value)
  {
    super(value);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	ExprPro value = (ExprPro) _value;
	
	if (value != null)
	  value.getGenerator().analyze(info);

	return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	    throws IOException
      {
        ExprPro value = (ExprPro) _value;
        
        if (value != null) {
          out.print("env.exit(");
          value.getGenerator().generate(out);
          
          // php/33k6
          out.print(".toValue())");
        }
        else {
          out.print("env.exit()");
        }
      }
    };
}

