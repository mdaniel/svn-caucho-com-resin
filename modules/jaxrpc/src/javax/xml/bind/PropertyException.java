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

/**
 * This exception indicates that an error was encountered while getting or
 * setting a property. Since: JAXB1.0 Version: $Revision: 1.1 $ $Date:
 * 2004/12/14 21:50:40 $ Author: Ryan Shoemaker, Sun Microsystems, Inc.Kohsuke
 * Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun Microsystems, Inc. See
 * Also:JAXBContext, Validator, Unmarshaller, Serialized Form
 */
public class PropertyException extends JAXBException {

  /**
   * Construct a PropertyException with the specified detail message. The
   * errorCode and linkedException will default to null. Parameters:message - a
   * description of the exception
   */
  public PropertyException(String message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a PropertyException whose message field is set based on the name
   * of the property and value.toString(). Parameters:name - the name of the
   * property related to this exceptionvalue - the value of the property
   * related to this exception
   */
  public PropertyException(String name, Object value)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a PropertyException with the specified detail message and vendor
   * specific errorCode. The linkedException will default to null.
   * Parameters:message - a description of the exceptionerrorCode - a string
   * specifying the vendor specific error code
   */
  public PropertyException(String message, String errorCode)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a PropertyException with the specified detail message, vendor
   * specific errorCode, and linkedException. Parameters:message - a
   * description of the exceptionerrorCode - a string specifying the vendor
   * specific error codeexception - the linked exception
   */
  public PropertyException(String message, String errorCode, Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a PropertyException with the specified detail message and
   * linkedException. The errorCode will default to null. Parameters:message -
   * a description of the exceptionexception - the linked exception
   */
  public PropertyException(String message, Throwable exception)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct a PropertyException with a linkedException. The detail message
   * and vendor specific errorCode will default to null. Parameters:exception -
   * the linked exception
   */
  public PropertyException(Throwable exception)
  {
    throw new UnsupportedOperationException();
  }

}

