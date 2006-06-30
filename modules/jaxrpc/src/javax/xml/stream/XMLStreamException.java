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

package javax.xml.stream;

/**
 * The base exception for unexpected processing errors. This Exception class is
 * used to report well-formedness errors as well as unexpected processing
 * conditions. Version: 1.0 Author: Copyright (c) 2003 by BEA Systems. All
 * Rights Reserved. See Also:Serialized Form
 */
public class XMLStreamException extends Exception {
  protected Location location;

  protected Throwable nested;


  /**
   * Default constructor
   */
  public XMLStreamException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an exception with the assocated message. Parameters:msg - the
   * message to report
   */
  public XMLStreamException(String msg)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an exception with the assocated message, exception and location.
   * Parameters:msg - the message to reportlocation - the location of the error
   */
  public XMLStreamException(String msg, Location location)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an exception with the assocated message, exception and location.
   * Parameters:th - a nested exceptionmsg - the message to reportlocation -
   * the location of the error
   */
  public XMLStreamException(String msg, Location location, Throwable th)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an exception with the assocated message and exception
   * Parameters:th - a nested exceptionmsg - the message to report
   */
  public XMLStreamException(String msg, Throwable th)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Construct an exception with the assocated exception Parameters:th - a
   * nested exception
   */
  public XMLStreamException(Throwable th)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the location of the exception
   */
  public Location getLocation()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Gets the nested exception.
   */
  public Throwable getNestedException()
  {
    throw new UnsupportedOperationException();
  }

}

