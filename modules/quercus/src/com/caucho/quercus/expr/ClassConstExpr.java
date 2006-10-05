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

package com.caucho.quercus.expr;

import java.io.IOException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.program.AnalyzeInfo;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;

import com.caucho.util.L10N;

/**
 * Represents a PHP parent::FOO constant call expression.
 */
public class ClassConstExpr extends Expr {
  private static final L10N L = new L10N(ClassMethodExpr.class);

  private final String _className;
  private final String _name;

  public ClassConstExpr(Location location, String className, String name)
  {
    super(location);
    
    _className = className.intern();
    _name = name.intern();
  }

  public ClassConstExpr(String className, String name)
  {
    _className = className.intern();
    _name = name.intern();
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    return eval(env, env.getClass(_className));
  }

  public Value eval(Env env, QuercusClass ownerClass)
  {
    return ownerClass.getConstant(env, _name);
  }

  //
  // java code generation
  //
  
  /**
   * Analyzes the function.
   */
  public void analyze(AnalyzeInfo info)
  {
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PhpWriter out)
    throws IOException
  {
    out.print("env.getClass(\"");
    out.printJavaString(_className);
    out.print("\").getConstant(env, \"");
    out.printJavaString(_name);
    out.print("\")");
  }

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generateExpr(PhpWriter out)
    throws IOException
  {
    out.print("new ClassConstExpr(");
    out.print("\"");
    out.printJavaString(_className);
    out.print("\", \"");
    out.printJavaString(_name);
    out.print("\")");
  }
  
  public String toString()
  {
    return _className + "::" + _name + "()";
  }
}

