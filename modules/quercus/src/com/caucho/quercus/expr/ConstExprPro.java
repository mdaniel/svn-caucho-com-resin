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
 * Represents a PHP constant expression.
 */
public class ConstExprPro extends ConstExpr
  implements ExprPro
{
  public ConstExprPro(String var)
  {
    super(var);
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
	// XXX: can analyze the const?
	
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
        String id = out.addConstantId(_var);

        out.print("env.getConstant(" + id + ")");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new ConstExpr(\"");
	out.printJavaString(_var);
	out.print("\")");
      }
    };
}

