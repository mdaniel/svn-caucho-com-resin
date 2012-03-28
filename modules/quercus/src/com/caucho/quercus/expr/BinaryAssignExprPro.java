/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP assignment expression.
 */
public class BinaryAssignExprPro extends BinaryAssignExpr
  implements ExprPro
{
  public BinaryAssignExprPro(AbstractVarExpr var, Expr value)
  {
    super(var, value);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      private ExprGenerator getVar()
      {
	return ((ExprPro) _var).getGenerator();
      }
      
      /**
       * Returns true for an assignment expression.
       */
      public boolean isAssignment()
      {
        return true;
      }

      /**
       * Returns the native type
       */
      public ExprType getType()
      {
        return getVar().getType();
      }

      /**
       * Analyze the expression
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
        ExprGenerator value = ((ExprPro) _value).getGenerator();
	
        ExprType type = getVar().analyzeAssign(info, value);
        
        return type;
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
        throws IOException
      {
        getVar().generateAssign(out, _value, false);
      }
      
      /**
       * Generates code for a function arg.
       *
       * @param out the writer to the Java source code.
       */
      public void generateValue(PhpWriter out)
        throws IOException
      {
        generate(out);
        
        if (getVar().getType() ==  ExprType.VALUE)
          out.print(".toValue()");
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateLong(PhpWriter out)
        throws IOException
      {
        if (isLong())
          getVar().generateAssign(out, _value, true);
        else if (isDouble()) {
          out.print("(long) (");
          getVar().generateAssign(out, _value, true);
          out.print(")");
        }
        else if (isBoolean()) {
          out.print("((");
          getVar().generateAssign(out, _value, true);
          out.print(") ? 1 : 0)");
        }
        else {
          getVar().generateAssign(out, _value, false);
          out.print(".toLong()");
        }
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateDouble(PhpWriter out)
        throws IOException
      {
        if (isLong())
          getVar().generateAssign(out, _value, true);
        else if (isDouble())
          getVar().generateAssign(out, _value, true);
        else if (isBoolean()) {
          out.print("((");
          getVar().generateAssign(out, _value, true);
          out.print(") ? 1.0 : 0.0)");
        }
        else {
          getVar().generateAssign(out, _value, false);
          out.print(".toDouble()");
	}
      }
      
      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateBoolean(PhpWriter out)
        throws IOException
      {
        if (isNumber())
          getVar().generateAssignBoolean(out, _value, true);
        else if (isBoolean())
          getVar().generateAssignBoolean(out, _value, true);
        else {
          getVar().generateAssignBoolean(out, _value, false);
        }
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateRef(PhpWriter out)
        throws IOException
      {
        /* php/33d9 'true will not work because var may be a Java primitive
        // php/344m
        // the 'true' parameter isn't quite logical, but the effect is correct
        */
        getVar().generateAssign(out, _value, false);
      }

      /**
       * Generates code to evaluate the expression, copying the result
       *
       * @param out the writer to the Java source code.
       */
      public void generateCopy(PhpWriter out)
        throws IOException
      {
        generate(out);
        out.print(".copy()");  // php/3a5q
      }

      /**
       * Generates code to evaluate the expression
       *
       * @param out the writer to the Java source code.
       */
      public void generateTop(PhpWriter out)
        throws IOException
      {
        getVar().generateAssign(out, _value, true);
      }
    };
}

