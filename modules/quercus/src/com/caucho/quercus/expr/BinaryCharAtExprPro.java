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
 * Represents the character at expression
 */
public class BinaryCharAtExprPro extends BinaryCharAtExpr
  implements ExprPro {
  
  public BinaryCharAtExprPro(Expr objExpr, Expr indexExpr)
  {
    super(objExpr, indexExpr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      ExprGenerator getObjGen()
      {
	return ((ExprPro) _objExpr).getGenerator();
      }
      
      ExprGenerator getIndexGen()
      {
	return ((ExprPro) _indexExpr).getGenerator();
      }
      
      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        // quercus/3a0w
    
        getObjGen().analyze(info);
        getIndexGen().analyze(info);

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
	getObjGen().generate(out);
	out.print(".charValueAt(");
	getIndexGen().generateLong(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssign(PhpWriter out, Expr valueExpr, boolean isTop)
	throws IOException
      {
	if (_objExpr instanceof AbstractVarExpr) {
	  AbstractVarExpr varExpr = (AbstractVarExpr) _objExpr;

	  getObjGen().generateAssign(out, new BinarySetCharAtExprPro(_objExpr,
							       _indexExpr,
							       valueExpr),
				     isTop);
	}
	else {
	  ExprPro value = (ExprPro) valueExpr;
	  
	  getObjGen().generate(out);
	  out.print(".setCharValueAt(");
	  getIndexGen().generateLong(out);
	  out.print(", ");
	  value.getGenerator().generate(out);
	  out.print(")");
	}
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssignRef(PhpWriter out, Expr valueExpr, boolean isTop)
	throws IOException
      {
	generateAssign(out, valueExpr, isTop);
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateUnset(PhpWriter out)
	throws IOException
      {
	throw new UnsupportedOperationException();
      }
    };
}

