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

package javax.xml.soap;

/**
 * An exception that signals that a SOAP exception has occurred. A
 * SOAPException object may contain a String that gives the reason for the
 * exception, an embedded Throwable object, or both. This class provides
 * methods for retrieving reason messages and for retrieving the embedded
 * Throwable object. Typical reasons for throwing a SOAPException object are
 * problems such as difficulty setting a header, not being able to send a
 * message, and not being able to get a connection with the provider. Reasons
 * for embedding a Throwable object include problems such as input/output
 * errors or a parsing problem, such as an error in parsing a header. See
 * Also:Serialized Form
 */
public class SOAPException extends Exception {

  /**
   * Constructs a SOAPException object with no reason or embedded Throwable
   * object.
   */
  public SOAPException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a SOAPException object with the given String as the reason for
   * the exception being thrown. Parameters:reason - a description of what
   * caused the exception
   */
  public SOAPException(String reason)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a SOAPException object with the given String as the reason for
   * the exception being thrown and the given Throwable object as an embedded
   * exception. Parameters:reason - a description of what caused the
   * exceptioncause - a Throwable object that is to be embedded in this
   * SOAPException object
   */
  public SOAPException(String reason, Throwable cause)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Constructs a SOAPException object initialized with the given Throwable
   * object.
   */
  public SOAPException(Throwable cause)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the Throwable object embedded in this SOAPException if there is
   * one. Otherwise, this method returns null.
   */
  public Throwable getCause()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns the detail message for this SOAPException object. If there is an
   * embedded Throwable object, and if the SOAPException object has no detail
   * message of its own, this method will return the detail message from the
   * embedded Throwable object.
   */
  public String getMessage()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Initializes the cause field of this SOAPException object with the given
   * Throwable object. This method can be called at most once. It is generally
   * called from within the constructor or immediately after the constructor
   * has returned a new SOAPException object. If this SOAPException object was
   * created with the constructor SOAPException(Throwable) or
   * SOAPException(String,Throwable), meaning that its cause field already has
   * a value, this method cannot be called even once.
   */
  public Throwable initCause(Throwable cause)
  {
    throw new UnsupportedOperationException();
  }

}

