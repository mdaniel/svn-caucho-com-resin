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
public class ObjectMethodVarExprPro extends ObjectMethodVarExpr
  implements ExprPro
{
  public ObjectMethodVarExprPro(Location location,
			      Expr objExpr,
			      Expr name,
			      ArrayList<Expr> args)
  {
    super(location, objExpr, name, args);
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
	ExprGenerator objExprGen = ((ExprPro) _objExpr).getGenerator();
	objExprGen.analyze(info);
	
	ExprGenerator nameGen = ((ExprPro) _name).getGenerator();
	nameGen.analyze(info);
    
	for (int i = 0; i < _args.length; i++) {
	  ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
	  
	  argGen.analyze(info);
	}

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
	ExprGenerator objExprGen = ((ExprPro) _objExpr).getGenerator();
	ExprGenerator nameGen = ((ExprPro) _name).getGenerator();
	
	objExprGen.generate(out);
	out.print(".callMethod(env, ");
        
	nameGen.generate(out);
        out.print(".toStringValue()");

	if (_args.length <= 5) {
	  for (int i = 0; i < _args.length; i++) {
	    ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
	    
	    out.print(", ");
      
	    argGen.generateArg(out, true);
	  }
	}
	else {
	  out.print(", new Value[] {");

	  for (int i = 0; i < _args.length; i++) {
	    ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
	    
	    if (i != 0)
	      out.print(", ");
      
	    argGen.generateArg(out, true);
	  }
      
	  out.print("}");
	}
      
	out.print(")");
      }
    };
}

