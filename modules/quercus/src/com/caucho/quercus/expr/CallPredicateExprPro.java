/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.gen.AnalyzeInfo;

import java.util.ArrayList;
import java.io.IOException;

/**
 * Represents predicates on the value object
 */
public class CallPredicateExprPro extends CallExpr
  implements ExprPro
{
  private Expr _valueExpr;
  private String _methodName;
  
  public CallPredicateExprPro(Location location, String name,
			  ArrayList<Expr> args,
			  String methodName)
  {
    super(location, name, args);

    _valueExpr = args.get(0);
    _methodName = methodName;
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprGenerator getValue()
      {
	return ((ExprPro) _valueExpr).getGenerator();
      }
      
      public ExprType analyze(AnalyzeInfo info)
      {
	getValue().analyze(info);

	return ExprType.BOOLEAN;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
	throws IOException
      {
	getValue().generate(out);

	out.print("." + _methodName + "()");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	out.print("BooleanValue.create(");
	generateBoolean(out);
	out.print(")");
      }
    };
}

