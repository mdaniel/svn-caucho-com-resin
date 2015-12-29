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

/**
 * Represents an encoder of a websocket message.
 */
public interface Encoder
{
  void init(EndpointConfig config);
  
  void destroy();
  
  public static class Adapter implements Encoder
  {
    @Override
    public void init(EndpointConfig config)
    {
    }
    
    @Override
    public void destroy()
    {
    }
  }
  
  interface Binary<T> extends Encoder
  {
    ByteBuffer encode(T object)
      throws EncodeException;
  }
  
  interface BinaryStream<T> extends Encoder
  {
    void encode(T object, OutputStream os)
      throws IOException, EncodeException;
  }
  
  interface Text<T> extends Encoder
  {
    String encode(T object)
      throws EncodeException;
  }
  
  interface TextStream<T> extends Encoder
  {
    void encode(T object, Writer out)
      throws EncodeException, IOException;
  }
}
