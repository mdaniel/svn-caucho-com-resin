/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.statement.BlockStatement;
import com.caucho.quercus.statement.Statement;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents sequence of statements.
 */
public class ProBlockStatement extends BlockStatement
  implements CompilingStatement
{
  public ProBlockStatement(Location location,
			   ArrayList<Statement> statementList)
  {
    super(location, statementList);
  }
  
  public ProBlockStatement(Location location,
			   Statement []statements)
  {
    super(location, statements);
  }

  public BlockStatement append(ArrayList<Statement> statementList)
  {
    Statement []statements
      = new Statement[_statements.length + statementList.size()];

    System.arraycopy(_statements, 0, statements, 0, _statements.length);

    for (int i = 0; i < statementList.size(); i++)
      statements[i + _statements.length] = statementList.get(i);

    return new ProBlockStatement(getLocation(), statements);
  }

  public StatementGenerator getGenerator()
  {
    return GENERATOR;
  }

  private StatementGenerator GENERATOR = new StatementGenerator() {
      protected Location getLocation()
      {
	return ProBlockStatement.this.getLocation();
      }

      /**
       * Analyze the statement
       */
      public boolean analyze(AnalyzeInfo info)
      {
	for (int i = 0; i < getStatements().length; i++) {
	  CompilingStatement stmt = (CompilingStatement) getStatements()[i];
	  
	  if (! stmt.getGenerator().analyze(info))
	    return false;
	}

	return true;
      }

      /**
       * Returns true if the statement can fallthrough.
       */
      @Override
      public int fallThrough()
      {
	for (int i = 0; i < getStatements().length; i++) {
	  CompilingStatement stmt = (CompilingStatement) getStatements()[i];
	  
	  int fallThrough = stmt.getGenerator().fallThrough();

    //XXX: how about BREAK_FALL_THROUGH?
	  if (fallThrough != FALL_THROUGH)
	    return fallThrough;
	}

	return FALL_THROUGH;
      }

      /**
       * Generates the Java code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      @Override
      protected void generateImpl(PhpWriter out)
	throws IOException
      {
	for (int i = 0; i < getStatements().length; i++) {
	  CompilingStatement stmt = (CompilingStatement) getStatements()[i];
	  
	  stmt.getGenerator().generate(out);

	  if (! (stmt.getGenerator().fallThrough() == FALL_THROUGH
		 || stmt.getGenerator().fallThrough() == BREAK_FALL_THROUGH)) {
	    return;
	  }
	}
      }

      /**
       * Generates the Java footer code for the statement.
       *
       * @param out the writer to the generated Java source.
       */
      public void generateCoda(PhpWriter out)
	throws IOException
      {
	for (int i = 0; i < getStatements().length; i++) {
	  CompilingStatement stmt = (CompilingStatement) getStatements()[i];
	  
	  stmt.getGenerator().generateCoda(out);
	}
      }
    };
}

