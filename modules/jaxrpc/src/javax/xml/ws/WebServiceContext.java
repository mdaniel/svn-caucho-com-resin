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
import javax.xml.ws.handler.*;
import java.security.*;

/**
 * A WebServiceContext makes it possible for a web service endpoint
 * implementation class to access message context and security information
 * relative to a request being served. Typically a WebServiceContext is
 * injected into an endpoint implementation class using the Resource
 * annotation. Since: JAX-WS 2.0 See Also:Resource
 */
public interface WebServiceContext {

  /**
   * Returns the MessageContext for the request being served at the time this
   * method is called. Only properties with APPLICATION scope will be visible
   * to the application.
   */
  abstract MessageContext getMessageContext();


  /**
   * Returns the Principal that identifies the sender of the request currently
   * being serviced. If the sender has not been authenticated, the method
   * returns null.
   */
  abstract Principal getUserPrincipal();


  /**
   * Returns a boolean indicating whether the authenticated user is included in
   * the specified logical role. If the user has not been authenticated, the
   * method returns false.
   */
  abstract boolean isUserInRole(String role);

}

