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

package javax.xml.ws;

/**
 * The WebServiceException class is the base exception class for all JAX-WS API
 * runtime exceptions. Since: JAX-WS 2.0 See Also:Serialized Form
 */
public class WebServiceException extends RuntimeException {

  /**
   * Constructs a new exception with null as its detail message. The cause is
   * not initialized.
   */
  public WebServiceException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new exception with the specified detail message. The cause is
   * not initialized. Parameters:message - The detail message which is later
   * retrieved using the getMessage method
   */
  public WebServiceException(String message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new exception with the specified detail message and cause.
   * Parameters:message - The detail message which is later retrieved using the
   * getMessage methodcause - The cause which is saved for the later retrieval
   * throw by the getCause method
   */
  public WebServiceException(String message, Throwable cause)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new WebServiceException with the specified cause and a detail
   * message of (cause==null ? null : cause.toString()) (which typically
   * contains the class and detail message of cause). Parameters:cause - The
   * cause which is saved for the later retrieval throw by the getCause method.
   * (A null value is permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public WebServiceException(Throwable cause)
  {
    throw new UnsupportedOperationException();
  }

}

