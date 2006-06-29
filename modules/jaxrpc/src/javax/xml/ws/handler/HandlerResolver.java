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
import java.util.*;

/**
 * HandlerResolver is an interface implemented by an application to get control
 * over the handler chain set on proxy/dispatch objects at the time of their
 * creation. A HandlerResolver may be set on a Service using the
 * setHandlerResolver method. When the runtime invokes a HandlerResolver, it
 * will pass it a PortInfo object containing information about the port that
 * the proxy/dispatch object will be accessing. Since: JAX-WS 2.0 See
 * Also:Service.setHandlerResolver(javax.xml.ws.handler.HandlerResolver)
 */
public interface HandlerResolver {

  /**
   * Gets the handler chain for the specified port.
   */
  abstract List<Handler> getHandlerChain(PortInfo portInfo);

}

