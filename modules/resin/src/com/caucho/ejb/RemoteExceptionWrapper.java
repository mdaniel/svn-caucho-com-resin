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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 *
 * $Id: RemoteExceptionWrapper.java,v 1.2 2004/09/29 00:13:01 cvs Exp $
 */

package com.caucho.ejb;

import com.caucho.util.ExceptionWrapper;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Wraps the actual exception with an Remote exception
 */
public class RemoteExceptionWrapper extends java.rmi.RemoteException
  implements ExceptionWrapper {
  private Throwable rootCause;

  /**
   * Null constructor for beans
   */
  public RemoteExceptionWrapper()
  {
  }
  /**
   * Create a basic RemoteExceptionWrapper with a message.
   *
   * @param msg the exception message.
   */
  public RemoteExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Create a RemoteExceptionWrapper wrapping a root exception.
   *
   * @param rootCause the underlying wrapped exception.
   */
  public RemoteExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.getMessage());

    this.rootCause = rootCause;
  }

  public static RemoteExceptionWrapper create(Throwable rootCause)
  {
    if (rootCause instanceof RemoteExceptionWrapper)
      return (RemoteExceptionWrapper) rootCause;
    else
      return new RemoteExceptionWrapper(rootCause);
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  public Throwable getRootCause()
  {
    return rootCause;
  }

  /**
   * Returns the appropriate exception message.
   */
  public String getMessage()
  {
    if (rootCause != null)
      return rootCause.getMessage();
    else
      return super.getMessage();
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace()
  {
    if (rootCause != null)
      rootCause.printStackTrace();
    else
      super.printStackTrace();
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintStream os)
  {
    if (rootCause != null)
      rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }

  /**
   * Prints the stack trace, preferring the root cause if it exists.
   */
  public void printStackTrace(PrintWriter os)
  {
    if (rootCause != null)
      rootCause.printStackTrace(os);
    else
      super.printStackTrace(os);
  }

  /**
   * Print the exception as a string.
   */
  public String toString()
  {
    if (rootCause == null)
      return super.toString();
    else
      return getClass().getName() + ": " + rootCause;
  }
}

