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

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpException;
import com.caucho.amqp.client.AmqpClientSenderLink;
import com.caucho.amqp.common.AmqpLink;
import com.caucho.amqp.common.AmqpSenderLink;
import com.caucho.amqp.common.AmqpSession;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.message.MessageSettleMode;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Amqp server connection
 */
public class AmqpConnectionWriter
{
  private static final Logger log
    = Logger.getLogger(AmqpConnectionWriter.class.getName());
  
  private WriteStream _os;
  private AmqpFrameWriter _fout;
  private AmqpWriter _aout;
  
  private boolean _isClosed;
  private boolean _isDisconnected;
  
  public AmqpConnectionWriter(WriteStream os)
  {
    _os = os;
    _fout = new AmqpFrameWriter(os);
    _aout = new AmqpWriter();
    _aout.initBase(_fout);
  }
  
  public void writeVersion(int code)
    throws IOException
  {
    WriteStream os = _os;
    
    os.write('A');
    os.write('M');
    os.write('Q');
    os.write('P');
    os.write(code); // sasl?
    os.write(0x01); // major
    os.write(0x00); // minor
    os.write(0x00); // version
    os.flush();
  }

  public void writeOpen()
  {
    FrameOpen open = new FrameOpen();
    open.setContainerId("ResinAmqpServer");
    
    sendFrame(open);
  }
  

  public void writeBegin()
  {
    FrameBegin begin = new FrameBegin();
    
    sendFrame(begin);
  }

  public void attachSender(FrameAttach clientAttach, 
                           AmqpLink session,
                           AmqpLink link)
    throws IOException
  {
    FrameAttach serverAttach = new FrameAttach();
    
    serverAttach.setName(clientAttach.getName());
    serverAttach.setHandle(link.getOutgoingHandle());
    
    serverAttach.setRole(Role.RECEIVER);
    
    sendFrame(serverAttach);
  }

  public void attachReceiver(FrameAttach clientAttach, 
                             AmqpLink session,
                             AmqpLink link)
    throws IOException
  {
    FrameAttach serverAttach = new FrameAttach();
    
    serverAttach.setName(link.getName());
    serverAttach.setHandle(link.getOutgoingHandle());
    
    serverAttach.setRole(Role.SENDER);
    
    sendFrame(serverAttach);
  }
  
  //
  // message transfer
  //

  /**
   * Transfers a message.
   */
  public void transfer(AmqpSession session,
                       AmqpSenderLink link,
                       long deliveryId,
                       MessageSettleMode settleMode, InputStream is)
  {
    try {
      boolean isSettled = (settleMode == MessageSettleMode.ALWAYS);

      _fout.startFrame(0, session.getOutgoingIndex());

      FrameTransfer transfer = new FrameTransfer();
      transfer.setHandle(link.getOutgoingHandle());

      transfer.setSettled(isSettled);
      transfer.setDeliveryId(deliveryId);

      transfer.write(_aout);

      _fout.write(is);

      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  public void sendDisposition(AmqpSession session,
                              long deliveryId, 
                              DeliveryState state)
  {
    FrameDisposition disposition = new FrameDisposition();
    disposition.setFirst(deliveryId);
    disposition.setLast(deliveryId);
    disposition.setState(state);
    
    sendFrame(disposition);
  }

  /**
   * @param handle
   */
  public void sendFlow(AmqpSession session, 
                       AmqpLink link,
                       long deliveryCount,
                       int credit)
  {
    FrameFlow flow = new FrameFlow();
    flow.setHandle(link.getOutgoingHandle());
    flow.setDeliveryCount(deliveryCount);
    flow.setLinkCredit(credit);
      
    sendFrame(session.getOutgoingIndex(), flow);
  }

  public void disconnect()
  {
    _isDisconnected = true;
  }

  public void sendFrame(AmqpAbstractFrame frame)
  {
    sendFrame(0, frame);
  }

  public void sendFrame(int channel, AmqpAbstractFrame frame)
  {
    try {
      _fout.startFrame(0, channel);
    
      frame.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  public void sendSaslChallenge()
    throws IOException
  {
    _fout.startFrame(1);
    
    SaslMechanisms mechanisms = new SaslMechanisms();
    mechanisms.write(_aout);
    
    _fout.finishFrame();
    
    _fout.startFrame(1);
    
    SaslOutcome outcome = new SaslOutcome();
    outcome.write(_aout);
    
    _fout.finishFrame();
  }
}
