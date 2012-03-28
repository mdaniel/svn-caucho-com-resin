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
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.InterpretedClassDef;

import java.io.IOException;

/**
 * Represents the get_called_class() function.
 */
public class FunGetCalledClassExprPro extends FunGetCalledClassExpr
  implements ExprPro
{
  public FunGetCalledClassExprPro(Location location)
  {
    super(location);
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
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        out.print("env.createString(");
        
        if (_hasThis)
          out.print("q_this");
        else
          out.print("NullValue.NULL");
        
        out.print(".getClassName())");
      }
    };
}

