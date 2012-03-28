/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.expr.ExprGenerator;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.CompilingStatement;
import com.caucho.quercus.statement.StatementGenerator;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents sequence of statements.
 */
public class ProMethodDeclaration extends MethodDeclaration
  implements CompilingFunction
{
  private static final Logger log
    = Logger.getLogger(ProMethodDeclaration.class.getName());
  private static final L10N L = new L10N(ProMethodDeclaration.class);
  
  public ProMethodDeclaration(ExprFactory exprFactory,
                              Location location,
                              ClassDef qClass,
                              String name,
                              FunctionInfo info,
                              Arg []argList)
  {
    super(exprFactory, location, qClass, name, info, argList);
  }

  public FunctionGenerator getGenerator()
  {
    return GENERATOR;
  }

  private FunctionGenerator GENERATOR = new FunctionGenerator() {
      private StatementGenerator getStatement()
      {
	return ((CompilingStatement) _statement).getGenerator();
      }
      
      /**
       * Analyzes the function.
       */
      public void analyze()
      {
      }

    public void generate(PhpWriter out)
      throws IOException
    {
      generateImpl(out);
    }

    /**
       * Generates code to calluate the expression.
       *
       * @param out the writer to the Java source code.
       */
      public void generate(PhpWriter out,
			   ExprGenerator funExpr,
			   Expr []args)
	throws IOException
      {
        // XXX: this method is probably unneeded
        
        generateImpl(out);
      }

      private void generateImpl(PhpWriter out)
        throws IOException
      {
        // php/39j2
        out.println();
        out.println("public static final LazyMethod fun_" + getCompilationName());
        out.println(" = new LazyMethod(" + out.getCurrentClassName() + ".class"
		    + ", \"" + ProMethodDeclaration.this.getName()
		    + "\", \"fun_" + getCompilationName() + "\");");

        out.println();
        out.println("public final static class fun_" + getCompilationName() + " extends CompiledMethod {");
        out.pushDepth();
        
        out.println("public boolean isAbstract()");
        out.println("{");
        out.pushDepth();
        out.println("return true;");
        out.popDepth();
        out.println("}");
        
        out.println();

        out.println("public String getName()");
        out.println("{");
        out.pushDepth();
        out.print("return \"");
        out.printJavaString(_name);
        out.println("\";");
        out.popDepth();
        out.println("}");

        out.println();
        
        if (getComment() != null) {
          out.println("@Override");
          out.println("public String getComment()");
          out.println("{");
          out.pushDepth();
          out.print("return \"");
          out.printJavaString(getComment());
          out.println("\";");
          out.popDepth();
          out.println("}");
          
          out.println();
        }

        out.println("public Value callMethod(Env env, Value obj)");
        out.println("{");
        out.pushDepth();
        out.println("return NullValue.NULL;");
        out.popDepth();
        out.println("}");

        out.popDepth();
        out.println("}");
      }
    };
  
}

