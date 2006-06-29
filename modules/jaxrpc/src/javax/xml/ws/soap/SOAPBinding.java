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

package javax.xml.ws.soap;
import javax.xml.soap.*;
import javax.xml.ws.*;
import java.util.*;

/**
 * The SOAPBinding interface is an abstraction for the SOAP binding. Since:
 * JAX-WS 2.0
 */
public interface SOAPBinding extends Binding {

  /**
   * A constant representing the identity of the SOAP 1.1 over HTTP binding.
   * See Also:Constant Field Values
   */
  static final String SOAP11HTTP_BINDING="http://schemas.xmlsoap.org/wsdl/soap/http";


  /**
   * A constant representing the identity of the SOAP 1.1 over HTTP binding
   * with MTOM enabled by default. See Also:Constant Field Values
   */
  static final String SOAP11HTTP_MTOM_BINDING="http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true";


  /**
   * A constant representing the identity of the SOAP 1.2 over HTTP binding.
   * See Also:Constant Field Values
   */
  static final String SOAP12HTTP_BINDING="http://www.w3.org/2003/05/soap/bindings/HTTP/";


  /**
   * A constant representing the identity of the SOAP 1.2 over HTTP binding
   * with MTOM enabled by default. See Also:Constant Field Values
   */
  static final String SOAP12HTTP_MTOM_BINDING="http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true";


  /**
   * Gets the SAAJ MessageFactory instance used by this SOAP binding.
   */
  abstract MessageFactory getMessageFactory();


  /**
   * Gets the roles played by the SOAP binding instance.
   */
  abstract Set<String> getRoles();


  /**
   * Gets the SAAJ SOAPFactory instance used by this SOAP binding.
   */
  abstract SOAPFactory getSOAPFactory();


  /**
   * Returns true if the use of MTOM is enabled.
   */
  abstract boolean isMTOMEnabled();


  /**
   * Enables or disables use of MTOM.
   */
  abstract void setMTOMEnabled(boolean flag);


  /**
   * Sets the roles played by the SOAP binding instance.
   */
  abstract void setRoles(Set<String> roles);

}

