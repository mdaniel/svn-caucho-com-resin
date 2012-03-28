/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Represents a PHP append ('.') expression.
 */
public class BinaryAppendExprPro extends BinaryAppendExpr
  implements ExprPro
{
  protected BinaryAppendExprPro(Expr value, BinaryAppendExprPro next)
  {
    super(value, next);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprGenerator getValue()
      {
        return ((ExprPro) BinaryAppendExprPro.this.getValue()).getGenerator();
      }
      
      /**
       * Returns true for a string.
       */
      public boolean isString()
      {
        return true;
      }

      /**
       * Analyze the expression
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        getValue().analyze(info);

        if (getNext() != null)
          ((ExprPro) getNext()).getGenerator().analyze(info);

        return ExprType.STRING;
      }

      /**
       * Generates code to evaluate the expression as a string.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        Expr valueExpr = BinaryAppendExprPro.this.getValue();
        BinaryAppendExpr ptr = getNext();

        if (valueExpr instanceof LiteralStringExpr) {
          LiteralStringExpr literal = (LiteralStringExpr) valueExpr;
          String string = literal.evalConstant().toString();
          
          if (! out.getPhp().isUnicodeSemantics())
            out.print("new StringBuilderValue(");
          else if (valueExpr instanceof LiteralBinaryStringExpr)
            out.print("new BinaryBuilderValue(");
          else
            out.print("new UnicodeBuilderValue(");

          if (! "".equals(string)) {
            String charArray = out.addCharArray(string);

            out.print(charArray);
          }

          if (ptr != null && ! hasCustomAppend(ptr.getValue())) {
            ExprPro value = (ExprPro) ptr.getValue();

            if (! "".equals(string))
              out.print(", ");
            
            value.getGenerator().generate(out);

            ptr = ptr.getNext();
          }

          out.print(")");
        }
        else {
          getValue().generate(out);
          out.print(".toStringBuilder(env");

          if (ptr != null) {
            ExprPro value = (ExprPro) ptr.getValue();
          
            out.print(", ");
            value.getGenerator().generateValue(out);

            ptr = ptr.getNext();
          }
          
          out.print(")");
        }

        for (; ptr != null; ptr = ptr.getNext()) {
          ExprPro value = (ExprPro) ptr.getValue();
          
          out.print(".appendUnicode(");
          value.getGenerator().generateAppend(out);
          out.print(")");
        }
      }

      /**
       * Generates code to evaluate the expression as a string.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateStringValue(PhpWriter out)
        throws IOException
      {
        generate(out);
      }

      /**
       * Generates code to evaluate the expression as a string.
       *
       * @param out the writer to the Java source code.
       */
      private void generateSubAppend(PhpWriter out)
        throws IOException
      {
        for (BinaryAppendExprPro ptr = BinaryAppendExprPro.this;
             ptr != null;
             ptr = (BinaryAppendExprPro) ptr.getNext()) {
          out.print(".appendUnicode(");
          ((ExprPro) ptr.getValue()).getGenerator().generateAppend(out);
          out.print(")");
        }
      }

      /**
       * Generates code to evaluate the expression as a string.
       *
       * @param out the writer to the Java source code.
       */
      public void generateString(PhpWriter out)
        throws IOException
      {
        getValue().generate(out);
        out.print(".toStringBuilder(env)");
        
        for (BinaryAppendExprPro ptr = (BinaryAppendExprPro) BinaryAppendExprPro.this.getNext();
             ptr != null;
             ptr = (BinaryAppendExprPro) ptr.getNext()) {
          ExprPro value = (ExprPro) ptr.getValue();
     
          out.print(".append(");
          value.getGenerator().generateAppend(out);
          out.print(")");
        }

        out.print(".toString()");
      }

      /**
       * Generates code to print the expression to the output
       *
       * @param out the writer to the Java source code.
       */
      public void generatePrint(PhpWriter out)
        throws IOException
      {
        for (BinaryAppendExpr ptr = BinaryAppendExprPro.this;
             ptr != null;
             ptr = (BinaryAppendExprPro) ptr.getNext()) {
          ((ExprPro) ptr.getValue()).getGenerator().generatePrint(out);
        }
      }
    };

  /**
   * Returns true if the expr class customizes the generateAppend
   * method.
   */
  static boolean hasCustomAppend(Expr expr)
  {
    try {
      ExprGenerator gen = ((ExprPro) expr).getGenerator();
      Class cl = gen.getClass();
      
      Method method = cl.getMethod("generateAppend",
				   new Class[] { PhpWriter.class });

      if (method != null
          && method.getDeclaringClass().equals(ExprGenerator.class))
        return false;
      else
        return true;
    } catch (Exception e) {
      return true;
    }
  }
}
