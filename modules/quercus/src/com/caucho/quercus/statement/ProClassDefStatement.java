/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.statement.ClassDefStatement;
import com.caucho.quercus.program.ProClassDef;

import java.io.IOException;

/**
 * Represents a class definition
 */
public class ProClassDefStatement extends ClassDefStatement
  implements CompilingStatement
{
  private String _name;
  
  public ProClassDefStatement(Location location,
                              InterpretedClassDef cl)
  {
    super(location, cl);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProClassDefStatement.this.getLocation();
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
        // php/39p1
        // needed for wordpress subscribe2 plugin
        ((ProClassDef) _cl).generateInit(out, true);
      }
    };
}

