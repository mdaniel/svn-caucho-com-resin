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
 * Represents a PHP isset call
 */
public class FunDefineExprPro extends CallExpr
  implements ExprPro
{
  private Expr _nameExpr;
  private Expr _valueExpr;
  
  public FunDefineExprPro(Location location, String name,
		       ArrayList<Expr> args)
  {
    super(location, name, args);

    _nameExpr = args.get(0);
    _valueExpr = args.get(1);
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
	((ExprPro) _nameExpr).getGenerator().analyze(info);
	return getValue().analyze(info);
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	String name = _nameExpr.eval(null).toString();

	String id = out.addConstantId(name);
	
	out.print("env.addConstant(");
	out.print(id);
	out.print(", ");

	getValue().generate(out);
	
	out.print(", false)");
      }
    };
}

