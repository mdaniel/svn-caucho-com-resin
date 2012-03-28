/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.InterpretedClassDef;

import java.io.IOException;

/**
 * Represents the get_class() function.
 */
public class FunGetClassExprPro extends FunGetClassExpr
  implements ExprPro
{
  public FunGetClassExprPro(QuercusParser parser)
  {
    super(parser);
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
        out.print("env.createString(\"");
        out.printJavaString(getClassName());
        out.print("\")");
      }
    };
}

