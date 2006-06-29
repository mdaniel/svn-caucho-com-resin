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
 * The container for the SOAPHeader and SOAPBody portions of a SOAPPart object.
 * By default, a SOAPMessage object is created with a SOAPPart object that has
 * a SOAPEnvelope object. The SOAPEnvelope object by default has an empty
 * SOAPBody object and an empty SOAPHeader object. The SOAPBody object is
 * required, and the SOAPHeader object, though optional, is used in the
 * majority of cases. If the SOAPHeader object is not needed, it can be
 * deleted, which is shown later. A client can access the SOAPHeader and
 * SOAPBody objects by calling the methods SOAPEnvelope.getHeader and
 * SOAPEnvelope.getBody. The following lines of code use these two methods
 * after starting with the SOAPMessage object message to get the SOAPPart
 * object sp, which is then used to get the SOAPEnvelope object se. SOAPPart sp
 * = message.getSOAPPart(); SOAPEnvelope se = sp.getEnvelope(); SOAPHeader sh =
 * se.getHeader(); SOAPBody sb = se.getBody(); It is possible to change the
 * body or header of a SOAPEnvelope object by retrieving the current one,
 * deleting it, and then adding a new body or header. The javax.xml.soap.Node
 * method deleteNode deletes the XML element (node) on which it is called. For
 * example, the following line of code deletes the SOAPBody object that is
 * retrieved by the method getBody. se.getBody().detachNode(); To create a
 * SOAPHeader object to replace the one that was removed, a client uses the
 * method SOAPEnvelope.addHeader, which creates a new header and adds it to the
 * SOAPEnvelope object. Similarly, the method addBody creates a new SOAPBody
 * object and adds it to the SOAPEnvelope object. The following code fragment
 * retrieves the current header, removes it, and adds a new one. Then it
 * retrieves the current body, removes it, and adds a new one. SOAPPart sp =
 * message.getSOAPPart(); SOAPEnvelope se = sp.getEnvelope();
 * se.getHeader().detachNode(); SOAPHeader sh = se.addHeader();
 * se.getBody().detachNode(); SOAPBody sb = se.addBody(); It is an error to add
 * a SOAPBody or SOAPHeader object if one already exists. The SOAPEnvelope
 * interface provides three methods for creating Name objects. One method
 * creates Name objects with a local name, a namespace prefix, and a namesapce
 * URI. The second method creates Name objects with a local name and a
 * namespace prefix, and the third creates Name objects with just a local name.
 * The following line of code, in which se is a SOAPEnvelope object, creates a
 * new Name object with all three. Name name =
 * se.createName("GetLastTradePrice", "WOMBAT", "http://www.wombat.org/trader");
 */
public interface SOAPEnvelope extends SOAPElement {

  /**
   * Creates a SOAPBody object and sets it as the SOAPBody object for this
   * SOAPEnvelope object. It is illegal to add a body when the envelope already
   * contains a body. Therefore, this method should be called only after the
   * existing body has been removed.
   */
  abstract SOAPBody addBody() throws SOAPException;


  /**
   * Creates a SOAPHeader object and sets it as the SOAPHeader object for this
   * SOAPEnvelope object. It is illegal to add a header when the envelope
   * already contains a header. Therefore, this method should be called only
   * after the existing header has been removed.
   */
  abstract SOAPHeader addHeader() throws SOAPException;


  /**
   * Creates a new Name object initialized with the given local name. This
   * factory method creates Name objects for use in the SOAP/XML document.
   */
  abstract Name createName(String localName) throws SOAPException;


  /**
   * Creates a new Name object initialized with the given local name, namespace
   * prefix, and namespace URI. This factory method creates Name objects for
   * use in the SOAP/XML document.
   */
  abstract Name createName(String localName, String prefix, String uri) throws SOAPException;


  /**
   * Returns the SOAPBody object associated with this SOAPEnvelope object. A
   * new SOAPMessage object is by default created with a SOAPEnvelope object
   * that contains an empty SOAPBody object. As a result, the method getBody
   * will always return a SOAPBody object unless the body has been removed and
   * a new one has not been added.
   */
  abstract SOAPBody getBody() throws SOAPException;


  /**
   * Returns the SOAPHeader object for this SOAPEnvelope object. A new
   * SOAPMessage object is by default created with a SOAPEnvelope object that
   * contains an empty SOAPHeader object. As a result, the method getHeader
   * will always return a SOAPHeader object unless the header has been removed
   * and a new one has not been added.
   */
  abstract SOAPHeader getHeader() throws SOAPException;

}

