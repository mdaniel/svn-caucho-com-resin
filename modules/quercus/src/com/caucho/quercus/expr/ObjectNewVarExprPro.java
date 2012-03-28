/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class ObjectNewVarExprPro extends ObjectNewVarExpr
  implements ExprPro
{
  public ObjectNewVarExprPro(Location loc, Expr name, ArrayList<Expr> args)
  {
    super(loc, name, args);
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
	ExprPro name = (ExprPro) _name;
	
	name.getGenerator().analyze(info);
    
	for (int i = 0; i < _args.length; i++) {
	  ExprPro arg = (ExprPro) _args[i];
	  
	  arg.getGenerator().analyze(info);
	}

	return ExprType.VALUE;
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	ExprPro name = (ExprPro) _name;
	
	out.print("env.findAbstractClass(");
	name.getGenerator().generateString(out);
	out.print(").callNew(env, new Value[] {");

	for (int i = 0; i < _args.length; i++) {
	  ExprPro arg = (ExprPro) _args[i];
	  
	  if (i != 0)
	    out.print(", ");
      
	  arg.getGenerator().generate(out);
	}
	out.print("})");
      }
    };
}

