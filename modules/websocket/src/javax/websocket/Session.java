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

package javax.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a websocket session.
 */
public interface Session extends Closeable 
{
  String getId();
  
  WebSocketContainer getContainer();
  
  boolean isOpen();
  
  boolean isSecure();
  
  //
  // timeouts and limits
  //

  int getMaxBinaryMessageBufferSize();
  
  long getMaxIdleTimeout();
  
  int getMaxTextMessageBufferSize();
  
  void setMaxBinaryMessageBufferSize(int length);
  
  void setMaxIdleTimeout(long timeout);
  
  void setMaxTextMessageBufferSize(int length);
  
  //
  // handshake values
  //
  
  List<Extension> getNegotiatedExtensions();
  
  String getNegotiatedSubprotocol();
  
  Set<Session> getOpenSessions();
  
  Map<String,String> getPathParameters();
  
  String getProtocolVersion();
  
  String getQueryString();
  
  Map<String,List<String>> getRequestParameterMap();
  
  URI getRequestURI();
  
  Principal getUserPrincipal();
  
  Map<String,Object> getUserProperties();
  
  
  //
  // message handlers
  //
  
  void addMessageHandler(MessageHandler handler);
  
  Set<MessageHandler> getMessageHandlers();
  
  void removeMessageHandler(MessageHandler handler);
  
  //
  // message senders
  //
  
  RemoteEndpoint.Async getAsyncRemote();
  
  RemoteEndpoint.Basic getBasicRemote();
  
  void close()
    throws IOException;
  
  void close(CloseReason reason)
    throws IOException;
}
