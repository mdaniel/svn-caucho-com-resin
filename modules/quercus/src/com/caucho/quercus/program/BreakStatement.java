/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.program;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BreakValue;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.vfs.WriteStream;

/**
 * Represents a break expression statement in a PHP program.
 */
public class BreakStatement extends Statement {
  public static final BreakStatement BREAK = new BreakStatement();
  /**
   * Creates the break statement.
   */
  private BreakStatement()
  {
    super(Location.UNKNOWN);
  }

  /**
   * Executes the statement, returning the expression value.
   */
  public Value execute(Env env)
  {
    return BreakValue.BREAK;
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   *
   * @return true if the following statement can be executed
   */
  public boolean analyze(AnalyzeInfo info)
  {
    info.mergeLoopBreakInfo();

    // quercus/067i
    return true;
  }

  /**
   * Returns true if the statement can fallthrough.
   */
  public int fallThrough()
  {
    return BREAK_FALL_THROUGH;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    String breakVar = out.getBreakVar();

    if (breakVar != null)
      out.println("if (true) break " + breakVar + ";");
    else
      out.println("if (true) break;");
  }
  
}

