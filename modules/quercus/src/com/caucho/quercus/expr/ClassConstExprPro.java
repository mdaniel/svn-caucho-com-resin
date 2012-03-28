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
public class ClassConstExprPro extends ClassConstExpr
  implements ExprPro
{
  public ClassConstExprPro(String className, String name)
  {
    super(className, name);
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
	out.print("env.getClass(\"");
	out.printJavaString(_className);
	out.print("\").getConstant(env, \"");
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
	out.print("new ClassConstExpr(");
	out.print("\"");
	out.printJavaString(_className);
	out.print("\", \"");
	out.printJavaString(_name);
	out.print("\")");
      }
    };
}

