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

package javax.xml.bind;
import java.io.*;

/**
 * This exception indicates that a violation of a dynamically checked type
 * constraint was detected. This exception can be thrown by the generated
 * setter methods of the schema derived Java content classes. However, since
 * fail-fast validation is an optional feature for JAXB Providers to support,
 * not all setter methods will throw this exception when a type constraint is
 * violated. If this exception is throw while invoking a fail-fast setter, the
 * value of the property is guaranteed to remain unchanged, as if the setter
 * were never called. Since: JAXB1.0 Version: $Revision: 1.1 $ Author: Ryan
 * Shoemaker, Sun Microsystems, Inc.Joe Fialli, Sun Microsystems, Inc. See
 * Also:ValidationEvent, Serialized Form
 */
public class TypeConstraintException extends RuntimeException {

  /**
   * Construct a TypeConstraintException with the specified detail message. The
   * errorCode and linkedException will default to null. Parameters:message - a
   * description of the exception
   */
  public TypeConstraintException(String message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a TypeConstraintException with the specified detail message and
   * vendor specific errorCode. The linkedException will default to null.
   * Parameters:message - a description of the exceptionerrorCode - a string
   * specifying the vendor specific error code
   */
  public TypeConstraintException(String message, String errorCode)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a TypeConstraintException with the specified detail message,
   * vendor specific errorCode, and linkedException. Parameters:message - a
   * description of the exceptionerrorCode - a string specifying the vendor
   * specific error codeexception - the linked exception
   */
  public TypeConstraintException(String message, String errorCode, Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a TypeConstraintException with the specified detail message and
   * linkedException. The errorCode will default to null. Parameters:message -
   * a description of the exceptionexception - the linked exception
   */
  public TypeConstraintException(String message, Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a TypeConstraintException with a linkedException. The detail
   * message and vendor specific errorCode will default to null.
   * Parameters:exception - the linked exception
   */
  public TypeConstraintException(Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Get the vendor specific error code
   */
  public String getErrorCode()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Get the linked exception
   */
  public Throwable getLinkedException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Prints this TypeConstraintException and its stack trace (including the
   * stack trace of the linkedException if it is non-null) to System.err.
   */
  public void printStackTrace()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Prints this TypeConstraintException and its stack trace (including the
   * stack trace of the linkedException if it is non-null) to the PrintStream.
   */
  public void printStackTrace(PrintStream s)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Add a linked Exception.
   */
  public void setLinkedException(Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns a short description of this TypeConstraintException.
   */
  public String toString()
  {
    throw new UnsupportedOperationException();
  }

}

