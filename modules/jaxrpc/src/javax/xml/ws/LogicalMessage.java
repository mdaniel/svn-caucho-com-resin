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

package javax.xml.ws;
import javax.xml.transform.*;
/* XXX: uncomment when we have jaxb
import javax.xml.bind.*;
*/

/**
 * The LogicalMessage interface represents a protocol agnostic XML message and
 * contains methods that provide access to the payload of the message. Since:
 * JAX-WS 2.0
 */
public interface LogicalMessage {

  /**
   * Gets the message payload as an XML source, may be called multiple times on
   * the same LogicalMessage instance, always returns a new Source that may be
   * used to retrieve the entire message payload. If the returned Source is an
   * instance of DOMSource, then modifications to the encapsulated DOM tree
   * change the message payload in-place, there is no need to susequently call
   * setPayload. Other types of Source provide only read access to the message
   * payload.
   */
  abstract Source getPayload();


  /**
   * Gets the message payload as a JAXB object. Note that there is no
   * connection between the returned object and the message payload, changes to
   * the payload require calling setPayload.
   */
/* XXX: uncomment when we have jaxb
  abstract Object getPayload(JAXBContext context);
*/

  /**
   * Sets the message payload
   */
/* XXX: uncomment when we have jaxb
  abstract void setPayload(Object payload, JAXBContext context);
*/

  /**
   * Sets the message payload
   */
  abstract void setPayload(Source payload);

}

