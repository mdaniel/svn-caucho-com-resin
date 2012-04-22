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
abstract public class AbstractMethodGenerator extends ExprGenerator
{
  protected AbstractMethodGenerator(Location loc)
  {
    super(loc);
  }

  /**
   * Analyzes the function.
   */
  public ExprType analyzeArgs(AnalyzeInfo info, Expr []args)
  {
    for (int i = 0; i < args.length; i++) {
      ExprGenerator argGen = ((ExprPro) args[i]).getGenerator();

      argGen.analyze(info);

      argGen.analyzeSetReference(info);
      argGen.analyzeSetModified(info);
    }

    return ExprType.VALUE;
  }

  @Override
  public void generate(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
  }

  @Override
  public void generateRef(PhpWriter out)
    throws IOException
  {
    generateImpl(out, true);
    // out.print(".toRefVar()");
  }

  @Override
  public void generateVar(PhpWriter out)
    throws IOException
  {
    generateImpl(out, true);
    out.print(".toVar()");
  }

  @Override
  public void generateCopy(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
    out.print(".copyReturn()"); // php/3a5x
  }

  /**
   * Generates code to evaluate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateValue(PhpWriter out)
    throws IOException
  {
    generateImpl(out, false);
    out.print(".toValue()");  // php/3a5z
  }

  /**
   * Generates code to evaluate;
   *
   * @param out the writer to the Java source code.
   */
  protected abstract void generateImpl(PhpWriter out,
                                       boolean isRef)
    throws IOException;

  protected void generateArgs(PhpWriter out, Expr []args)
    throws IOException
  {
    if (args.length <= 5) {
      for (int i = 0; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        out.print(", ");

        arg.getGenerator().generateArg(out, true);
      }
    }
    else {
      out.print(", new Value[] {");

      for (int i = 0; i < args.length; i++) {
        ExprPro arg = (ExprPro) args[i];

        if (i != 0)
          out.print(", ");

        arg.getGenerator().generateArg(out, true);
      }

      out.print("}");
    }
  }
}

