/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.expr;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.MethodMap;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.Arg;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A "A::A(...)" call
 */
public class ClassConstructorExprPro extends ClassConstructorExpr
  implements ExprPro
{
  public ClassConstructorExprPro(Location location,
                                 String className,
                                 String methodName,
                                 ArrayList<Expr> args)
  {
    super(location, className, methodName, args);
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
        // _isMethod = info.getFunction().isMethod();;
        
        for (int i = 0; i < _args.length; i++) {
          ExprGenerator argGen = ((ExprPro) _args[i]).getGenerator();

          argGen.analyze(info);
          
          // php/39e1
          argGen.analyzeSetReference(info);
          argGen.analyzeSetModified(info);
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
        out.print("env.getClass(\"");
        out.printJavaString(_className);
        out.print("\").callMethod(env, q_this, ");
            
        String nameConst = out.addStringValue(_name);

        out.print(nameConst + ", " + _name.hashCode());
        
        // out.print(").callStaticMethod(env, ");

        /*
        if (_isMethod)
          out.print("q_this");
        else
          out.print("NullValue.NULL");
          */
        
        // out.print("q_this");
          
        if (_args.length <= 5) {
          for (int i = 0; i < _args.length; i++) {
            ExprPro arg = (ExprPro) _args[i];
            
            out.print(", ");
          
            arg.getGenerator().generate(out);
          }
        }
        else {
          out.print(", new Value[] {");

          for (int i = 0; i < _args.length; i++) {
            ExprPro arg = (ExprPro) _args[i];
            
            if (i != 0)
              out.print(", ");
          
            arg.getGenerator().generate(out);
          }
        
          out.print("}");
        }

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
	out.print("new ClassMethodExpr(");
	out.print("\"");
	out.printJavaString(_name.toString());
	out.print("\", new Expr[] {");

	for (int i = 0; i < _args.length; i++) {
	  ExprPro arg = (ExprPro) _args[i];
	  
	  if (i != 0)
	    out.print(", ");
      
	  arg.getGenerator().generateExpr(out);
	}
	out.print("})");
      }
  
      public String toString()
      {
	return _className + "::" + _name + "()";
      }
    };
}

