/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.hemp.servlet;

import java.io.*;
import javax.servlet.*;

import com.caucho.hemp.*;
import com.caucho.hemp.broker.*;
import com.caucho.hessian.io.*;
import com.caucho.server.connection.*;
import com.caucho.vfs.*;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class HempHandler implements TcpConnectionHandler {
  private Broker _broker;
  private Hessian2StreamingInput _in;
  private Hessian2StreamingOutput _out;

  HempHandler(Broker broker, ReadStream is, WriteStream os)
  {
    _broker = broker;
    _in = new Hessian2StreamingInput(is);
    _out = new Hessian2StreamingOutput(os);
  }
  
  public boolean serviceRead(ReadStream is,
			     TcpConnectionController controller)
    throws IOException
  {
    Hessian2StreamingInput in = _in;

    if (in == null)
      return false;

    Packet packet = (Packet) in.readObject();

    if (packet == null) {
      controller.close();
      return false;
    }

    if (packet instanceof Message) {
      Message msg = (Message) packet;
      msg.setFrom("anonymous@localhost");
      
      _broker.sendMessage(msg);
    }
    else
      System.out.println("PACKET: " + packet);

    return true;
  }
  
  public boolean serviceWrite(WriteStream os,
			      TcpConnectionController controller)
    throws IOException
  {
    return false;
  }

  public void close()
  {
    Hessian2StreamingInput in = _in;
    _in = null;
    
    Hessian2StreamingOutput out = _out;
    _out = null;

    if (in != null)
      try { in.close(); } catch (IOException e) {}

    if (out != null)
      try { out.close(); } catch (IOException e) {}
  }
}
