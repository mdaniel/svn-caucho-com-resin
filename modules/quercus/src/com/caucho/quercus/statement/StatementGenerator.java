/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.statement;

import com.caucho.quercus.Location;
import com.caucho.quercus.expr.VarExprPro;
import com.caucho.quercus.gen.AnalyzeInfo;
import com.caucho.quercus.gen.PhpWriter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a PHP statement
 */
abstract public class StatementGenerator
{
  private static final Logger log
    = Logger.getLogger(StatementGenerator.class.getName());

  public static final int FALL_THROUGH = 0;
  public static final int BREAK_FALL_THROUGH = 0x1;
  public static final int RETURN = 0x2;

  abstract protected Location getLocation();
  /**
   * Analyze the statement
   *
   * @return true if the following statement can be executed
   */
  abstract public boolean analyze(AnalyzeInfo info);

  /**
   * Returns true if the statement can fallthrough.
   */
  public int fallThrough()
  {
    return FALL_THROUGH;
  }

  /**
   * Returns true if the variable is ever assigned.
   *
   * @param var the variable to test
   */
  public boolean isVarAssigned(VarExprPro var)
  {
    return false;
  }

  /**
   * Returns true if the output is used in the statement.
   */
  public boolean isOutUsed()
  {
    return false;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public final void generate(PhpWriter out)
    throws IOException
  {
    Location location = getLocation();

    if (location != null) {
      out.setLocation(location.getFileName(), location.getLineNumber());

      if (log.isLoggable(Level.FINE)) {
        out.print("// ");

        // windows
        String fileName = location.getFileName();

        if (fileName != null)
          fileName = fileName.replace('\\', '/');

        out.print(fileName);
        out.print(":");
        out.print(location.getLineNumber());

        if (log.isLoggable(Level.FINER)) {
          out.print(" ");
          out.print(toString());
        }

        out.println();
      }
    }

    generateImpl(out);
  }

  /**
   * Implementation of the generation.
   */
  abstract protected void generateImpl(PhpWriter out)
    throws IOException;

  /**
   * Generates static/initialization code code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
  }
}

