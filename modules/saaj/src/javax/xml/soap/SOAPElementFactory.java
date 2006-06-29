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
 * public class SOAPElementFactoryextends Object SOAPElementFactory is a
 * factory for XML fragments that will eventually end up in the SOAP part.
 * These fragments can be inserted as children of the SOAPHeader or SOAPBody or
 * SOAPEnvelope. Elements created using this factory do not have the properties
 * of an element that lives inside a SOAP header document. These elements are
 * copied into the XML document tree when they are inserted. See
 * Also:SOAPFactory
 */
class SOAPElementFactory {

  /**
   * Deprecated. Create a SOAPElement object initialized with the given Name
   * object.
   */
  public SOAPElement create(Name name) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Deprecated. Create a SOAPElement object initialized with the given local
   * name.
   */
  public SOAPElement create(String localName) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Deprecated. Create a new SOAPElement object with the given local name,
   * prefix and uri.
   */
  public SOAPElement create(String localName, String prefix, String uri) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Deprecated. Creates a new instance of SOAPElementFactory.
   */
  public static SOAPElementFactory newInstance() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

}

