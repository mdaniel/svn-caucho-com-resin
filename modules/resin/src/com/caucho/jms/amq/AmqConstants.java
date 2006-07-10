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

package com.caucho.jms.amq;

import java.io.*;

import java.util.logging.*;

import com.caucho.vfs.*;

import com.caucho.server.port.ServerRequest;
import com.caucho.server.connection.Connection;

/**
 * Constants for AMQ.
 */
public interface AmqConstants {
  public static final int FRAME_METHOD = 1;
  public static final int FRAME_HEADER = 2;
  public static final int FRAME_BODY = 3;
  public static final int FRAME_OOB_METHOD = 4;
  public static final int FRAME_OOB_HEADER = 5;
  public static final int FRAME_OOB_BODY = 6;
  public static final int FRAME_TRACE = 7;
  public static final int FRAME_HEARTBEAT = 8;
  
  public static final int CYCLE_TBD = 0;

  public static final int FRAME_END = 0xd3;
  
  public static final int CLASS_CONNECTION = 10;
  
  public static final int ID_CONNECTION_START = 10;
  public static final int ID_CONNECTION_START_OK = 20;
  public static final int ID_CONNECTION_SECURE = 30;
  public static final int ID_CONNECTION_SECURE_OK = 40;
  public static final int ID_CONNECTION_TUNE = 50;
  public static final int ID_CONNECTION_TUNE_OK = 60;
  public static final int ID_CONNECTION_OPEN = 70;
  public static final int ID_CONNECTION_OPEN_OK = 80;
  public static final int ID_CONNECTION_REDIRECT = 90;
  public static final int ID_CONNECTION_CLOSE = 100;
  public static final int ID_CONNECTION_CLOSE_OK = 110;
  
  public static final int CLASS_CHANNEL = 20;
  
  public static final int ID_CHANNEL_OPEN = 10;
  public static final int ID_CHANNEL_OPEN_OK = 20;
  public static final int ID_CHANNEL_FLOW = 30;
  public static final int ID_CHANNEL_FLOW_OK = 40;
  public static final int ID_CHANNEL_ALERT = 50;
  public static final int ID_CHANNEL_CLOSE = 60;
  public static final int ID_CHANNEL_CLOSE_OK = 70;
  
  public static final int CLASS_ACCESS = 30;
  public static final int CLASS_EXCHANGE = 40;
  public static final int CLASS_QUEUE = 50;
  
  public static final int ID_QUEUE_DECLARE = 10;
  public static final int ID_QUEUE_DECLARE_OK = 20;
  
  public static final int CLASS_BASIC = 60;
  
  public static final int ID_BASIC_PUBLISH = 50;
  
  public static final int CLASS_FILE = 70;
  public static final int CLASS_STREAM = 80;
  public static final int CLASS_TX = 90;
  public static final int CLASS_DTX = 100;
  public static final int CLASS_TUNNEL = 110;
  public static final int CLASS_TEST = 120;
  public static final int CLASS_CLUSTER = 61440;
}
