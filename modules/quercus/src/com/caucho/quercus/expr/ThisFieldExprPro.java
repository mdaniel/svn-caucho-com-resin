/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.InterpretedClassDef;

/**
 * Represents a PHP field reference.
 */
public class ThisFieldExprPro extends ThisFieldExpr
  implements ExprPro
{
  InterpretedClassDef _quercusClass;
  
  public ThisFieldExprPro(ThisExpr qThis,
                          StringValue name)
  {
    super(qThis, name);
    
    _quercusClass = qThis.getQuercusClass();
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
      // php/33lm
      return true;
    }
    /**
     * Analyze the statement
     */
    public ExprType analyze(AnalyzeInfo info)
    {
      _hasThis = info.getFunction().hasThis();

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
      if (false && _quercusClass.isDeclaredField(_name)) {
        out.print("q_this._fields[_f_" + _name + "].toValue()");
      }
      else if (_hasThis) {
        out.print("q_this.getThisField(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateRef(PhpWriter out)
      throws IOException
    {
      if (_hasThis) {
        out.print("q_this.getThisFieldVar(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }

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
        out.print("q_this.getThisFieldVar(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression, as a copy.
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
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArg(PhpWriter out, boolean isTop)
      throws IOException
    {
      if (_hasThis) {
        out.print("q_this.getThisFieldArg(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression, creating an array for the
     * field value if unset.
     *
     * @param out the writer to the Java source code.
     */
    public void generateArray(PhpWriter out)
      throws IOException
    {
      if (_hasThis) {
        out.print("q_this.getThisFieldArray(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
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
      // php/33mm
      // php/344i
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
      
      out.print(_name);
      out.print(", ");
      
      index.generate(out);
      out.print(", ");
      
      value.generateCopy(out); // php/3a5k
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression, creating an object for the
     * field value if unset.
     *
     * @param out the writer to the Java source code.
     */
    public void generateObject(PhpWriter out)
      throws IOException
    {
      if (_hasThis) {
        out.print("q_this.getThisFieldObject(env, ");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssign(PhpWriter out, Expr value, boolean isTop)
      throws IOException
    {
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      if (false && _quercusClass.isDeclaredField(_name)) {
        if (! isTop)
          out.print("(");

        out.print("q_this._fields[_f_" + _name + "] = ");
        out.print("q_this._fields[_f_" + _name + "].set(");

        valueGen.generateCopy(out);

        out.print(")");

        if (! isTop)
          out.print(")");
      }
      else if (_hasThis) {
        out.print("q_this.putThisField(env, ");
        out.print(_name);
        out.print(", ");
        valueGen.generateCopy(out);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
      throws IOException
    {
      ExprGenerator valueGen = ((ExprPro) value).getGenerator();

      if (false && _quercusClass.isDeclaredField(_name)) {
        // php/39f5
        if (! isTop)
          out.print("(");

        out.print("q_this._fields[_f_" + _name + "] = ");

        valueGen.generateRef(out);

        if (! isTop)
          out.print(")");
      }
      else if (_hasThis) {
        out.print("q_this.putThisField(env, ");
        out.print(_name);
        out.print(", ");
        valueGen.generateRef(out);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }

    /**
     * Generates code to evaluate the expression.
     *
     * @param out the writer to the Java source code.
     */
    public void generateUnset(PhpWriter out)
      throws IOException
    {
      if (_hasThis) {
        out.print("q_this.unsetThisField(");
        out.print(_name);
        out.print(")");
      }
      else {
        out.print("env.error(\"Cannot use '$this' when not in object context.\")");
      }
    }
  };
}

