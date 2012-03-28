/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Dummy for code generation
 */
public class BinarySetCharAtExprPro extends Expr
  implements ExprPro
{
  private Expr _objExpr;
  private Expr _indexExpr;
  private Expr _valueExpr;
  
  public BinarySetCharAtExprPro(Expr objExpr, Expr indexExpr, Expr valueExpr)
  {

    _objExpr = objExpr;
    _indexExpr = indexExpr;
    _valueExpr = valueExpr;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    throw new UnsupportedOperationException();
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
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
	ExprGenerator indexGen = ((ExprPro) _indexExpr).getGenerator();
	ExprGenerator valueGen = ((ExprPro) _valueExpr).getGenerator();
	
	objGen.generate(out);
	out.print(".setCharValueAt(");
	indexGen.generateLong(out);
	out.print(",");
	valueGen.generate(out);
	out.print(")");
      }
    };
}

