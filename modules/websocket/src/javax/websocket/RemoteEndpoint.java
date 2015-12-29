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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * Interface for outbound websocket messages.
 */
public interface RemoteEndpoint
{
  boolean getBatchingAllowed();
  
  void setBatchingAllowed(boolean isAllowed)
    throws IOException;
  
  void flushBatch()
    throws IOException;
  
  void sendPing(ByteBuffer applicationData)
    throws IOException;
  
  void sendPong(ByteBuffer applicationData)
    throws IOException;
  
  interface Async extends RemoteEndpoint
  {
    void setSendTimeout(long timeout);
    
    long getSendTimeout();
    
    Future<Void> sendBinary(ByteBuffer data);
    
    void sendBinary(ByteBuffer data, SendHandler handler);
    
    Future<Void> sendObject(Object data);
    
    void sendObject(Object data, SendHandler handler);
    
    void sendText(String text, SendHandler handler);
    
    Future<Void> sendText(String text);
  }
  
  interface Basic extends RemoteEndpoint
  {
    OutputStream getSendStream()
      throws IOException;
    
    Writer getSendWriter()
      throws IOException;
    
    void sendBinary(ByteBuffer data)
      throws IOException;
    
    void sendBinary(ByteBuffer partial, boolean isLast)
        throws IOException;
    
    void sendObject(Object data)
      throws IOException, EncodeException;
    
    void sendText(String text)
      throws IOException;
    
    void sendText(String partial, boolean isLast)
      throws IOException;
  }
}
