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
 * An object representing the contents in the SOAP header part of the SOAP
 * envelope. The immediate children of a SOAPHeader object can be represented
 * only as SOAPHeaderElement objects. A SOAPHeaderElement object can have other
 * SOAPElement objects as its children.
 */
public interface SOAPHeaderElement extends SOAPElement {

  /**
   * Returns the uri of the attribute of this SOAPHeaderElement. If this
   * SOAPHeaderElement supports SOAP 1.2 then this call is equivalent to
   * getRole()
   */
  abstract String getActor();


  /**
   * Returns the boolean value of the mustUnderstand attribute for this
   * SOAPHeaderElement.
   */
  abstract boolean getMustUnderstand();


  /**
   * Returns the boolean value of the attribute for this SOAPHeaderElement
   */
  abstract boolean getRelay();


  /**
   * Returns the value of the attribute of this SOAPHeaderElement.
   */
  abstract String getRole();


  /**
   * Sets the actor associated with this SOAPHeaderElement object to the
   * specified actor. The default value of an actor is:
   * SOAPConstants.URI_SOAP_ACTOR_NEXT If this SOAPHeaderElement supports SOAP
   * 1.2 then this call is equivalent to setRole(String)
   */
  abstract void setActor(String actorURI);


  /**
   * Sets the mustUnderstand attribute for this SOAPHeaderElement object to be
   * either true or false. If the mustUnderstand attribute is on, the actor who
   * receives the SOAPHeaderElement must process it correctly. This ensures,
   * for example, that if the SOAPHeaderElement object modifies the message,
   * that the message is being modified correctly.
   */
  abstract void setMustUnderstand(boolean mustUnderstand);


  /**
   * Sets the attribute for this SOAPHeaderElement to be either true or false.
   * The SOAP relay attribute is set to true to indicate that the SOAP header
   * block must be relayed by any node that is targeted by the header block but
   * not actually process it. This attribute is ignored on header blocks whose
   * mustUnderstand attribute is set to true or that are targeted at the
   * ultimate reciever (which is the default). The default value of this
   * attribute is false.
   */
  abstract void setRelay(boolean relay) throws SOAPException;


  /**
   * Sets the Role associated with this SOAPHeaderElement object to the
   * specified Role.
   */
  abstract void setRole(String uri) throws SOAPException;

}

