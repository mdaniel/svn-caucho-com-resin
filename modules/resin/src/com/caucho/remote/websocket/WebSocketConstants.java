/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.remote.websocket;

/**
 * WebSocketOutputStream writes a single WebSocket packet.
 *
 * <code><pre>
 * 0x84 0x8X 0x8X 0x0X binarydata
 * </pre></code>
 */
public interface WebSocketConstants {
  public static final int FLAG_FIN = 0x80;
  
  public static final int OP_CONT = 0x00;
  public static final int OP_CLOSE = 0x01;
  public static final int OP_PING = 0x02;
  public static final int OP_PONG = 0x03;
  public static final int OP_TEXT = 0x04;
  public static final int OP_BINARY = 0x05;
  
  public static final int OP_EXT = 0x0E;
  public static final int OP_HELLO = 0x0F;
}
