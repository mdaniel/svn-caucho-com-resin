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
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class ObjectMethodExprPro extends ObjectMethodExpr
  implements ExprPro
{
  private static final L10N L = new L10N(ObjectMethodExprPro.class);

  public ObjectMethodExprPro(Location location,
                           Expr objExpr,
                           String name,
                           ArrayList<Expr> args)
  {
    super(location, objExpr, name, args);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new AbstractMethodGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
        return ((ExprPro) _objExpr).getGenerator();
      }

      /**
       * Analyzes the function.
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        getExpr().analyze(info);

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

        getExpr().generate(out);

        String nameConst = out.addStringValue(_methodName);
        int hash = _methodName.hashCodeCaseInsensitive();

        out.print(".callMethod" + ref + "(env, "
                  + nameConst + ", " + hash);

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

