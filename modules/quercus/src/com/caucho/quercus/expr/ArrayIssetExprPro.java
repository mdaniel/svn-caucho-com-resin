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
 * Represents a PHP array is set expression.
 */
public class ArrayIssetExprPro extends ArrayIsSetExpr
  implements ExprPro
{
  public ArrayIssetExprPro(Expr expr, Expr index)
  {
    super(expr, index);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public boolean isBoolean()
      {
	return true;
      }

      //
      // java code generation
      //

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	ExprPro expr = (ExprPro) _expr;
	ExprPro index = (ExprPro) _index;
	
	expr.getGenerator().analyze(info);
	index.getGenerator().analyze(info);

	return ExprType.BOOLEAN;
      }
      
      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
	throws IOException
      {
	ExprPro expr = (ExprPro) _expr;
	ExprPro index = (ExprPro) _index;
	
	out.print("(");
	expr.getGenerator().generate(out);
	out.print(".get(");
	index.getGenerator().generate(out);
	out.print(") != UnsetValue.UNSET)");
      }
    };
}

