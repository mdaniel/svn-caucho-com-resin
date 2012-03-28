/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP function expression.
 */
public class CallVarExprPro extends CallVarExpr
  implements ExprPro
{
  public CallVarExprPro(Location location, Expr name, ArrayList<Expr> args)
  {
    super(location, name, args);
  }

  public CallVarExprPro(Location location, Expr name, Expr []args)
  {
    super(location, name, args);
  }

  public CallVarExprPro(Expr name, ArrayList<Expr> args)
  {
    super(name, args);
  }

  public CallVarExprPro(Expr name, Expr []args)
  {
    super(name, args);
  }

  /**
   * Returns the reference of the value.
   * @param location
   */
  public Expr createRef()
  {
    return new UnaryRefExprPro(this);
  }

  /**
   * Returns the copy of the value.
   * @param location
   */
  public Expr createCopy(Location location)
  {
    return this;
  }

  public ExprGenerator getGenerator()
  {
    return GENERATOR;
  }

  private ExprGenerator GENERATOR = new ExprGenerator(getLocation()) {
      /**
       * Analyze the statement
       */
      public ExprType analyze(AnalyzeInfo info)
      {
	ExprGenerator nameGen = ((ExprPro) _name).getGenerator();
	nameGen.analyze(info);

	for (int i = 0; i < _args.length; i++) {
	  ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
	  
	  argGen.analyze(info);
	  argGen.analyzeSetModified(info);
	  argGen.analyzeSetReference(info);
	}

	return ExprType.VALUE;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	generateImpl(out, false);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateRef(PhpWriter out)
	throws IOException
      {
	generateImpl(out, true);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateCopy(PhpWriter out)
	throws IOException
      {
	generateImpl(out, true);
	out.print(".copyReturn()");
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      private void generateImpl(PhpWriter out, boolean isRef)
      throws IOException
      {
        ExprGenerator nameGen = ((ExprPro) _name).getGenerator();
        
        nameGen.generate(out);

        if (isRef)
          out.print(".callRef(env");
        else
          out.print(".call(env");
    
        if (_args.length <= 5) {
          for (int i = 0; i < _args.length; i++) {
            ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
            
            out.print(", ");

            argGen.generateArg(out, true);
          }

          out.print(")");
        }
        else {
          out.print(", new Value[] {");

          for (int i = 0; i < _args.length; i++) {
            ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();
            
            if (i != 0)
              out.print(", ");

            argGen.generateArg(out, true);
          }
          out.print("})");
        }
      }
    };
}

