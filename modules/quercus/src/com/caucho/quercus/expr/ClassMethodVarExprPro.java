/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.env.MethodMap;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP static method expression A::$f().
 */
public class ClassMethodVarExprPro extends ClassMethodVarExpr
  implements ExprPro
{
  public ClassMethodVarExprPro(Location location,
                               String className,
                               Expr nameExpr,
                               ArrayList<Expr> args)
  {
    super(location, className, nameExpr, args);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractMethodGenerator(getLocation()) {
      /**
       * Analyzes the function.
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        _isMethod = info.getFunction().isMethod();
        
        ExprPro name = (ExprPro) _nameExpr;
        
        ExprGenerator nameGen = name.getGenerator();
        nameGen.analyze(info);
        
        analyzeArgs(info, _args);

        return ExprType.VALUE;
      }

      private boolean isMethod()
      {
	return _isMethod;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      protected void generateImpl(PhpWriter out, boolean isRef)
        throws IOException
      {
        String ref = isRef ? "Ref" : "";
        
        ExprPro nameExpr = (ExprPro) _nameExpr;

        out.print("env.getClass(\"");
        out.printJavaString(_className);
        
        out.print("\").callMethod" + ref + "(env, ");

        // php/09q3
        if (isMethod())
          out.print("q_this");
        else
          out.print("NullThisValue.NULL");

        out.print(", ");
        nameExpr.getGenerator().generateStringValue(out);

        generateArgs(out, _args);
          
        out.print(")");
      }
    };
}

