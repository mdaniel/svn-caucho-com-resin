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
import javax.xml.namespace.*;

/**
 * The PortInfo interface is used by a HandlerResolver to query information
 * about the port it is being asked to create a handler chain for. This
 * interface is never implemented by an application, only by a JAX-WS
 * implementation. Since: JAX-WS 2.0
 */
public interface PortInfo {

  /**
   * Gets the URI identifying the binding used by the port being accessed.
   */
  abstract String getBindingID();


  /**
   * Gets the qualified name of the WSDL port being accessed.
   */
  abstract QName getPortName();


  /**
   * Gets the qualified name of the WSDL service name containing the port being
   * accessed.
   */
  abstract QName getServiceName();

}

