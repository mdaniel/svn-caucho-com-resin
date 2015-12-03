/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.amqp.io;

import javax.annotation.PostConstruct;

import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;

/**
 * Custom serialization for the cache
 */
public interface AmqpConstants {
  // frame conn/session/link
  public static final int FT_CONN_OPEN = 0x10;
  
  public static final int FT_SESSION_OPEN = 0x11;
  
  public static final int FT_LINK_ATTACH = 0x12;
  public static final int FT_LINK_FLOW = 0x13;
  
  public static final int FT_MESSAGE_TRANSFER = 0x14;
  public static final int FT_MESSAGE_DISPOSITION = 0x15;
  
  public static final int FT_LINK_DETACH = 0x16;
  public static final int FT_SESSION_END = 0x17;
  public static final int FT_CONN_CLOSE = 0x18;
  
  public static final int FT_ERROR = 0x1d;
  
  // message receiving
  public static final int ST_MESSAGE_RECEIVED = 0x23;
  public static final int ST_MESSAGE_ACCEPTED = 0x24;
  public static final int ST_MESSAGE_REJECTED = 0x25;
  public static final int ST_MESSAGE_RELEASED = 0x26;
  public static final int ST_MESSAGE_MODIFIED = 0x27;
  
  // message source
  public static final int ST_MESSAGE_SOURCE = 0x28;
  public static final int ST_MESSAGE_TARGET = 0x29;
  
  public static final int ST_NODE_DELETE_ON_CLOSE = 0x2b;
  public static final int ST_NODE_DELETE_ON_NO_LINK = 0x2c;
  public static final int ST_NODE_DELETE_ON_NO_MESSAGES = 0x2d;
  public static final int ST_NODE_DELETE_ON_NO_LINK_OR_MESSAGES = 0x2e;
  
  // xa
  public static final int ST_XA_COORDINATOR = 0x30;
  public static final int ST_XA_DECLARE = 0x31;
  public static final int ST_XA_DISCHARGE = 0x32;
  public static final int ST_XA_DECLARED = 0x33;
  public static final int ST_XA_STATE = 0x34;
  
  // security
  public static final int ST_SASL_MECHANISMS = 0x40;
  public static final int ST_SASL_INIT = 0x41;
  public static final int ST_SASL_CHALLENGE = 0x42;
  public static final int ST_SASL_RESPONSE = 0x43;
  public static final int ST_SASL_OUTCOME = 0x44;
  
  // message sending
  public static final int ST_MESSAGE_HEADER = 0x70;
  public static final int ST_MESSAGE_DELIVERY_ANN = 0x71;
  public static final int ST_MESSAGE_ANN = 0x72;
  public static final int ST_MESSAGE_PROPERTIES = 0x73;
  public static final int ST_MESSAGE_APP_PROPERTIES = 0x74;
  public static final int ST_MESSAGE_DATA = 0x75;
  public static final int ST_MESSAGE_SEQUENCE = 0x76;
  public static final int ST_MESSAGE_VALUE = 0x77;
  public static final int ST_MESSAGE_FOOTER = 0x78;
  
  public static final int MIN_MAX_FRAME_SIZE = 512;
  
  // encoding
  // descriptor
  public static final int E_DESCRIPTOR = 0x00;
  
  // zero-length values
  public static final int E_NULL = 0x40;
  public static final int E_TRUE = 0x41;
  public static final int E_FALSE = 0x42;
  public static final int E_I0 = 0x43;
  public static final int E_L0 = 0x44;
  public static final int E_LIST_0 = 0x45;
  
  // one-length values
  public static final int E_UBYTE_1 = 0x50;
  public static final int E_BYTE_1 = 0x51;
  public static final int E_UINT_1 = 0x52;
  public static final int E_ULONG_1 = 0x53;
  public static final int E_INT_1 = 0x54;
  public static final int E_LONG_1 = 0x55;
  public static final int E_BOOLEAN_1 = 0x56;
  
  // 2-length values
  public static final int E_USHORT = 0x60;
  public static final int E_SHORT = 0x61;
  
  // 4-length values
  public static final int E_UINT_4 = 0x70;
  public static final int E_INT_4 = 0x71;
  public static final int E_FLOAT = 0x72;
  public static final int E_CHAR = 0x73;
  public static final int E_DECIMAL_4 = 0x74;
  
  // 8-length values;
  public static final int E_ULONG_8 = 0x80;
  public static final int E_LONG_8 = 0x81;
  public static final int E_DOUBLE = 0x82;
  public static final int E_TIMESTAMP = 0x83;
  public static final int E_DECIMAL_8 = 0x84;
  
  // 16-length values
  public static final int E_DECIMAL_16 = 0x94;
  public static final int E_UUID = 0x98;
  
  // variable width
  
  public static final int E_BIN_1 = 0xa0;
  public static final int E_UTF8_1 = 0xa1;
  public static final int E_SYMBOL_1 = 0xa3;
  
  public static final int E_BIN_4 = 0xb0;
  public static final int E_UTF8_4 = 0xb1;
  public static final int E_SYMBOL_4 = 0xb3;
  
  // compound
  
  public static final int E_LIST_1 = 0xc0;
  public static final int E_MAP_1 = 0xc1;
  
  public static final int E_LIST_4 = 0xd0;
  public static final int E_MAP_4 = 0xd1;
  
  // array
  
  public static final int E_ARRAY_1 = 0xe0;

  public static final int E_ARRAY_4 = 0xf0;
}
