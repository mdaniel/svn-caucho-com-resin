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
import java.util.logging.Logger;

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
import com.caucho.amqp.io.SaslMechanisms;
import com.caucho.amqp.io.SaslOutcome;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.message.broker.MessageBroker;
import com.caucho.message.broker.BrokerSubscriber;
import com.caucho.message.broker.BrokerPublisher;
import com.caucho.message.broker.EnvironmentMessageBroker;
import com.caucho.message.broker.SubscriberMessageHandler;
import com.caucho.network.listen.ProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.WriteStream;

/**
 * Amqp server connection
 */
public class AmqpServerConnection implements ProtocolConnection, AmqpFrameHandler
{
  private static final Logger log
    = Logger.getLogger(AmqpServerConnection.class.getName());
  
  private AmqpProtocol _amqp;
  private SocketLink _link;
  
  private State _state = State.NEW;
  private boolean _isSasl;
  
  private AmqpSession []_sessions = new AmqpSession[1];
  
  private AmqpFrameReader _fin;
  private AmqpReader _ain;
  
  private AmqpFrameWriter _fout;
  private AmqpWriter _aout;
  
  private boolean _isClosed;
  private boolean _isDisconnected;
  
  AmqpServerConnection(AmqpProtocol amqp, SocketLink link)
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
  public void onStartConnection()
  {
    _state = State.NEW;
    
    _fin = new AmqpFrameReader();
    _fin.init(getReadStream());
    _ain = new AmqpReader();
    _ain.init(_fin);
    
    _fout = new AmqpFrameWriter(getWriteStream());
    _aout = new AmqpWriter();
    _aout.initBase(_fout);
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
      
      readOpen();
      writeOpen();
      
      _state = State.OPEN;
      return true;
      
    case OPEN:
      return readFrame();
      
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
    out.initBase(frameOs);
    
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
  
  private boolean readOpen()
    throws IOException
  {
    System.out.println("PREOPEN:");
    if (! _fin.startFrame()) {
      System.out.println("NOOPEN");
      return false;
    }
    
    FrameOpen open = _ain.readObject(FrameOpen.class);
    
    _fin.finishFrame();
    
    System.out.println("OPEN: "+ open);
    
    return true;
  }
  
  private void writeOpen()
    throws IOException
  {
    _fout.startFrame(0);
    
    FrameOpen open = new FrameOpen();
    open.setContainerId("ResinAmqpServer");
    
    open.write(_aout);
    
    _fout.finishFrame();
    _fout.flush();
  }
  
  void writeMessage(AmqpLink link, long messageId, InputStream is, long length)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    long deliveryId = session.addDelivery(link, messageId);
    
    FrameTransfer transfer = new FrameTransfer();
    transfer.setDeliveryId(deliveryId);
    transfer.setHandle(link.getHandle());
    
    _fout.startFrame(0);
    
    transfer.write(_aout);
    
    int ch;
    
    while ((ch = is.read()) >= 0) {
      _fout.write(ch);
    }
    
    _fout.finishFrame();
    _fout.flush();
    System.out.println("MSG:");
  }
  
  private boolean readFrame()
    throws IOException
  {
    if (! _fin.startFrame()) {
      return false;
    }
    
    AmqpAbstractFrame frame = _ain.readObject(AmqpAbstractFrame.class);
    
    System.out.println("SFRAME: " + frame);
        
    frame.invoke(_fin, this);
    
    _fin.finishFrame();
    
    return true;
  }
  
  @Override
  public void onBegin(FrameBegin clientBegin)
    throws IOException
  {
    _sessions[0] = new AmqpSession();
    
    FrameBegin serverBegin = new FrameBegin();
    
    sendFrame(serverBegin);
  }
  
  @Override
  public void onAttach(FrameAttach clientAttach)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    if (Role.SENDER.equals(clientAttach.getRole())) {
      attachSender(clientAttach, session);
    }
    else if (Role.RECEIVER.equals(clientAttach.getRole())) {
      attachReceiver(clientAttach, session);
    }
    else {
      throw new IllegalStateException("bad role");
    }
  }

  private void attachSender(FrameAttach clientAttach, 
                            AmqpSession session)
    throws IOException
  {
    String targetAddress = clientAttach.getTarget().getAddress();
    
    MessageBroker broker = EnvironmentMessageBroker.create();
    
    BrokerPublisher pub = broker.createSender(targetAddress);
    
    session.addLink(new AmqpLink(clientAttach, pub));
    
    FrameAttach serverAttach = new FrameAttach();
    
    serverAttach.setName(clientAttach.getName());
    serverAttach.setHandle(clientAttach.getHandle());
    
    if (clientAttach.getRole() == Role.SENDER) {
      serverAttach.setRole(Role.RECEIVER);
    }
    
    sendFrame(serverAttach);
  }

  private void attachReceiver(FrameAttach clientAttach, 
                              AmqpSession session)
    throws IOException
  {
    String sourceAddress = clientAttach.getSource().getAddress();
    
    MessageBroker broker = EnvironmentMessageBroker.create();
    
    AmqpReceiverLink link = new AmqpReceiverLink(this, clientAttach);
    
    BrokerSubscriber sub = broker.createReceiver(sourceAddress, link);
    
    link.setReceiver(sub);
    
    session.addLink(link);
    
    FrameAttach serverAttach = new FrameAttach();
    
    serverAttach.setName(clientAttach.getName());
    serverAttach.setHandle(clientAttach.getHandle());
    
    serverAttach.setRole(Role.SENDER);
    
    sendFrame(serverAttach);
  }
  
  @Override
  public void onTransfer(AmqpFrameReader fin, FrameTransfer transfer)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    int handle = transfer.getHandle();
    
    AmqpLink link = session.getLink(handle);
    
    int len = fin.getSize() - fin.getOffset();
    
    TempBuffer tBuf = TempBuffer.allocate();
    
    fin.read(tBuf.getBuffer(), 0, len);
    
    long xid = 0;
    link.write(xid, tBuf.getBuffer(), 0, len);
    // Link link = _links.get(handle);
    //System.out.println("MSG: " + transfer + " " + link);
  }
  
  @Override
  public void onDisposition(FrameDisposition disposition)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    DeliveryState state = disposition.getState();
    long xid = 0;
    
    if (state instanceof DeliveryAccepted) {
      session.accept(xid);
    }
    else if (state instanceof DeliveryRejected) {
      DeliveryRejected rejected = (DeliveryRejected) state;
      
      AmqpError error = rejected.getError();
      
      String message = null;
      
      if (error != null) {
        message = error.getCondition() + ": " + error.getDescription();
      }
      
      session.reject(xid,
                     disposition.getFirst(),
                     disposition.getLast(),
                     message);
    }
    else if (state instanceof DeliveryModified) {
      DeliveryModified modified = (DeliveryModified) state;
      
      session.modified(xid,
                       disposition.getFirst(),
                       disposition.getLast(),
                       modified.isDeliveryFailed(),
                       modified.isUndeliverableHere());
    }
    else if (state instanceof DeliveryReleased) {
      session.release(xid, disposition.getFirst(), disposition.getLast());
    }
    else {
      System.out.println("UNKNOWN");
    }
  }
  
  @Override
  public void onFlow(FrameFlow flow)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    
    session.onFlow(flow);
  }

  @Override
  public void onDetach(FrameDetach clientDetach)
    throws IOException
  {
    FrameDetach serverDetach = new FrameDetach();
    
    serverDetach.setHandle(clientDetach.getHandle());
    
    System.out.println("STACCH: " + clientDetach);
    
    sendFrame(serverDetach);
  }

  @Override
  public void onEnd(FrameEnd clientEnd)
    throws IOException
  {
    AmqpSession session = _sessions[0];
    _sessions[0] = null;

    System.out.println("SERVERE:" + clientEnd);
    if (session != null) {
      FrameEnd serverEnd = new FrameEnd();
    
      sendFrame(serverEnd);
    }
  }
  
  @Override
  public void onClose(FrameClose clientClose)
    throws IOException
  {
    try {
      System.out.println("SERVERE:" + clientClose);
    
      FrameClose serverClose = new FrameClose();
    
      sendFrame(serverClose);
    } finally {
      disconnect();
    }
  }
  
  private void disconnect()
  {
    _isDisconnected = true;
  }

  private void sendFrame(AmqpAbstractFrame frame)
    throws IOException
  {
    _fout.startFrame(0);
    
    frame.write(_aout);
    
    _fout.finishFrame();
    _fout.flush();
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
  
  private enum State {
    NEW,
    VERSION,
    OPEN;
  }
}
