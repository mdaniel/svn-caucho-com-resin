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
 * Represents a PHP ${class}::FOO constant call expression.
 */
public class ClassVarConstExprPro extends ClassVarConstExpr
  implements ExprPro
{
  public ClassVarConstExprPro(Expr className, String name)
  {
    super(className, name);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }
  
  ExprGenerator getClassName()
  {
    return ((ExprPro) _className).getGenerator();
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyzes the function.
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        getClassName().analyze(info);
        
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
        out.print("env.getClass(");
        getClassName().generateString(out);
        out.print(").getConstant(env, \"");
        out.printJavaString(_name);
        out.print("\")");
      }
    };
}

