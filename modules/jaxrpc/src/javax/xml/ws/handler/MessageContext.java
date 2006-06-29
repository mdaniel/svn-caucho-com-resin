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

package javax.xml.ws.handler;
import javax.xml.ws.handler.MessageContext.*;
import java.util.*;

/**
 * The interface MessageContext abstracts the message context that is processed
 * by a handler in the handle method. The MessageContext interface provides
 * methods to manage a property set. MessageContext properties enable handlers
 * in a handler chain to share processing related state. Since: JAX-WS 2.0
 */
public interface MessageContext extends Map<String,Object> {

  /**
   * Standard property: HTTP request headers. Type: java.util.Map> See
   * Also:Constant Field Values
   */
  static final String HTTP_REQUEST_HEADERS="javax.xml.ws.http.request.headers";


  /**
   * Standard property: HTTP request method. Type: java.lang.String See
   * Also:Constant Field Values
   */
  static final String HTTP_REQUEST_METHOD="javax.xml.ws.http.request.method";


  /**
   * Standard property: HTTP response status code. Type: java.lang.Integer See
   * Also:Constant Field Values
   */
  static final String HTTP_RESPONSE_CODE="javax.xml.ws.http.response.code";


  /**
   * Standard property: HTTP response headers. Type: java.util.Map> See
   * Also:Constant Field Values
   */
  static final String HTTP_RESPONSE_HEADERS="javax.xml.ws.http.response.headers";


  /**
   * Standard property: Map of attachments to a message for the inbound
   * message, key is the MIME Content-ID, value is a DataHandler. Type:
   * java.util.Map See Also:Constant Field Values
   */
  static final String INBOUND_MESSAGE_ATTACHMENTS="javax.xml.ws.binding.attachments.inbound";


  /**
   * Standard property: message direction, true for outbound messages, false
   * for inbound. Type: boolean See Also:Constant Field Values
   */
  static final String MESSAGE_OUTBOUND_PROPERTY="javax.xml.ws.handler.message.outbound";


  /**
   * Standard property: Map of attachments to a message for the outbound
   * message, key is the MIME Content-ID, value is a DataHandler. Type:
   * java.util.Map See Also:Constant Field Values
   */
  static final String OUTBOUND_MESSAGE_ATTACHMENTS="javax.xml.ws.binding.attachments.outbound";


  /**
   * Standard property: Request Path Info Type: String See Also:Constant Field
   * Values
   */
  static final String PATH_INFO="javax.xml.ws.http.request.pathinfo";


  /**
   * Standard property: Query string for request. Type: String See
   * Also:Constant Field Values
   */
  static final String QUERY_STRING="javax.xml.ws.http.request.querystring";


  /**
   * Standard property: servlet context object. Type:
   * javax.servlet.ServletContext See Also:Constant Field Values
   */
  static final String SERVLET_CONTEXT="javax.xml.ws.servlet.context";


  /**
   * Standard property: servlet request object. Type:
   * javax.servlet.http.HttpServletRequest See Also:Constant Field Values
   */
  static final String SERVLET_REQUEST="javax.xml.ws.servlet.request";


  /**
   * Standard property: servlet response object. Type:
   * javax.servlet.http.HttpServletResponse See Also:Constant Field Values
   */
  static final String SERVLET_RESPONSE="javax.xml.ws.servlet.response";


  /**
   * Standard property: input source for WSDL document. Type:
   * org.xml.sax.InputSource See Also:Constant Field Values
   */
  static final String WSDL_DESCRIPTION="javax.xml.ws.wsdl.description";


  /**
   * Standard property: name of wsdl interface (2.0) or port type (1.1). Type:
   * javax.xml.namespace.QName See Also:Constant Field Values
   */
  static final String WSDL_INTERFACE="javax.xml.ws.wsdl.interface";


  /**
   * Standard property: name of WSDL operation. Type: javax.xml.namespace.QName
   * See Also:Constant Field Values
   */
  static final String WSDL_OPERATION="javax.xml.ws.wsdl.operation";


  /**
   * Standard property: name of WSDL port. Type: javax.xml.namespace.QName See
   * Also:Constant Field Values
   */
  static final String WSDL_PORT="javax.xml.ws.wsdl.port";


  /**
   * Standard property: name of WSDL service. Type: javax.xml.namespace.QName
   * See Also:Constant Field Values
   */
  static final String WSDL_SERVICE="javax.xml.ws.wsdl.service";


  /**
   * Gets the scope of a property.
   */
  abstract Scope getScope(String name);


  /**
   * Sets the scope of a property.
   */
  abstract void setScope(String name, Scope scope);


  /**
   * Property scope. Properties scoped as APPLICATION are visible to handlers,
   * client applications and service endpoints; properties scoped as HANDLER
   * are only normally visible to handlers.
   */
  public static enum Scope {

      APPLICATION, HANDLER;

  }
}

