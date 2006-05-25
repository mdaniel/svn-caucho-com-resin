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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.IdentityHashMap;

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
  private static final Logger log = Logger.getLogger(Statement.class.getName());

  public static final int FALL_THROUGH = 0;
  public static final int BREAK_FALL_THROUGH = 0x1;
  public static final int RETURN = 0x2;

  private final Location _location;

  protected Statement()
  {
    _location = Location.UNKNOWN;
  }

  protected Statement(Location location)
  {
    _location = location;
  }

  public Location getLocation()
  {
    return _location;
  }

  abstract public Value execute(Env env);

  final protected void rethrow(Throwable t)
    throws Throwable
  {
    rethrow(t, Throwable.class);
  }

  final protected <E extends Throwable> void rethrow(Throwable t, Class<E> cl)
    throws E
  {
    E typedT;

    if (!cl.isAssignableFrom(t.getClass())) {
      try {
        typedT = cl.newInstance();
        typedT.initCause(t);
      }
      catch (InstantiationException e) {
        log.log(Level.WARNING, t.toString(), t);
        throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
        log.log(Level.WARNING, t.toString(), t);
        throw new RuntimeException(e);
      }
    }
    else
      typedT = (E) t;

    Throwable rootCause = t;

    // guard against circular cause
    IdentityHashMap<Throwable, Boolean> causes = new IdentityHashMap<Throwable, Boolean>();

    causes.put(rootCause, Boolean.TRUE);

    while (rootCause.getCause() != null) {
      Throwable cause = rootCause.getCause();

      if (causes.containsKey(cause))
        break;

      causes.put(cause, Boolean.TRUE);

      rootCause = cause;
    }

    if (!(rootCause instanceof QuercusExecutionException)) {
      String rootCauseName = rootCause.getClass().getName();
      String rootCauseMessage = rootCause.getMessage();

      StringBuilder quercusExMessage = new StringBuilder();

      quercusExMessage.append(rootCauseName);

      if (rootCauseMessage != null && rootCauseMessage.length() > 0) {
        quercusExMessage.append(" ");
        quercusExMessage.append(rootCauseMessage);
      }

      QuercusExecutionException quercusEx
        = new QuercusExecutionException(quercusExMessage.toString());

      StackTraceElement[] quercusExStackTrace = quercusEx.getStackTrace();
      StackTraceElement[] rootCauseStackTrace = rootCause.getStackTrace();

      int quercusExIndex = quercusExStackTrace.length - 1;
      int rootCauseIndex = rootCauseStackTrace.length - 1;

      while (rootCauseIndex >= 0 &&  quercusExIndex >= 0) {
        StackTraceElement rootCauseElement = rootCauseStackTrace[rootCauseIndex];
        StackTraceElement quercusExElement = quercusExStackTrace[quercusExIndex];

        if (! quercusExElement.equals(rootCauseElement))
          break;

        rootCauseIndex--;
        quercusExIndex--;
      }

      int len = rootCauseIndex + 1;

      StackTraceElement[] trimmedElements = new StackTraceElement[len];
      System.arraycopy(rootCauseStackTrace, 0, trimmedElements, 0, len);

      quercusEx.setStackTrace(trimmedElements);
      rootCause.initCause(quercusEx);
      rootCause = quercusEx;
    }

    String className = _location.getClassName();
    String functionName = _location.getFunctionName();
    String fileName = _location.getFileName();
    int lineNumber = _location.getLineNumber();

    if (className == null)
      className = "";

    if (functionName == null)
      functionName = "";

    StackTraceElement[] existingElements = rootCause.getStackTrace();
    int len = existingElements.length;
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
      throw typedT;
    }

    StackTraceElement[] elements = new StackTraceElement[len + 1];

    System.arraycopy(existingElements, 0, elements, 0, len);

    elements[len] = new StackTraceElement(className,
                                          functionName,
                                          fileName,
                                          lineNumber);

    rootCause.setStackTrace(elements);

    throw typedT;
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
    if (_location != null) {
      out.setLocation(_location.getFileName(), _location.getLineNumber());

      if (log.isLoggable(Level.FINE)) {
        out.print("// ");
        out.print(_location.getFileName());
        out.print(":");
        out.print(_location.getLineNumber());

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
   * Generates the Java code for the statement.
   *
   * @param out the writer to the generated Java source.
   */
  public void generateGetOut(PhpWriter out)
    throws IOException
  {
    // quercus/1l07
    // out.print("_quercus_out");

    //out.print("env.getOut()");
    out.print("env");
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
    return getClass().getSimpleName() + "[]";
  }
}

