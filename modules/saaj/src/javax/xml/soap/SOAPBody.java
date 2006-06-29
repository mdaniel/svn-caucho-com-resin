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
import org.w3c.dom.*;
import javax.xml.namespace.*;
import java.util.*;

/**
 * An object that represents the contents of the SOAP body element in a SOAP
 * message. A SOAP body element consists of XML data that affects the way the
 * application-specific content is processed. A SOAPBody object contains
 * SOAPBodyElement objects, which have the content for the SOAP body. A
 * SOAPFault object, which carries status and/or error information, is an
 * example of a SOAPBodyElement object. See Also:SOAPFault
 */
public interface SOAPBody extends SOAPElement {

  /**
   * Creates a new SOAPBodyElement object with the specified name and adds it
   * to this SOAPBody object.
   */
  abstract SOAPBodyElement addBodyElement(Name name) throws SOAPException;


  /**
   * Creates a new SOAPBodyElement object with the specified QName and adds it
   * to this SOAPBody object.
   */
  abstract SOAPBodyElement addBodyElement(QName qname) throws SOAPException;


  /**
   * Adds the root node of the DOM to this SOAPBody object. Calling this method
   * invalidates the document parameter. The client application should discard
   * all references to this Document and its contents upon calling addDocument.
   * The behavior of an application that continues to use such references is
   * undefined.
   */
  abstract SOAPBodyElement addDocument(Document document) throws SOAPException;


  /**
   * Creates a new SOAPFault object and adds it to this SOAPBody object. The
   * new SOAPFault will have default values set for the mandatory child
   * elements. The type of the SOAPFault will be a SOAP 1.1 or a SOAP 1.2
   * SOAPFault depending on the protocol specified while creating the
   * MessageFactory instance. A SOAPBody may contain at most one SOAPFault
   * child element.
   */
  abstract SOAPFault addFault() throws SOAPException;


  /**
   * Creates a new SOAPFault object and adds it to this SOAPBody object. The
   * type of the SOAPFault will be a SOAP 1.1 or a SOAP 1.2 SOAPFault depending
   * on the protocol specified while creating the MessageFactory instance. For
   * SOAP 1.2 the faultCode parameter is the value of the Fault/Code/Value
   * element and the faultString parameter is the value of the
   * Fault/Reason/Text element. For SOAP 1.1 the faultCode parameter is the
   * value of the faultcode element and the faultString parameter is the value
   * of the faultstring element. In case of a SOAP 1.2 fault, the default value
   * for the mandatory xml:lang attribute on the Fault/Reason/Text element will
   * be set to java.util.Locale.getDefault() A SOAPBody may contain at most one
   * SOAPFault child element.
   */
  abstract SOAPFault addFault(Name faultCode, String faultString) throws SOAPException;


  /**
   * Creates a new SOAPFault object and adds it to this SOAPBody object. The
   * type of the SOAPFault will be a SOAP 1.1 or a SOAP 1.2 SOAPFault depending
   * on the protocol specified while creating the MessageFactory instance. For
   * SOAP 1.2 the faultCode parameter is the value of the Fault/Code/Value
   * element and the faultString parameter is the value of the
   * Fault/Reason/Text element. For SOAP 1.1 the faultCode parameter is the
   * value of the faultcode element and the faultString parameter is the value
   * of the faultstring element. A SOAPBody may contain at most one SOAPFault
   * child element.
   */
  abstract SOAPFault addFault(Name faultCode, String faultString, Locale locale) throws SOAPException;


  /**
   * Creates a new SOAPFault object and adds it to this SOAPBody object. The
   * type of the SOAPFault will be a SOAP 1.1 or a SOAP 1.2 SOAPFault depending
   * on the protocol specified while creating the MessageFactory instance. For
   * SOAP 1.2 the faultCode parameter is the value of the Fault/Code/Value
   * element and the faultString parameter is the value of the
   * Fault/Reason/Text element. For SOAP 1.1 the faultCode parameter is the
   * value of the faultcode element and the faultString parameter is the value
   * of the faultstring element. In case of a SOAP 1.2 fault, the default value
   * for the mandatory xml:lang attribute on the Fault/Reason/Text element will
   * be set to java.util.Locale.getDefault() A SOAPBody may contain at most one
   * SOAPFault child element
   */
  abstract SOAPFault addFault(QName faultCode, String faultString) throws SOAPException;


  /**
   * Creates a new SOAPFault object and adds it to this SOAPBody object. The
   * type of the SOAPFault will be a SOAP 1.1 or a SOAP 1.2 SOAPFault depending
   * on the protocol specified while creating the MessageFactory instance. For
   * SOAP 1.2 the faultCode parameter is the value of the Fault/Code/Value
   * element and the faultString parameter is the value of the
   * Fault/Reason/Text element. For SOAP 1.1 the faultCode parameter is the
   * value of the faultcode element and the faultString parameter is the value
   * of the faultstring element. A SOAPBody may contain at most one SOAPFault
   * child element.
   */
  abstract SOAPFault addFault(QName faultCode, String faultString, Locale locale) throws SOAPException;


  /**
   * Creates a new DOM and sets the first child of this SOAPBody as it's
   * document element. The child SOAPElement is removed as part of the process.
   */
  abstract Document extractContentAsDocument() throws SOAPException;


  /**
   * Returns the SOAPFault object in this SOAPBody object.
   */
  abstract SOAPFault getFault();


  /**
   * Indicates whether a SOAPFault object exists in this SOAPBody object.
   */
  abstract boolean hasFault();

}

