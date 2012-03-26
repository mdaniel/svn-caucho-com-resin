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

package com.caucho.amqp.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.common.AmqpConnectionHandler;
import com.caucho.amqp.common.AmqpLink;
import com.caucho.amqp.common.AmqpLinkFactory;
import com.caucho.amqp.common.AmqpReceiverLink;
import com.caucho.amqp.common.AmqpSenderLink;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.DeliveryAccepted;
import com.caucho.amqp.io.DeliveryModified;
import com.caucho.amqp.io.DeliveryRejected;
import com.caucho.amqp.io.DeliveryReleased;
import com.caucho.amqp.io.DeliveryState;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.FrameClose;
import com.caucho.amqp.io.FrameDetach;
import com.caucho.amqp.io.FrameDisposition;
import com.caucho.amqp.io.FrameEnd;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.FrameTransfer;
import com.caucho.amqp.io.MessageHeader;
import com.caucho.amqp.io.SaslMechanisms;
import com.caucho.amqp.io.SaslOutcome;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.message.DistributionMode;
import com.caucho.message.SettleMode;
import com.caucho.message.broker.MessageBroker;
import com.caucho.message.broker.BrokerReceiver;
import com.caucho.message.broker.BrokerSender;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.broker.ReceiverMessageHandler;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.CurrentTime;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

/**
 * Amqp server connection
 */
public class AmqpServerConnection extends AbstractProtocolConnection
{
  private static final Logger log
    = Logger.getLogger(AmqpServerConnection.class.getName());
  
  private AmqpProtocol _amqp;
  private SocketLink _link;
  
  private State _state = State.NEW;
  private boolean _isSasl;
  
  private ServerLinkFactory _linkFactory;
  private AmqpConnectionHandler _handler;
  
  AmqpServerConnection(AmqpProtocol amqp, SocketLink link)
  {
    _amqp = amqp;
    _link = link;
  }
  
  @Override
  public String getProtocolRequestURL()
  {
    return "amqp:";
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
  public void onStartConnection()
  {
    _state = State.NEW;
    
    _linkFactory = new ServerLinkFactory();
  }

  @Override
  public boolean handleRequest() throws IOException
  {
    switch (_state) {
    case NEW:
      _handler = new AmqpConnectionHandler(_linkFactory,
                                           getReadStream(),
                                           getWriteStream());
      
      if (! _handler.getReader().readVersion()) {
        return false;
      }

      if (_isSasl) {
        System.out.println("SASL:");
        try {
          _handler.getWriter().sendSaslChallenge();
          System.out.println("SENDED:");
          
          _handler.getReader().readVersion();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      
      _handler.getWriter().writeVersion(0);
      
      _handler.getReader().readOpen();
      _handler.getWriter().writeOpen();
      
      _state = State.OPEN;
      return true;
      
    case OPEN:
      return _handler.getReader().readFrame();
      
    default:
      System.out.println("UNKNOWN STATE: " + _state);
      break;
    }
    System.out.println("REQ:");

    return false;
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
    AmqpConnectionHandler handler = _handler;
    _handler = null;
    _linkFactory = null;

    if (handler != null) {
      handler.closeConnection();
    }
  }
  
  private enum State {
    NEW,
    VERSION,
    OPEN;
  }
  
  class ServerLinkFactory implements AmqpLinkFactory {
    @Override
    public AmqpReceiverLink createReceiverLink(String name,
                                               String address,
                                               Map<String,Object> targetProperties)
    {
      return _amqp.createReceiverLink(name, address, targetProperties);
    }

    @Override
    public AmqpSenderLink createSenderLink(String name, 
                                           String address,
                                           DistributionMode distMode,
                                           SettleMode settleMode,
                                           Map<String,Object> sourceProperties)
    {
      return _amqp.createSenderLink(name, address, distMode, settleMode,
                                    sourceProperties);
    }
  }
}
