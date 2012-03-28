/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;

/**
 * Represents a PHP array reference expression.
 */
public class ArrayGetGetExprPro extends ArrayGetExpr
  implements ExprPro
{
  public ArrayGetGetExprPro(Location location, Expr expr, Expr index)
  {
    super(location, expr, index);
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      public ExprGenerator getExpr()
      {
	return ((ExprPro) _expr).getGenerator();
      }
      
      public ExprGenerator getIndex()
      {
	return ((ExprPro) _index).getGenerator();
      }
	
      /**
       * Analyze the statement
       */
      @Override
      public ExprType analyze(AnalyzeInfo info)
      {
	getExpr().analyze(info);
	getIndex().analyze(info);

	return ExprType.VALUE;
      }

      /**
       * Analyze the statement
       */
      @Override
      public ExprType analyzeAssign(AnalyzeInfo info, ExprGenerator value)
      {
	analyze(info);

 	getExpr().analyzeSetModified(info);
	value.analyze(info);
	
	// php/3a68 XXX: no, that's a modified
	// getExpr().analyzeSetReference(info);
	
	return ExprType.VALUE;
      }

      /**
       * Analyze the statement as modified
       */
      @Override
      public void analyzeSetModified(AnalyzeInfo info)
      {
	getExpr().analyzeSetModified(info);
      }

      /**
       * Analyze the statement as a reference
       */
      @Override
      public void analyzeSetReference(AnalyzeInfo info)
      {
	// php/3a69
	getExpr().analyzeSetReference(info);
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
	getExpr().generate(out);
	out.print(".get(");
	getIndex().generate(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression, marking as dirty, i.e.
       * copy-on-write for an unset.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateDirty(PhpWriter out)
	throws IOException
      {
	getExpr().generate(out);
	out.print(".getDirty(");
	getIndex().generate(out);
	out.print(")");
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      /*
      @Override
      public void generateArray(PhpWriter out)
	throws IOException
      {
	getExpr().generateArray(out);
	out.print(".getArray(");
	getIndex().generate(out);
	out.print(")");
      }
      */

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      /*
      @Override
      public void generateObject(PhpWriter out)
	throws IOException
      {
	getExpr().generateArray(out);
	out.print(".getObject(env, ");
	getIndex().generate(out);
	out.print(")");
      }
      */

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateRef(PhpWriter out)
	throws IOException
      {
	getExpr().generateRef(out); // php/3d1c
	out.print(".getRef(");
	getIndex().generate(out);
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
	getExpr().generateArg(out, false);
	out.print(".getArg(");
	getIndex().generate(out);
	out.print(", " + isTop + ")");
      }

      /**
       * Generates code to evaluate the expression with a copied result.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateCopy(PhpWriter out)
	throws IOException
      {
	generate(out);
	out.print(".copy()"); // php/3a5m
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssign(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();

        getExpr().generateAssignOpen(out);
        
        getExpr().generateRef(out);
        out.print(", ");
        getIndex().generate(out);
        out.print(", ");
        valueGen.generateCopy(out);  // php/3a5k
        
        getExpr().generateAssignClose(out);
      }

      /**
       * Generates code to evaluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      @Override
      public void generateAssignRef(PhpWriter out, Expr value, boolean isTop)
        throws IOException
      {
        ExprGenerator valueGen = ((ExprPro) value).getGenerator();
        
        getExpr().generateAssignOpen(out);
        
        getExpr().generateRef(out);
        out.print(", ");
        getIndex().generate(out);
        out.print(", ");
        valueGen.generateRef(out);
        
        getExpr().generateAssignClose(out);
      }
      
      @Override
      public void generateAssignOpen(PhpWriter out)
        throws IOException
      {
        getExpr().generateRef(out);
        out.print(".put(");
        getIndex().generate(out);
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
      @Override
      public void generateUnset(PhpWriter out)
        throws IOException
      {
        /*
        getExpr().generateDirty(out);
        out.print(".remove(");
        getIndex().generate(out);
        out.print(")");
        */
        
        getExpr().generateUnsetArray(out, getIndex());
      }
    };
  
}

