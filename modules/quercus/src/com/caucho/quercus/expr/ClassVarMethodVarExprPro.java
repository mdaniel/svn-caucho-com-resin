/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.MethodMap;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.Arg;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP ${class}::$foo method call expression.
 */
public class ClassVarMethodVarExprPro extends ClassVarMethodVarExpr
  implements ExprPro
{
  public ClassVarMethodVarExprPro(Location location,
                                  Expr className,
                                  Expr methodName,
                                  ArrayList<Expr> args)
  {
    super(location, className, methodName, args);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractMethodGenerator(getLocation()) {
      public ExprGenerator getClassName()
      {
        return ((ExprPro) _className).getGenerator();
      }

      public ExprGenerator getMethodName()
      {
        return ((ExprPro) _methodName).getGenerator();
      }

      /**
       * Analyzes the function.
       */
      public ExprType analyze(AnalyzeInfo info)
      {
        getClassName().analyze(info);
        getMethodName().analyze(info);

        _isMethod = info.getFunction().isMethod();

        analyzeArgs(info, _args);

        return ExprType.VALUE;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      protected void generateImpl(PhpWriter out, boolean isRef)
        throws IOException
      {
        String ref = isRef ? "Ref" : "";
        
        out.print("env.getClass(");
        getClassName().generateString(out);
        out.print(").callMethod" + ref + "(env, ");
        
        if (_isMethod)
          out.print("q_this, ");
        else
          out.print("NullValue.NULL, ");
        
        getMethodName().generateStringValue(out);

        generateArgs(out, _args);

        out.print(")");
      }

      public String toString()
      {
        return _className + "::" + _methodName + "()";
      }
    };
}

