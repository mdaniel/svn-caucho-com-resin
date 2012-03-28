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
import com.caucho.quercus.env.StringValue;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a PHP static method expression.
 */
public class ClassVirtualMethodExprPro
  extends ClassVirtualMethodExpr
  implements ExprPro
{
  public ClassVirtualMethodExprPro(Location location,
                                   String methodName,
                                   ArrayList<Expr> args)
  {
    super(location, methodName, args);
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
	_isMethod = info.getFunction().isMethod();
    
	for (int i = 0; i < _args.length; i++) {
	  ExprPro arg = (ExprPro) _args[i];
	  
	  ExprGenerator argGen = arg.getGenerator();
	  argGen.analyze(info);

	  // php/344y
	  argGen.analyzeSetReference(info);
	  argGen.analyzeSetModified(info);
	}

	return ExprType.VALUE;
      }

      private boolean isMethod()
      {
	return _isMethod;
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out)
	throws IOException
      {
	generate(out, false);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generateRef(PhpWriter out)
	throws IOException
      {
	generate(out, true);
      }

      /**
       * Generates code to recreate the expression.
       *
       * @param out the writer to the Java source code.
       */
      private void generate(PhpWriter out, boolean isRef)
	throws IOException
      {
	Expr []args = _args;
	
	// out.print("env.getCallingClass(q_this)");
	out.print("q_this");
	if (isRef)
	  out.print(".callMethodRef(env");
	else
	  out.print(".callMethod(env");

	/*
	// XXX: needed for mediawiki
	if (isMethod())
	  out.print("q_this");
	else
	  out.print("NullThisValue.NULL");
	  */
        
        String nameConst = out.addStringValue(_methodName);
        
        int hash = _methodName.hashCodeCaseInsensitive();
        
        out.print(", " + nameConst + ", " + hash);

	if (args.length <= 5) {
	  // XXX: check variable args
      
	  for (int i = 0; i < args.length; i++) {
	    ExprPro arg = (ExprPro) args[i];
	      
	    out.print(", ");
      
	    arg.getGenerator().generateArg(out, true);
	  }

	  out.print(")");
	}
	else {
	  out.print(", new Value[] {");

	  for (int i = 0; i < args.length; i++) {
	    ExprPro arg = (ExprPro) args[i];
	      
	    if (i != 0)
	      out.print(", ");
      
	    arg.getGenerator().generateArg(out, true);
	  }
      
	  out.print("})");
	}
      }

      public void generateCopy(PhpWriter out)
	throws IOException
      {
	generate(out);
	out.print(".copyReturn()"); // php/3a5y
      }
    };
}

