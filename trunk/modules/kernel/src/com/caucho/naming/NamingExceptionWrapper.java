/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.naming;

import com.caucho.util.ExceptionWrapper;

import javax.naming.NamingException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Wraps the actual exception with a Naming exception
 */
public class NamingExceptionWrapper extends NamingException
  implements ExceptionWrapper {
  private Throwable _rootCause;

  /**
   * Null constructor for beans
   */
  public NamingExceptionWrapper()
  {
  }
  /**
   * Create a basic NamingExceptionWrapper with a message.
   *
   * @param msg the exception message.
   */
  public NamingExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Create a NamingExceptionWrapper wrapping a root exception.
   *
   * @param rootCause the underlying wrapped exception.
   */
  public NamingExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.getMessage());

    _rootCause = rootCause;
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  public Throwable getRootCause()
  {
    return _rootCause;
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  @Override
  public Throwable getCause()
  {
    return _rootCause;
  }

  /**
   * Returns the appropriate exception message.
   */
  public String getMessage()
  {
    if (_rootCause != null)
      return _rootCause.getMessage();
    else
      return super.getMessage();
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace()
  {
    if (_rootCause != null)
      _rootCause.printStackTrace();
    else
      super.printStackTrace();
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintStream os)
  {
    if (_rootCause != null)
      _rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintWriter os)
  {
    if (_rootCause != null)
      _rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }

  /**
   * Print the exception as a string.
   */
  public String toString()
  {
    if (_rootCause == null)
      return super.toString();
    else
      return getClass().getName() + ": " + _rootCause;
  }
}

