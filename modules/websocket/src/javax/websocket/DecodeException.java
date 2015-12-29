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

import java.nio.ByteBuffer;

/**
 * Represents an encoder of a websocket message.
 */
@SuppressWarnings("serial")
public class DecodeException extends RuntimeException
{
  private ByteBuffer _bytes;
  private String _text;

  public DecodeException()
  {
  }
  
  public DecodeException(ByteBuffer bb, String msg)
  {
    super(msg);
    
    _bytes = bb;
  }
  
  public DecodeException(ByteBuffer bb, String msg, Throwable e)
  {
    super(msg, e);
    
    _bytes = bb;
  }
  
  public DecodeException(String data, String msg)
  {
    super(msg);
    
    _text = data;
  }
  
  public DecodeException(String data, String msg, Throwable e)
  {
    super(msg, e);
    
    _text = data;
  }
  
  public ByteBuffer getBytes()
  {
    return _bytes;
  }
  
  public String getText()
  {
    return _text;
  }
}
