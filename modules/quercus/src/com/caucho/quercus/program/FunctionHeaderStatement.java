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
import java.util.ArrayList;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.VarExpr;
import com.caucho.quercus.expr.VarState;

import com.caucho.quercus.gen.PhpWriter;

import com.caucho.vfs.WriteStream;

/**
 * A dummy statement representing the function arguments
 */
public class FunctionHeaderStatement extends Statement {
  private ArrayList<Arg> _args;

  public FunctionHeaderStatement(ArrayList<Arg> args)
  {
    _args = args;
  }
  
  public Value execute(Env env)
    throws Throwable
  {
    return null;
  }

  //
  // java generation code
  //

  /**
   * Returns the variables used in the statement.
   */
  public void getVariables(HashSet<VarExpr> vars)
  {
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

  /**
   * Generates static/initialization code code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateCoda(PhpWriter out)
    throws IOException
  {
  }
  
  public String toString()
  {
    return "FunctionHeaderStatement[]";
  }
}

