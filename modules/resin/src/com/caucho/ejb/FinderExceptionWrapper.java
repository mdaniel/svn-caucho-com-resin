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

import com.caucho.amber.AmberObjectNotFoundException;
import com.caucho.util.ExceptionWrapper;

import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps the actual exception with an EJB exception
 */
public class FinderExceptionWrapper extends javax.ejb.FinderException
  implements ExceptionWrapper {
  private static final Logger log
    = Logger.getLogger(FinderExceptionWrapper.class.getName());
  
  /**
   * Null constructors for Serializable
   */
  public FinderExceptionWrapper()
  {
  }
  /**
   * Finder a basic FinderExceptionWrapper with a message.
   *
   * @param msg the exception message.
   */
  public FinderExceptionWrapper(String msg)
  {
    super(msg);
  }

  /**
   * Finder a FinderExceptionWrapper wrapping a root exception.
   *
   * @param rootCause the underlying wrapped exception.
   */
  public FinderExceptionWrapper(Throwable rootCause)
  {
    super(rootCause.toString());

    initCause(rootCause);
  }

  /**
   * Wraps and exception with a finder exception wrapper.
   */
  public static FinderException create(Throwable rootCause)
  {
    while (rootCause instanceof ExceptionWrapper) {
      ExceptionWrapper wrapper = (ExceptionWrapper) rootCause;

      if (wrapper.getRootCause() != null)
        rootCause = wrapper.getRootCause();
      else
        break;
    }

    if (rootCause instanceof FinderException)
      return (FinderException) rootCause;
    else if (rootCause instanceof AmberObjectNotFoundException) {
      log.log(Level.FINER, rootCause.toString(), rootCause);
      
      return new ObjectNotFoundException(rootCause.getMessage());
    }
    else
      return new FinderExceptionWrapper(rootCause);
  }

  /**
   * Returns the root exception if it exists.
   *
   * @return the underlying wrapped exception.
   */
  public Throwable getRootCause()
  {
    return getCause();
  }
}

