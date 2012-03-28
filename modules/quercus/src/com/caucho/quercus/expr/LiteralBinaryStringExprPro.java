/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP string literal expression.
 */
public class LiteralBinaryStringExprPro extends LiteralBinaryStringExpr
  implements ExprPro
{
  public LiteralBinaryStringExprPro(byte[] bytes)
  {
    super(bytes);
  }

  public LiteralBinaryStringExprPro(BinaryBuilderValue value)
  {
    super(value);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprType analyze(AnalyzeInfo info)
      {
        return ExprType.STRING;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        if (_value.length() == 0) {
          out.print("BinaryBuilderValue.EMPTY");
          /*
          if (out.getPhp().isUnicodeSemantics())
            out.print("BinaryBuilderValue.EMPTY");
          else
            out.print("StringBuilderValue.EMPTY");
          */
        }
        else {
          String var = out.addValue(_value);

          out.print(var);
        }
      }

      /**
       * Generates code to append to a string builder.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAppend(PhpWriter out)
        throws IOException
      {
        generate(out);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
        throws IOException
      {
        out.print("new BinaryLiteralExpr(\"");
        out.printJavaString(_value.toString());
        out.print("\")");
      }
    };
}

