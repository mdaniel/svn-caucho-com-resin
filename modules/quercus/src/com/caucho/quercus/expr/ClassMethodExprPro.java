/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.MethodMap;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.Arg;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP parent:: method call expression.
 */
public class ClassMethodExprPro extends ClassMethodExpr
  implements ExprPro
{
  public ClassMethodExprPro(Location location,
			    String className,
			    String methodName,
			    ArrayList<Expr> args)
  {
    super(location, className, methodName, args);
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
      public void generateImpl(PhpWriter out, boolean isRef)
        throws IOException
      {
        String ref = isRef ? "Ref" : "";
        
        out.print("env.getClass(\"");
        out.printJavaString(_className);
        out.print("\").callMethod" + ref + "(env, ");
        
        // php/09q3, php/324b, php/3953

        if (isMethod())
          out.print("q_this, ");

        String nameConst = out.addStringValue(_methodName);

        out.print(nameConst + ", " + _methodName.hashCodeCaseInsensitive());

        generateArgs(out, _args);

        out.print(")");
      }
  
      public String toString()
      {
	return _className + "::" + _methodName + "()";
      }
    };
}

