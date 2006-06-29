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
 * A factory for creating SOAPConnection objects. Implementation of this class
 * is optional. If SOAPConnectionFactory.newInstance() throws an
 * UnsupportedOperationException then the implementation does not support the
 * SAAJ communication infrastructure. Otherwise SOAPConnection objects can be
 * created by calling createConnection() on the newly created
 * SOAPConnectionFactory object.
 */
public abstract class SOAPConnectionFactory {
  public SOAPConnectionFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new SOAPConnection.
   */
  public abstract SOAPConnection createConnection() throws SOAPException;


  /**
   * Creates an instance of the default SOAPConnectionFactory object.
   */
  public static SOAPConnectionFactory newInstance() throws SOAPException, UnsupportedOperationException
  {
    throw new UnsupportedOperationException();
  }

}

