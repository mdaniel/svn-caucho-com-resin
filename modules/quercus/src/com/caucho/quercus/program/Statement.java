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

import com.caucho.java.JavaWriter;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.expr.VarExpr;

import com.caucho.quercus.gen.PhpWriter;
import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusExecutionException;

/**
 * Represents a PHP statement
 */
abstract public class Statement {
  public static final int FALL_THROUGH = 0;
  public static final int BREAK_FALL_THROUGH = 0x1;
  public static final int RETURN = 0x3;

  private final Location _location;

  protected Statement(Location location)
  {
    _location = location;
  }

  public Location getLocation()
  {
    return _location;
  }

  abstract public Value execute(Env env)
    throws Throwable;

  final protected void rethrow(Throwable t)
    throws Throwable
  {
    // add stack information to the rootCause
    Throwable rootCause = t;

    while (rootCause.getCause() != null)
      rootCause = t.getCause();

    if (!(rootCause instanceof QuercusExecutionException)) {
      QuercusExecutionException ex = new QuercusExecutionException(t.getMessage());
      ex.setStackTrace(new StackTraceElement[] {});
      rootCause.initCause(ex);
      rootCause = ex;
    }

    StackTraceElement[] existingElements = rootCause.getStackTrace();
    int len = existingElements.length;

    String className = _location.getClassName();
    String functionName = _location.getFunctionName();
    String fileName = _location.getFileName();
    int lineNumber = _location.getLineNumber();

    if (className == null)
      className = "";

    if (functionName == null)
      functionName = "";

    StackTraceElement lastElement;

    if (len > 1)
      lastElement = existingElements[len - 1];
    else
      lastElement = null;

    // early return if function and class are same as last one
    if (lastElement != null
        && (functionName.equals(lastElement.getMethodName()))
        && (className.equals(lastElement.getClassName())))
    {
      throw t;
    }

    StackTraceElement[] elements = new StackTraceElement[len + 1];

    System.arraycopy(existingElements, 0, elements, 0, len);

    elements[len] = new StackTraceElement(className,
                                          functionName,
                                          fileName,
                                          lineNumber);

    rootCause.setStackTrace(elements);

    throw t;
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
    if (_location != null)
      out.setLocation(_location.getFileName(), _location.getLineNumber());

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

