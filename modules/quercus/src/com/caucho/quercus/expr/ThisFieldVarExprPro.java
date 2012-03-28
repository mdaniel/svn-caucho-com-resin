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
 * Represents a PHP field reference.
 */
public class ThisFieldVarExprPro extends ThisFieldVarExpr
  implements ExprPro
{
  public ThisFieldVarExprPro(ThisExpr qThis, Expr nameExpr)
  {
    super(qThis, nameExpr);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
    private boolean _hasThis;
    
    @Override
    public boolean isVar()
    {
      // php/33ln
      return true;
    }
    
    /**
     * Analyze the statement
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      _hasThis = info.getFunction().hasThis();

      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      nameGen.analyze(info);

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
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".getThisField(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArg(PhpWriter out, boolean isTop)
      throws IOException
    {
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".getThisFieldArg(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateRef(PhpWriter out)
      throws IOException
    {
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".getThisFieldVar(env, ");
      nameGen.generateStringValue(out);
      out.print(")");
    }

    /**
     * Generates code to assign the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
      throws IOException
    {
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      out.print("q_this");
      out.print(".putThisField(env, ");
      nameGen.generateStringValue(out);
      out.print(", ");
      valueGen.generateCopy(out); // php/3a85
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
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      out.print("q_this");
      out.print(".putThisField(env, ");
      nameGen.generateStringValue(out);
      out.print(", ");
      valueGen.generateRef(out);
      out.print(")");
    }
    
    /**
     * Generates code for an array assignment $a[$index] = $value.
     */
    public void generateArrayAssign(PhpWriter out,
                                    ExprGenerator index,
                                    ExprGenerator value,
                                    boolean isTop)
      throws IOException
    {
      // php/33mn
      
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
      
      generateArray(out);
      out.print(".putThisFieldArray(");
      out.print("env, ");
      
      if (_hasThis) {
        out.print("q_this");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
      
      out.print(", ");
      
      nameGen.generateStringValue(out);
      out.print(", ");
      
      index.generate(out);
      out.print(", ");
      
      value.generateCopy(out); // php/3a5k
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
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".getThisFieldObject(env, ");
      nameGen.generateStringValue(out);
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
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".getThisFieldArray(env, ");
      nameGen.generateStringValue(out);
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
      if (_hasThis) {
        ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();
        
        out.print("q_this.getThisFieldVar(env, ");
        nameGen.generateStringValue(out);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to assign the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateUnset(PhpWriter out)
      throws IOException
    {
      ExprGenerator nameGen = ((ExprPro) _nameExpr).getGenerator();

      out.print("q_this");
      out.print(".unsetField(");
      nameGen.generateStringValue(out);
      out.print(")");
    }
  };
}

