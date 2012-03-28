/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.Function;
import com.caucho.quercus.statement.FunctionDefStatement;

import java.io.IOException;
import java.util.Locale;

/**
 * Represents a function definition
 */
public class ProFunctionDefStatement extends FunctionDefStatement
  implements CompilingStatement
{
  public ProFunctionDefStatement(Location location, Function fun)
  {
    super(location, fun);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
        return ProFunctionDefStatement.this.getLocation();
      }
      
      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
        return true;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      protected void generateImpl(PhpWriter out)
        throws IOException
      {
        out.print("if (env.findFunction(\"");
        out.printJavaString(_fun.getName().toLowerCase(Locale.ENGLISH));
        out.println("\") == null)");
        out.print("  env.addFunction(\"");
        out.printJavaString(_fun.getName().toLowerCase(Locale.ENGLISH));
        out.println("\", fun_" + _fun.getCompilationName() + ".toFun());");
        
        out.println("else");
        out.println("  env.error(\"function " + _fun.getName() + "() is already defined.\");");
      }
    };
  
}

