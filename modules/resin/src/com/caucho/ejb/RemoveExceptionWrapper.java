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
 */

package com.caucho.ejb;

import java.io.*;
import java.util.*;

import javax.ejb.*;

import com.caucho.util.*;

/**
 * Wraps the actual exception with an EJB exception
 */
public class RemoveExceptionWrapper extends javax.ejb.RemoveException
  implements ExceptionWrapper {
  private Throwable rootCause;

  /**
   * Null constructors for Serializable
   */
  public RemoveExceptionWrapper()
  {
  }
  /**
   * Remove a basic RemoveExceptionWrapper with a message.
   *
   * @param msg the exception message.
   */
  public RemoveExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Remove a RemoveExceptionWrapper wrapping a root exception.
   *
   * @param rootCause the underlying wrapped exception.
   */
  public RemoveExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.getMessage());

    this.rootCause = rootCause;
  }

  /**
   * Wraps and exception with a remove exception wrapper.
   */
  public static RemoveException create(Throwable rootCause)
  {
    while (rootCause instanceof ExceptionWrapper) {
      ExceptionWrapper wrapper = (ExceptionWrapper) rootCause;

      if (wrapper.getRootCause() != null)
        rootCause = wrapper.getRootCause();
      else
        break;
    }

    if (rootCause instanceof RemoveException)
      return (RemoveException) rootCause;
    else
      return new RemoveExceptionWrapper(rootCause);
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

