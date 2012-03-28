/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class ObjectNewExprPro extends ObjectNewExpr
  implements ExprPro
{
  private static final L10N L = new L10N(ObjectNewExprPro.class);

  public ObjectNewExprPro(Location loc, String name, ArrayList<Expr> args)
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
      @Override
      public void generate(PhpWriter out)
        throws IOException
      {
        String index = out.addClassId(_name);
        out.print("env.getClass(" + index + ").callNew(env");

        if (_args.length > 0) {
          // out.print("new Value[] {");

          for (int i = 0; i < _args.length; i ++) {
            ExprPro arg = (ExprPro) _args[i];

            out.print(", ");

                // php/394g
            arg.getGenerator().generateRef(out);
          }
          // out.print("}");
        }
        else
          out.print(", Value.NULL_ARGS");

        out.print(")");
      }
    };
}
