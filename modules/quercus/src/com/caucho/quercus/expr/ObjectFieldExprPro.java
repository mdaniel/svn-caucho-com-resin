/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.env.StringValue;

import java.io.IOException;

/**
 * Represents a PHP field reference.
 */
public class ObjectFieldExprPro extends ObjectFieldExpr
  implements ExprPro
{
  public ObjectFieldExprPro(Expr objExpr, StringValue name)
  {
    super(objExpr, name);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    @Override
    public boolean isVar()
    {
      return true;
    }
    
      /**
       * Analyze the statement
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.analyze(info);

        return ExprType.VALUE;
      }

      /**
       * Analyze the statement as an assignement
       */
      @Override
      public ExprType analyzeAssign(AnalyzeInfo info, ExprGenerator value)
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
        objGen.analyze(info);

        value.analyze(info);

        // php/3a6e, php/39o3
        // objGen.analyzeSetReference(info);
        objGen.analyzeSetModified(info);

        return ExprType.VALUE;
      }

      /**
       * Analyze the statement as modified
       */
      public void analyzeSetModified(AnalyzeInfo info)
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.analyzeSetModified(info);
      }

      /**
       * Analyze the statement as a reference
       */
      public void analyzeSetReference(AnalyzeInfo info)
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        // php/3a6f
        objGen.analyzeSetReference(info);
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

        objGen.generate(out);
        out.print(".getField(env, ");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateArg(PhpWriter out, boolean isTop)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateArg(out, false);
        out.print(".getFieldArg(env, ");
        out.printIntern(_name);
        out.print(", " + isTop + ")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateDirty(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateObject(out);
        out.print(".getField(env, ");
        out.printIntern(_name);
        out.print(")"); // php/3228 create the field if necessary
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateRef(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateVar(out);
        out.print(".getFieldVar(env, ");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateVar(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateVar(out);
        out.print(".getFieldVar(env, ");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression, where the result is copied.
       *
       * @param out the writer to the Java source code.
       */
      public void generateCopy(PhpWriter out)
        throws IOException
      {
        generate(out);
        out.print(".copy()");
      }

      /**
       * Generates code to assign the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssign(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        objGen.generateObject(out);
        out.print(".putField(env, ");
        out.printIntern(_name);
        out.print(", ");
        valueGen.generateCopy(out);
        out.print(")");
      }

      /**
       * Generates code to assign the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        objGen.generateObject(out);
        out.print(".putField(env, ");
        out.printIntern(_name);
        out.print(", ");
        valueGen.generateRef(out);
        out.print(")");
      }

      @Override
      public void generateAssignOpen(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateObject(out);
        out.print(".putField(env, ");
        out.printIntern(_name);
        out.print(", ");
      }

      @Override
      public void generateAssignClose(PhpWriter out)
        throws IOException
      {
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateObject(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateObject(out);
        out.print(".getFieldObject(env, ");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateArray(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generateObject(out);
        out.print(".getFieldArray(env, ");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to assign the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateUnset(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generate(out);
        out.print(".unsetField(");
        out.printIntern(_name);
        out.print(")");
      }

      /**
       * Generates code to unset the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateUnsetArray(PhpWriter out, ExprGenerator index)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        objGen.generate(out);
        out.print(".unsetArray(env, ");
        out.print(_name);
        out.print(", ");

        index.generate(out);
        out.print(")");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateExpr(PhpWriter out)
        throws IOException
      {
        ExprGenerator objGen = ((ExprPro) _objExpr).getGenerator();

        out.print("new ObjectFieldExpr(");

        objGen.generateExpr(out);

        out.print(", \"");

        out.printJavaString(_name.toString());

        out.print("\")");
      }
    };
}

