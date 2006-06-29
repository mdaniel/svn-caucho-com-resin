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
 * The access point for the implementation classes of the factories defined in
 * the SAAJ API. All of the newInstance methods defined on factories in SAAJ
 * 1.3 defer to instances of this class to do the actual object creation. The
 * implementations of newInstance() methods (in SOAPFactory and MessageFactory)
 * that existed in SAAJ 1.2 have been updated to also delegate to the
 * SAAJMetaFactory when the SAAJ 1.2 defined lookup fails to locate the Factory
 * implementation class name. SAAJMetaFactory is a service provider interface.
 * There are no public methods on this class. Since: SAAJ 1.3 Author: SAAJ RI
 * Development Team
 */
public abstract class SAAJMetaFactory {
  protected SAAJMetaFactory()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a MessageFactory object for the given String protocol.
   */
  protected abstract MessageFactory newMessageFactory(String protocol) throws SOAPException;


  /**
   * Creates a SOAPFactory object for the given String protocol.
   */
  protected abstract SOAPFactory newSOAPFactory(String protocol) throws SOAPException;

}

