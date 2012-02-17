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

package com.caucho.mqueue.amqp;

import java.io.IOException;
import java.util.logging.Logger;

import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class AmqpConnection implements ProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(AmqpConnection.class.getName());
  
  private AmqpProtocol _amqp;
  private SocketLink _link;
  
  private State _state = State.NEW;
  private boolean _isSasl;
  
  AmqpConnection(AmqpProtocol amqp, SocketLink link)
  {
    _amqp = amqp;
    _link = link;
  }
  
  @Override
  public String getProtocolRequestURL()
  {
    return "stomp:";
  }
  
  @Override
  public void init()
  {
  }
  
  SocketLink getLink()
  {
    return _link;
  }
  
  ReadStream getReadStream()
  {
    return _link.getReadStream();
  }
  
  WriteStream getWriteStream()
  {
    return _link.getWriteStream();
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    ReadStream is = _link.getReadStream();
    
    System.out.println("REQ1:");
    
    switch (_state) {
    case NEW:
      if (! readVersion(is)) {
        return false;
      }
      
      System.out.println("SASL: " + _isSasl);
      
      if (_isSasl) {
        System.out.println("SASL:");
        try {
          sendSaslChallenge();
          System.out.println("SENDED:");
          
          readVersion(is);
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        int ch;
        while ((ch = is.read()) >= 0) {
          System.out.println("CH: " + Integer.toHexString(ch) + " " + (char) ch);
        }
        System.out.println("DONE-CH: " + Integer.toHexString(ch));
      }
      break;
    default:
      System.out.println("UNKNOWN STATE: " + _state);
      break;
    }
    System.out.println("REQ:");

    return false;
  }
  
  private boolean readVersion(ReadStream is)
    throws IOException
  {
    if (is.read() != 'A'
        || is.read() != 'M'
        || is.read() != 'Q'
        || is.read() != 'P') {
      System.out.println("ILLEGAL_HEADER:");
      throw new IOException();
    }
    
    int code = is.read();
    
    switch (code) {
    case 0x00:
      _isSasl = false;
      break;
    case 0x03:
      _isSasl = true;
      break;
    default:
      System.out.println("BAD_CODE: " + code);
      throw new IOException("Unknown code");
    }
    
    int major = is.read() & 0xff;
    int minor = is.read() & 0xff;
    int version = is.read() & 0xff;
    
    if (major != 0x01 || minor != 0x00 || version != 0x00) {
      System.out.println("UNKNOWN_VERSION");
      throw new IOException();
    }
    
    WriteStream os = _link.getWriteStream();
    
    os.write('A');
    os.write('M');
    os.write('Q');
    os.write('P');
    os.write(code); // sasl?
    os.write(0x01); // major
    os.write(0x00); // minor
    os.write(0x00); // version
    os.flush();
    
    System.out.println("VERSION:");
    
    return true;
  }
  
  private void sendSaslChallenge()
    throws IOException
  {
    WriteStream os = _link.getWriteStream();
    
    AmqpFrameWriter frameOs = new AmqpFrameWriter(os);
    
    frameOs.startFrame(1);
    
    AmqpWriter out = new AmqpWriter();
    out.init(frameOs);
    
    SaslMechanisms mechanisms = new SaslMechanisms();
    mechanisms.write(out);
    
    frameOs.finishFrame();
    os.flush();
    
    System.out.println("SASL_DONE:");
    
    frameOs.startFrame(1);
    out.init(frameOs);
    
    SaslOutcome outcome = new SaslOutcome();
    outcome.write(out);
    
    frameOs.finishFrame();
    os.flush();
    
    System.out.println("SASL_DONE2:");
  }
  
  @Override
  public boolean handleResume() throws IOException
  {
    return false;
  }

  @Override
  public boolean isWaitForRead()
  {
    return false;
  }

  @Override
  public void onCloseConnection()
  {
    System.out.println("CLOSE");
  }

  @Override
  public void onStartConnection()
  {
    _state = State.NEW;
  }
  
  private enum State {
    NEW,
    VERSION;
  }
}
