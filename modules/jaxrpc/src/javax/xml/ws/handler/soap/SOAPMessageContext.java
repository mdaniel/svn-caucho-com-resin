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

package javax.xml.ws.handler.soap;
import javax.xml.soap.*;
import javax.xml.namespace.*;
import javax.xml.ws.handler.*;
/* XXX: uncomment when we have JAXB
import javax.xml.bind.*;
*/
import java.util.*;

/**
 * The interface SOAPMessageContext provides access to the SOAP message for
 * either RPC request or response. The javax.xml.soap.SOAPMessage specifies the
 * standard Java API for the representation of a SOAP 1.1 message with
 * attachments. Since: JAX-WS 2.0 See Also:SOAPMessage
 */
public interface SOAPMessageContext
    extends MessageContext, Map<String,Object> {

  /**
   * Gets headers that have a particular qualified name from the message in the
   * message context. Note that a SOAP message can contain multiple headers
   * with the same qualified name.
   */
    /* XXX: uncomment when we have JAXB
  abstract Object[] getHeaders(QName header, JAXBContext context, boolean allRoles);
    */

  /**
   * Gets the SOAPMessage from this message context. Modifications to the
   * returned SOAPMessage change the message in-place, there is no need to
   * susequently call setMessage.
   */
  abstract SOAPMessage getMessage();


  /**
   * Gets the SOAP actor roles associated with an execution of the handler
   * chain. Note that SOAP actor roles apply to the SOAP node and are managed
   * using SOAPBinding.setRoles and SOAPBinding.getRoles. Handler instances in
   * the handler chain use this information about the SOAP actor roles to
   * process the SOAP header blocks. Note that the SOAP actor roles are
   * invariant during the processing of SOAP message through the handler chain.
   */
  abstract List<String> getRoles();


  /**
   * Sets the SOAPMessage in this message context
   */
  abstract void setMessage(SOAPMessage message);

}

