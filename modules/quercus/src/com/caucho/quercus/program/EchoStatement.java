/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.HashSet;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.vfs.WriteStream;

/**
 * Represents an echo statement in a PHP program.
 */
public class EchoStatement extends Statement {
  private Expr _expr;

  private String _genId;
  
  /**
   * Creates the echo statement.
   */
  public EchoStatement(Expr expr)
  {
    _expr = expr;
  }
  
  public Value execute(Env env)
    throws Throwable
  {
    Value value = _expr.eval(env);

    value.print(env);

    return null;
  }

  //
  // java generation code
  //

  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    info.getFunction().setOutUsed();
      
    _expr.analyze(info);

    return true;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    _expr.generatePrint(out);
    out.println(";");
  }

  /**
   * Generates static/initialization code code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
    /*
    out.print("static com.caucho.quercus.expr.Expr " + _genId + " = ");
    _expr.generateExpr(out);
    out.println(";");
    */
  }

  /**
   * Disassembly.
   */
  public void debug(JavaWriter out)
    throws IOException
  {
    out.println("echo " + _expr + ";");
  }

  public String toString()
  {
    return "EchoStatement[]";
  }
}

