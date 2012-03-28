/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.vfs.Path;

import java.io.IOException;

/**
 * Represents a PHP include statement
 */
public class FunIncludeOnceExprPro extends FunIncludeOnceExpr
  implements ExprPro
{
  public FunIncludeOnceExprPro(Location loc, Path sourceFile,
			    Expr expr, boolean isRequire)
  {
    super(loc, sourceFile, expr, isRequire);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractUnaryExprGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
	return ((ExprPro) _expr).getGenerator();
      }

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        super.analyze(info);

        // skip symbol table for cases like MediaWiki.
        // If the symbol table mattered, the application would use include
        // instead of include_once
        // php/3b2c
        info.getFunction().setUsesSymbolTable(true);

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
        out.print("env.includeOnce(");
        out.print("_quercus_selfDirectory, ");
        getExpr().generateStringValue(out);
        out.print(", " + _isRequire + ")");
      }
    };
}

