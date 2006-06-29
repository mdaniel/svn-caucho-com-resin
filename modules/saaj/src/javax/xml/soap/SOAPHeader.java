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
import javax.xml.namespace.*;
import java.util.*;

/**
 * A representation of the SOAP header element. A SOAP header element consists
 * of XML data that affects the way the application-specific content is
 * processed by the message provider. For example, transaction semantics,
 * authentication information, and so on, can be specified as the content of a
 * SOAPHeader object. A SOAPEnvelope object contains an empty SOAPHeader object
 * by default. If the SOAPHeader object, which is optional, is not needed, it
 * can be retrieved and deleted with the following line of code. The variable
 * se is a SOAPEnvelope object. se.getHeader().detachNode(); A SOAPHeader
 * object is created with the SOAPEnvelope method addHeader. This method, which
 * creates a new header and adds it to the envelope, may be called only after
 * the existing header has been removed. se.getHeader().detachNode();
 * SOAPHeader sh = se.addHeader(); A SOAPHeader object can have only
 * SOAPHeaderElement objects as its immediate children. The method
 * addHeaderElement creates a new HeaderElement object and adds it to the
 * SOAPHeader object. In the following line of code, the argument to the method
 * addHeaderElement is a Name object that is the name for the new HeaderElement
 * object. SOAPHeaderElement shElement = sh.addHeaderElement(name); See
 * Also:SOAPHeaderElement
 */
public interface SOAPHeader extends SOAPElement {

  /**
   * Creates a new SOAPHeaderElement object initialized with the specified name
   * and adds it to this SOAPHeader object.
   */
  abstract SOAPHeaderElement addHeaderElement(Name name) throws SOAPException;


  /**
   * Creates a new SOAPHeaderElement object initialized with the specified
   * qname and adds it to this SOAPHeader object.
   */
  abstract SOAPHeaderElement addHeaderElement(QName qname) throws SOAPException;


  /**
   * Creates a new NotUnderstood SOAPHeaderElement object initialized with the
   * specified name and adds it to this SOAPHeader object. This operation is
   * supported only by SOAP 1.2.
   */
  abstract SOAPHeaderElement addNotUnderstoodHeaderElement(QName name) throws SOAPException;


  /**
   * Creates a new Upgrade SOAPHeaderElement object initialized with the
   * specified List of supported SOAP URIs and adds it to this SOAPHeader
   * object. This operation is supported on both SOAP 1.1 and SOAP 1.2 header.
   */
  abstract SOAPHeaderElement addUpgradeHeaderElement(Iterator supportedSOAPURIs) throws SOAPException;


  /**
   * Creates a new Upgrade SOAPHeaderElement object initialized with the
   * specified supported SOAP URI and adds it to this SOAPHeader object. This
   * operation is supported on both SOAP 1.1 and SOAP 1.2 header.
   */
  abstract SOAPHeaderElement addUpgradeHeaderElement(String supportedSoapUri) throws SOAPException, SOAPException;

  abstract SOAPHeaderElement addUpgradeHeaderElement(String[] supportedSoapUris);


  /**
   * Returns an Iterator over all the SOAPHeaderElement objects in this
   * SOAPHeader object.
   */
  abstract Iterator examineAllHeaderElements();


  /**
   * Returns an Iterator over all the SOAPHeaderElement objects in this
   * SOAPHeader object that have the specified . An is a global attribute that
   * indicates the intermediate parties that should process a message before it
   * reaches its ultimate receiver. An actor receives the message and processes
   * it before sending it on to the next actor. The default actor is the
   * ultimate intended recipient for the message, so if no actor attribute is
   * included in a SOAPHeader object, it is sent to the ultimate receiver along
   * with the message body. In SOAP 1.2 the env:actor attribute is replaced by
   * the env:role attribute, but with essentially the same semantics.
   */
  abstract Iterator examineHeaderElements(String actor);


  /**
   * Returns an Iterator over all the SOAPHeaderElement objects in this
   * SOAPHeader object that have the specified and that have a MustUnderstand
   * attribute whose value is equivalent to true. In SOAP 1.2 the env:actor
   * attribute is replaced by the env:role attribute, but with essentially the
   * same semantics.
   */
  abstract Iterator examineMustUnderstandHeaderElements(String actor);


  /**
   * Returns an Iterator over all the SOAPHeaderElement objects in this
   * SOAPHeader object and detaches them from this SOAPHeader object.
   */
  abstract Iterator extractAllHeaderElements();


  /**
   * Returns an Iterator over all the SOAPHeaderElement objects in this
   * SOAPHeader object that have the specified and detaches them from this
   * SOAPHeader object. This method allows an actor to process the parts of the
   * SOAPHeader object that apply to it and to remove them before passing the
   * message on to the next actor. In SOAP 1.2 the env:actor attribute is
   * replaced by the env:role attribute, but with essentially the same
   * semantics.
   */
  abstract Iterator extractHeaderElements(String actor);

}

