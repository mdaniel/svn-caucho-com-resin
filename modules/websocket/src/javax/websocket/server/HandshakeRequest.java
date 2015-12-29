/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.websocket.server;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Information from the HTTP handshake to websocket.
 */
public interface HandshakeRequest
{
  static String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
  static String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
  static String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
  static String SEC_WEBSOCKET_VERSION= "Sec-WebSocket-Version";
  
  Map<String,List<String>> getHeaders();
  
  Object getHttpSession();
  
  Map<String,List<String>> getParameterMap();
  
  String getQueryString();
  
  URI getRequestURI();
  
  Principal getUserPrincipal();
  
  boolean isUserInRole(String role);
}
