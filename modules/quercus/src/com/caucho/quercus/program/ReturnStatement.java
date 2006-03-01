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
import com.caucho.quercus.env.NullValue;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.vfs.WriteStream;

/**
 * Represents a return expression statement in a PHP program.
 */
public class ReturnStatement extends Statement {
  private Expr _expr;
  
  /**
   * Creates the echo statement.
   */
  public ReturnStatement(Expr expr)
  {
    _expr = expr;
  }

  /**
   * Executes the statement, returning the expression value.
   */
  public Value execute(Env env)
    throws Throwable
  {
    if (_expr != null) {
      return _expr.evalRef(env);
    }
    else
      return NullValue.NULL;
  }

  //
  // java code generation
  //
  
  /**
   * Analyze the statement
   */
  public boolean analyze(AnalyzeInfo info)
  {
    _expr.analyze(info);

    _expr.analyzeSetReference(info); // php/3783, php/3a5d

    return false;
  }

  /**
   * Returns true if control can go past the statement.
   */
  public int fallThrough()
  {
    return RETURN;
  }

  /**
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  protected void generateImpl(PhpWriter out)
    throws IOException
  {
    // the "if" test handles Java's issue with trailing statements
    
    if (_expr != null) {
      out.print("return ");

      // php/3a5h
      _expr.generate(out);
      
      /**
      Expr copy = _expr.createCopy();

      if (_expr instanceof VarExpr)
	copy.generateReturn(out);
      else
	copy.generate(out);
      */
      
      out.println(";");
    }
    else
      out.print("return NullValue.NULL;");
  }
  
  public String toString()
  {
    return "ReturnStatement[]";
  }
}

