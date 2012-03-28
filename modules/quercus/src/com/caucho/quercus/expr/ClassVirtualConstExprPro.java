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
 * Represents a PHP parent::FOO constant call expression.
 */
public class ClassVirtualConstExprPro
  extends ClassVirtualConstExpr
  implements ExprPro
{
  public ClassVirtualConstExprPro(String name)
  {
    super(name);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyzes the function.
       */
      public ExprType analyze(AnalyzeInfo info)
      {
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
	out.print("env.getCallingClass(q_this).getConstant(env, \"");
	out.printJavaString(_name);
	out.print("\")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new LateStaticBindingClassConstExpr(");
	out.print("\"");
	out.printJavaString(_name);
	out.print("\")");
      }
    };
}

