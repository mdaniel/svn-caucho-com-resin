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

import java.util.HashSet;

import java.io.IOException;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.VarState;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.vfs.WriteStream;

/**
 * Represents a PHP statement
 */
abstract public class Statement {
  public static final int FALL_THROUGH = 0;
  public static final int BREAK_FALL_THROUGH = 0x1;
  public static final int RETURN = 0x3;

  private String _fileName;
  private int _line;

  /**
   * Sets the location of the statement in the source.
   */
  public Statement setLocation(String fileName, int line)
  {
    _fileName = fileName;
    _line = line;

    return this;
  }
  
  abstract public Value execute(Env env)
    throws Throwable;

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
    System.out.println("ANALYZE: " + getClass().getName());

    return true;
  }

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
  public boolean isVarAssigned(VarExpr var)
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
    if (_fileName != null)
      out.setLocation(_fileName, _line);
    
    generateImpl(out);
  }

  /**
   * Implementation of the generation.
   */
  abstract protected void generateImpl(PhpWriter out)
    throws IOException;

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateGetOut(PhpWriter out)
    throws IOException
  {
    // quercus/1l07
    // out.print("_quercus_out");

    out.print("env.getOut()");
  }

  /**
   * Generates static/initialization code code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    out.println("# unknown " + getClass().getName());
  }
  
  public String toString()
  {
    return "Statement[]";
  }
}

