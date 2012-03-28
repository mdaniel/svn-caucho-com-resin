/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import com.caucho.quercus.Location;

import com.caucho.quercus.statement.NullStatement;

import java.io.IOException;

/**
 * Represents a compiled PHP program.
 */
public class ProNullStatement extends NullStatement
  implements CompilingStatement
{
  public static final ProNullStatement PRO_NULL = new ProNullStatement();

  protected ProNullStatement()
  {
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private final StatementGenerator GENERATOR = new StatementGenerator() {
      public Location getLocation()
      {
	return ProNullStatement.this.getLocation();
      }
      
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
      }
    };
}

