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
 * A point-to-point connection that a client can use for sending messages
 * directly to a remote party (represented by a URL, for instance). The
 * SOAPConnection class is optional. Some implementations may not implement
 * this interface in which case the call to SOAPConnectionFactory.newInstance()
 * (see below) will throw an UnsupportedOperationException. A client can obtain
 * a SOAPConnection object using a SOAPConnectionFactory object as in the
 * following example: SOAPConnectionFactory factory =
 * SOAPConnectionFactory.newInstance(); SOAPConnection con =
 * factory.createConnection(); A SOAPConnection object can be used to send
 * messages directly to a URL following the request/response paradigm. That is,
 * messages are sent using the method call, which sends the message and then
 * waits until it gets a reply.
 */
public abstract class SOAPConnection {
  public SOAPConnection()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Sends the given message to the specified endpoint and blocks until it has
   * returned the response.
   */
  public abstract SOAPMessage call(SOAPMessage request, Object to) throws SOAPException;


  /**
   * Closes this SOAPConnection object.
   */
  public abstract void close() throws SOAPException;


  /**
   * Gets a message from a specific endpoint and blocks until it receives,
   */
  public SOAPMessage get(Object to) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

}

