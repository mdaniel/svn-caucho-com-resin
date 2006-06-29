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
import javax.xml.transform.dom.*;

/**
 * Acts as a holder for the results of a JAXP transformation or a JAXB
 * marshalling, in the form of a SAAJ tree. These results should be accessed by
 * using the getResult() method. The DOMResult.getNode() method should be
 * avoided in almost all cases. Since: SAAJ 1.3 Author: XWS-Security
 * Development Team
 */
public class SAAJResult extends DOMResult {

  /**
   * Creates a SAAJResult that will present results in the form of a SAAJ tree
   * that supports the default (SOAP 1.1) protocol. This kind of SAAJResult is
   * meant for use in situations where the results will be used as a parameter
   * to a method that takes a parameter whose type, such as SOAPElement, is
   * drawn from the SAAJ API. When used in a transformation, the results are
   * populated into the SOAPPart of a SOAPMessage that is created internally.
   * The SOAPPart returned by DOMResult.getNode() is not guaranteed to be
   * well-formed. Throws: SOAPException - if there is a problem creating a
   * SOAPMessageSince: SAAJ 1.3
   */
  public SAAJResult() throws SOAPException
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a SAAJResult that will write the results as a child node of the
   * SOAPElement specified. In the normal case these results will be written
   * using DOM APIs and as a result may invalidate the structure of the SAAJ
   * tree. This kind of SAAJResult should only be used when the validity of the
   * incoming data can be guaranteed by means outside of the SAAJ
   * specification. Parameters:rootNode - - the root to which the results will
   * be appendedSince: SAAJ 1.3
   */
  public SAAJResult(SOAPElement rootNode)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a SAAJResult that will write the results into the SOAPPart of the
   * supplied SOAPMessage. In the normal case these results will be written
   * using DOM APIs and, as a result, the finished SOAPPart will not be
   * guaranteed to be well-formed unless the data used to create it is also
   * well formed. When used in a transformation the validity of the SOAPMessage
   * after the transformation can be guaranteed only by means outside SAAJ
   * specification. Parameters:message - - the message whose SOAPPart will be
   * populated as a result of some transformation or marshalling
   * operationSince: SAAJ 1.3
   */
  public SAAJResult(SOAPMessage message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Creates a SAAJResult that will present results in the form of a SAAJ tree
   * that supports the specified protocol. The DYNAMIC_SOAP_PROTOCOL is
   * ambiguous in this context and will cause this constructor to throw an
   * UnsupportedOperationException. This kind of SAAJResult is meant for use in
   * situations where the results will be used as a parameter to a method that
   * takes a parameter whose type, such as SOAPElement, is drawn from the SAAJ
   * API. When used in a transformation the results are populated into the
   * SOAPPart of a SOAPMessage that is created internally. The SOAPPart
   * returned by DOMResult.getNode() is not guaranteed to be well-formed.
   * Parameters:protocol - - the name of the SOAP protocol that the resulting
   * SAAJ tree should support Throws: SOAPException - if a SOAPMessage
   * supporting the specified protocol cannot be createdSince: SAAJ 1.3
   */
  public SAAJResult(String protocol) throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public Node getResult()
  {
    throw new UnsupportedOperationException();
  }

}

