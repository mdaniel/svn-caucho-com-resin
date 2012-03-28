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

/**
 * Represents a PHP comparison expression.
 */
public class FunImportExprPro extends ImportExpr
  implements ExprPro
{
  public FunImportExprPro(Location location, String name, boolean isWildcard)
  {
    super(location, name, isWildcard);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {

      //
      // Generates the java code
      //

      /**
       * Analyze the expression
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	return ExprType.VALUE;
      }
      
      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        if (_isWildcard) {
          out.print("env.addWildcardImport(\"");
        }
        else {
          out.print("env.putQualifiedImport(\"");
        }
        
        out.printJavaString(_name);
        out.print("\")");
      }
    };
}

