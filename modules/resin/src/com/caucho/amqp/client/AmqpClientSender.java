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

package com.caucho.amqp.client;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.io.AmqpAbstractComposite;
import com.caucho.amqp.io.AmqpAbstractPacket;
import com.caucho.amqp.io.AmqpReceiver;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.util.L10N;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.QSocketWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
public class AmqpClientSender {
  private static final Logger log
    = Logger.getLogger(AmqpClientSender.class.getName());
  
  private AmqpClientImpl _client;
  
  private String _address;
  private int _handle;
  
  AmqpClientSender(AmqpClientImpl client,
                   String address,
                   int handle)
  {
    _client = client;
    _address = address;
    _handle = handle;
  }
  
  public void send(byte []buffer)
  {
    _client.transmit(_handle, buffer, 0, buffer.length);
  }
  
  public void close()
  {
    _client.closeSender(_handle);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _handle + "," + _address + "]";
  }
}
