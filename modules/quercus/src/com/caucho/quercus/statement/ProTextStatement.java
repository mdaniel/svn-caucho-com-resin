/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.TextStatement;

import java.io.IOException;

/**
 * Represents static text in a PHP program.
 */
public class ProTextStatement extends TextStatement
  implements CompilingStatement
{
  /**
   * Creates the text statement with its string.
   */
  public ProTextStatement(Location location, String value)
  {
    super(location, value);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProTextStatement.this.getLocation();
      }

      /**
       * Analyze the statement
       *
       * @return true if the following statement can be executed
       */
      public boolean analyze(AnalyzeInfo info)
      {
	info.getFunction().setOutUsed();
    
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
	out.print("env.print(\"");
	out.printJavaString(getValue());
	out.println("\");");
      }
    };
}

