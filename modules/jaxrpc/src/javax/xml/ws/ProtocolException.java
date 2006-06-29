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
 * The ProtocolException class is a base class for exceptions related to a
 * specific protocol binding. Subclasses are used to communicate protocol level
 * fault information to clients and may be used on the server to control the
 * protocol specific fault representation. Since: JAX-WS 2.0 See
 * Also:Serialized Form
 */
public class ProtocolException extends WebServiceException {

  /**
   * Constructs a new protocol exception with null as its detail message. The
   * cause is not initialized, and may subsequently be initialized by a call to
   * Throwable.initCause(java.lang.Throwable).
   */
  public ProtocolException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new protocol exception with the specified detail message. The
   * cause is not initialized, and may subsequently be initialized by a call to
   * Throwable.initCause(java.lang.Throwable). Parameters:message - the detail
   * message. The detail message is saved for later retrieval by the
   * Throwable.getMessage() method.
   */
  public ProtocolException(String message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new runtime exception with the specified detail message and
   * cause. Note that the detail message associated with cause is not
   * automatically incorporated in this runtime exception's detail message.
   * Parameters:message - the detail message (which is saved for later
   * retrieval by the Throwable.getMessage() method).cause - the cause (which
   * is saved for later retrieval by the Throwable.getCause() method). (A null
   * value is permitted, and indicates that the cause is nonexistent or
   * unknown.)
   */
  public ProtocolException(String message, Throwable cause)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a new runtime exception with the specified cause and a detail
   * message of (cause==null ? null : cause.toString()) (which typically
   * contains the class and detail message of cause). This constructor is
   * useful for runtime exceptions that are little more than wrappers for other
   * throwables. Parameters:cause - the cause (which is saved for later
   * retrieval by the Throwable.getCause() method). (A null value is permitted,
   * and indicates that the cause is nonexistent or unknown.)
   */
  public ProtocolException(Throwable cause)
  {
    throw new UnsupportedOperationException();
  }

}

