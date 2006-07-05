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

import java.util.ArrayList;
import java.util.HashSet;

import java.io.IOException;

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.QuercusClass;

import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.vfs.WriteStream;

/**
 * Represents a class definition
 */
public class ClassDefStatement extends Statement {
  private InterpretedClassDef _cl;

  public ClassDefStatement(Location location, InterpretedClassDef cl)
  {
    super(location);

    _cl = cl;
  }
  
  public Value execute(Env env)
  {
    if (env.findClass(_cl.getName()) == null) {
      QuercusClass qClass = new QuercusClass(_cl, null);

      qClass.validate(env);
      env.addClass(_cl.getName(), qClass);
    }

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
    out.print("if (env.findClass(\"");
    out.printJavaString(_cl.getName());
    out.println("\") == null)");
    out.print("  env.addClass(\"");
    out.printJavaString(_cl.getName());
    out.print("\", new QuercusClass(env.findClassDef(\"");
    out.printJavaString(_cl.getName());
    out.print("\", ");

    if (_cl.getParentName() != null) {
      out.print("env.getClass(\"");
      out.printJavaString(_cl.getParentName());
      out.print("\")");
    }
    else
      out.print("null");
    
    out.println("));");
    // XXX:
  }
  
}

