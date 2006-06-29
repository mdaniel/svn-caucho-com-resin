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
import java.util.*;

/**
 * The BindingProvider interface provides access to the protocol binding and
 * associated context objects for request and response message processing.
 * Since: JAX-WS 2.0 See Also:Binding
 */
public interface BindingProvider {

  /**
   * Standard property: Target service endpoint address. The URI scheme for the
   * endpoint address specification must correspond to the protocol/transport
   * binding for the binding in use. Type: java.lang.String See Also:Constant
   * Field Values
   */
  static final String ENDPOINT_ADDRESS_PROPERTY="javax.xml.ws.service.endpoint.address";


  /**
   * Standard property: Password for authentication. Type: java.lang.String See
   * Also:Constant Field Values
   */
  static final String PASSWORD_PROPERTY="javax.xml.ws.security.auth.password";


  /**
   * Standard property: This boolean property is used by a service client to
   * indicate whether or not it wants to participate in a session with a
   * service endpoint. If this property is set to true, the service client
   * indicates that it wants the session to be maintained. If set to false, the
   * session is not maintained. The default value for this property is false.
   * Type: java.lang.Boolean See Also:Constant Field Values
   */
  static final String SESSION_MAINTAIN_PROPERTY="javax.xml.ws.session.maintain";


  /**
   * Standard property for SOAPAction. Indicates the SOAPAction URI if the
   * javax.xml.ws.soap.http.soapaction.use property is set to true. Type:
   * java.lang.String See Also:Constant Field Values
   */
  static final String SOAPACTION_URI_PROPERTY="javax.xml.ws.soap.http.soapaction.uri";


  /**
   * Standard property for SOAPAction. This boolean property indicates whether
   * or not SOAPAction is to be used. The default value of this property is
   * false indicating that the SOAPAction is not used. Type: java.lang.Boolean
   * See Also:Constant Field Values
   */
  static final String SOAPACTION_USE_PROPERTY="javax.xml.ws.soap.http.soapaction.use";


  /**
   * Standard property: User name for authentication. Type: java.lang.String
   * See Also:Constant Field Values
   */
  static final String USERNAME_PROPERTY="javax.xml.ws.security.auth.username";


  /**
   * Get the Binding for this binding provider.
   */
  abstract Binding getBinding();


  /**
   * Get the context that is used to initialize the message context for request
   * messages. Modifications to the request context do not affect the message
   * context of either synchronous or asynchronous operations that have already
   * been started.
   */
  abstract Map<String,Object> getRequestContext();


  /**
   * Get the context that resulted from processing a response message. The
   * returned context is for the most recently completed synchronous operation.
   * Subsequent synchronous operation invocations overwrite the response
   * context. Asynchronous operations return their response context via the
   * Response interface.
   */
  abstract Map<String,Object> getResponseContext();

}

