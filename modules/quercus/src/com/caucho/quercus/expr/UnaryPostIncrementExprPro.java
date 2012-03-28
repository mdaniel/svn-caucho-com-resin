/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP post increment expression.
 */
public class UnaryPostIncrementExprPro extends UnaryPostIncrementExpr
  implements ExprPro
{
  public UnaryPostIncrementExprPro(Expr expr, int incr)
  {
    super(expr, incr);
  }

  @Override
  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  public ExprGenerator GENERATOR = new AbstractUnaryExprGenerator(getLocation()) {
      protected ExprGenerator getExpr()
      {
	return ((ExprPro) _expr).getGenerator();
      }
      
      /**
       * Return true for a double value
       */
      public boolean isDouble()
      {
	return getExpr().isDouble();
      }

      /**
       * Return true for a long value
       */
      public boolean isLong()
      {
        return getExpr().isLong();
      }

      /**
       * Return true for a number
       */
      public boolean isNumber()
      {
	return true;
      }

      //
      // Java code generation
      //

      /**
       * Analyze the expression
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
	// getExpr().analyzeSetPostIncrement();
	
	if (_expr instanceof VarExprPro) {
	  return getExpr().analyzeAssign(info, new IncrementGenerator());
	}
	else {
	  IncrementGenerator valueGen = new IncrementGenerator();
	  ExprType type = getExpr().analyzeAssign(info, valueGen);

	  getExpr().analyzeSetReference(info);
	  getExpr().analyzeSetModified(info);

	  return type;
	}
      }

      /**
       * Analyze the expression
       */
      @Override
      public void analyzeTop(AnalyzeInfo info)
      {
	if (_expr instanceof VarExprPro) {
	  getExpr().analyzeAssign(info, new IncrementGenerator());
	}
	else
	  analyze(info);
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateTop(PhpWriter out)
	throws IOException
      {
        if (_expr instanceof VarExprPro) {
          // common case of $i++

          getExpr().generateAssign(out, new IncrementExpr(), true);
        }
        else if (_expr instanceof ObjectFieldExprPro
                 || _expr instanceof ObjectFieldVarExprPro) {
          // php/39kp
          getExpr().generateAssign(out, new IncrementExpr(), true);
        }
        else if (_incr == 1) {
          getExpr().generateRef(out);
          out.print(".postincr()");
        }
        else if (_incr == -1) {
          getExpr().generateRef(out);
          out.print(".postdecr()");
        }
        else {
          getExpr().generateRef(out);
          out.print(".postincr(");
          out.print(_incr);
          out.print(")");
        }
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generate(PhpWriter out)
	throws IOException
      {
	if (_expr instanceof VarExprPro) {
	  VarExprPro varExpr = (VarExprPro) _expr;
	  
	  if (getExpr().isLong() && varExpr.getVarInfo().getSymbolName() == null) {
	    out.print("LongValue.create(");

	    generateLong(out);

	    out.print(")");
	  }
	  else if (varExpr.getVarInfo().isValue()) {
	    out.print("Env.first(");
	    getExpr().generate(out);
	    out.print(",");
	    getExpr().generateAssign(out, new IncrementExpr(), false);
	    out.print(")");
	  }
	  else {
	    getExpr().generateRef(out);
	    
	    if (_incr == 1) {
	      out.print(".postincr()");
	    }
	    else if (_incr == -1) {
	      out.print(".postdecr()");
	    }
	    else {
	      out.print(".postincr(");
	      out.print(_incr);
	      out.print(")");
	    }
	  }
	}
	else {
	  getExpr().generateRef(out);
	  out.print(".postincr(");
	  out.print(_incr);
	  out.print(")");
	}
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateLong(PhpWriter out)
        throws IOException
      {
        if (_expr instanceof VarExprPro && getExpr().isLong()) {
          VarExprPro varExpr = (VarExprPro) _expr;
          
          if (varExpr.getVarInfo().getSymbolName() != null)
            super.generateLong(out);
          else {
            out.print("(");

            getExpr().generateLong(out);

            if (_incr == 1)
              out.print("++");
            else
              out.print("--");
              
            out.print(")");
          }
        }
        else {
          super.generateLong(out);
        }
      }

      /**
       * Generates code to evaluate the expression as a long.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateDouble(PhpWriter out)
	throws IOException
      {
	if (getExpr().isLong())
	  generateLong(out);
	else
	  super.generateDouble(out);
      }

      /**
       * Generates code to evaluate the expression as a boolea.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateBoolean(PhpWriter out)
        throws IOException
      {
        // php/3ab8
        if (_expr instanceof VarExprPro
            && ((VarExprPro) _expr).getVarInfo().getSymbolName() != null) {
          super.generateBoolean(out);
        }
        else if (getExpr().isLong()) {
          out.print("(");

          generateLong(out);

          out.print(" != 0)");
        }
        else {
          super.generateBoolean(out);
        }
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateExpr(PhpWriter out)
	throws IOException
      {
	out.print("new com.caucho.quercus.expr.PostIncrementExpr(");
	getExpr().generateExpr(out);
	out.print(", ");
	out.print(_incr);
	out.print(")");
      }
    };

  /**
   * Internal expression for simple, top-level increment
   */
  class IncrementExpr extends Expr
    implements ExprPro
  {
    @Override
    public Value eval(Env env)
    {
      throw new UnsupportedOperationException();
    }
    
    @Override
    public ExprGenerator getGenerator()
    {
      return new IncrementGenerator();
    }
  }

  /**
   * Internal generator for simple, top-level increment
   */
  class IncrementGenerator extends AbstractUnaryExprGenerator {
    protected ExprGenerator getExpr()
    {
      return ((ExprPro) _expr).getGenerator();
    }
      
    /**
     * Return true for a double value
     */
    public boolean isDouble()
    {
      return getExpr().isDouble();
    }

    /**
     * Return true for a long value
     */
    public boolean isLong()
    {
      return getExpr().isLong();
    }

    /**
     * Return true for a number
     */
    public boolean isNumber()
    {
      return true;
    }

    //
    // Java code generation
    //

    @Override
    public ExprType analyze(AnalyzeInfo info)
    {
      ExprType type = getExpr().analyze(info);
      
      return type.withLong();
    }

    /**
     * Generates code to evaluate the expression as a long.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generate(PhpWriter out)
      throws IOException
    {
      getExpr().generate(out);
      
      // php/33hb
      if (_incr == -1) {
        out.print(".subOne()");
      }
      else {
        out.print(".increment(");
        out.print(_incr);
        out.print(")");
      }
      
      /*
      if (_incr == 1) {
        out.print(".addOne()");
      }
      else if (_incr == -1) {
        out.print(".subOne()");
      }
      else {
        out.print(".increment(");
        out.print(_incr);
        out.print(")");
      }
      */
    }

    /**
     * Generates code to evaluate the expression as a long.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateLong(PhpWriter out)
      throws IOException
    {
      out.print("(");
      getExpr().generateLong(out);

      out.print("+");

      out.print(_incr);
      out.print(")");
    }

    /**
     * Generates code to evaluate the expression as a long.
     *
     * @param out the writer to the Java source code.
     */
    @Override
    public void generateDouble(PhpWriter out)
      throws IOException
    {
      out.print("(");
      getExpr().generateDouble(out);

      out.print("+");

      out.print(_incr);
      out.print(")");
    }
  }
}

