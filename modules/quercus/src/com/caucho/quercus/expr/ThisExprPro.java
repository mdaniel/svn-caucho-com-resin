/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.InterpretedClassDef;

import java.io.IOException;

/**
 * Represents the 'this' expression.
 */
public class ThisExprPro extends ThisExpr
  implements ExprPro
{
  public ThisExprPro(InterpretedClassDef quercusClass)
  {
    super(quercusClass);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      private boolean _hasThis;
      
      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        _hasThis = info.getFunction().hasThis();

	return ExprType.VALUE;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        if (_hasThis)
          out.print("q_this");
        else {
          // php/0962
          out.print("UnsetValue.NULL");
        }
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssign(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        throw new UnsupportedOperationException();
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        throw new UnsupportedOperationException();
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateUnset(PhpWriter out)
        throws IOException
      {
        throw new UnsupportedOperationException();
      }
    };
}

