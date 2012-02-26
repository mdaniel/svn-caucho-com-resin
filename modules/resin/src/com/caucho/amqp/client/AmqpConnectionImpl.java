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
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amqp.AmqpConnection;
import com.caucho.amqp.AmqpException;
import com.caucho.amqp.AmqpReceiver;
import com.caucho.amqp.io.AmqpAbstractComposite;
import com.caucho.amqp.io.AmqpAbstractFrame;
import com.caucho.amqp.io.AmqpAbstractPacket;
import com.caucho.amqp.io.DeliveryAccepted;
import com.caucho.amqp.io.DeliveryModified;
import com.caucho.amqp.io.DeliveryRejected;
import com.caucho.amqp.io.DeliveryReleased;
import com.caucho.amqp.io.FrameAttach;
import com.caucho.amqp.io.FrameClose;
import com.caucho.amqp.io.FrameDetach;
import com.caucho.amqp.io.FrameDisposition;
import com.caucho.amqp.io.FrameEnd;
import com.caucho.amqp.io.AmqpFrameHandler;
import com.caucho.amqp.io.FrameFlow;
import com.caucho.amqp.io.FrameOpen;
import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpError;
import com.caucho.amqp.io.AmqpFrameReader;
import com.caucho.amqp.io.AmqpFrameWriter;
import com.caucho.amqp.io.AmqpReader;
import com.caucho.amqp.io.FrameBegin;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.FrameTransfer;
import com.caucho.amqp.io.LinkSource;
import com.caucho.amqp.io.LinkTarget;
import com.caucho.amqp.io.FrameAttach.Role;
import com.caucho.amqp.server.AmqpSession;
import com.caucho.env.thread.ThreadPool;
import com.caucho.util.L10N;
import com.caucho.vfs.QSocket;
import com.caucho.vfs.QSocketWrapper;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;


/**
 * AMQP client
 */
public class AmqpConnectionImpl
  implements AmqpConnection, AmqpFrameHandler
{
  private static final L10N L = new L10N(AmqpConnectionImpl.class);
  
  private static final Logger log
    = Logger.getLogger(AmqpConnectionImpl.class.getName());
  
  private String _hostname;
  private int _port;
  
  private QSocketWrapper _s;
  private ReadStream _is;
  private WriteStream _os;
  
  private AmqpSession[]_sessions = new AmqpSession[1];
  private ArrayList<Link> _links = new ArrayList<Link>();
  
  private AmqpFrameWriter _fout = new AmqpFrameWriter();
  private AmqpWriter _aout = new AmqpWriter();
  
  private AmqpFrameReader _fin;
  private AmqpReader _ain;
  
  private AmqpClientFrameReader _reader;
  
  private int _handleCounter = 1;
  
  private final AtomicBoolean _isClosed = new AtomicBoolean();
  private boolean _isDisconnected;
  
  public AmqpConnectionImpl()
  {
  }
  
  public AmqpConnectionImpl(String hostname, int port)
  {
    _hostname = hostname;
    _port = port;
  }
  
  public void setHostname(String hostname)
  {
    _hostname = hostname;
  }
  
  public void setPort(int port)
  {
    _port = port;
  }

  public boolean isDisconnected()
  {
    return _isDisconnected;
  }
  
  public void connect()
  {
    boolean isValid = false;
    
    try {
      connectSocket();
      
      writeVersion();
      writeOpen();
      writeBegin();
      
      _os.flush();
      
      readVersion();
      
      System.out.println("OK-VERSION");
      readOpen();
      System.out.println("OK-OPEN");
      
      _reader = new AmqpClientFrameReader(this, _fin, _ain);
      
      ThreadPool.getCurrent().schedule(_reader);
      
      isValid = true;
    } catch (IOException e) {
      throw new AmqpException(e);
    } finally {
      if (! isValid) {
        disconnect();
      }
    }
  }
  
  public AmqpClientSenderFactory createSenderFactory()
  {
    return new AmqpClientSenderFactory(this);
  }
  
  @SuppressWarnings("unchecked")
  public AmqpClientSender<?> createSender(String address)
  {
    return (AmqpClientSender) createSenderFactory().setAddress(address).build();
  }
  
  AmqpClientSender<?> buildSender(AmqpClientSenderFactory factory)
  {
    int handle = _handleCounter++;
    
    String address = factory.getAddress();
    
    FrameAttach attach = new FrameAttach();
    attach.setName("client-" + address);
    attach.setHandle(handle);
    attach.setRole(Role.SENDER);
    
    LinkSource source = new LinkSource();
    attach.setSource(source);
    
    LinkTarget target = new LinkTarget();
    target.setAddress(address);
    attach.setTarget(target);
    
    while (_links.size() <= handle) {
      _links.add(null);
    }
    
    _links.set(handle, new Link(attach));
    
    try {
      writeFrame(attach);
    
      return new AmqpClientSender(this, factory, attach.getHandle());
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  @Override
  public AmqpClientReceiverFactory createReceiverFactory()
  {
    return new AmqpClientReceiverFactory(this);
  }
  
  @Override
  public AmqpReceiver createReceiver(String address)
  {
    AmqpClientReceiverFactory factory = createReceiverFactory();
    factory.setAddress(address);
    
    return factory.build();
  }
  
  AmqpClientReceiver buildReceiver(AmqpClientReceiverFactory factory)
  {
    int handle = _handleCounter++;
    String address = factory.getAddress();
    
    FrameAttach attach = new FrameAttach();
    attach.setName("client-" + address);
    attach.setHandle(handle);
    attach.setRole(Role.RECEIVER);
    
    LinkSource source = new LinkSource();
    source.setAddress(address);
    attach.setSource(source);
    
    LinkTarget target = new LinkTarget();
    attach.setTarget(target);
    
    while (_links.size() <= handle) {
      _links.add(null);
    }
    
    Link link = new Link(attach);
    
    _links.set(handle, link);
    
    try {
      writeFrame(attach);
      
      AmqpClientReceiver receiver;
    
      receiver = new AmqpClientReceiver(this, factory, attach.getHandle());
      
      link.setReceiver(receiver);
      
      return receiver;
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  void closeSender(int handle)
  {
    Link link = _links.set(handle, null);
    System.out.println("CLOSE: " + link);
    
    if (link != null) {
      FrameDetach detach = new FrameDetach();
      detach.setHandle(handle);
      
      try {
        writeFrame(detach);
      } catch (IOException e) {
        throw new AmqpException(e);
      }
    }
  }
  
  void closeReceiver(int handle)
  {
    Link link = _links.set(handle, null);
    System.out.println("CLOSE2: " + link);
    
    if (link != null) {
      FrameDetach detach = new FrameDetach();
      detach.setHandle(handle);
      
      try {
        writeFrame(detach);
      } catch (IOException e) {
        throw new AmqpException(e);
      }
    }
  }
  
  private void connectSocket()
    throws IOException
  {
    Socket socket = new Socket(_hostname, _port);
    
    _s = new QSocketWrapper(socket);
    
    _is = new ReadStream(_s.getStream());
    _os = new WriteStream(_s.getStream());
    
    _fout.init(_os);
    _aout.initBase(_fout);
    
    _fin = new AmqpFrameReader();
    _fin.init(_is);
    _ain = new AmqpReader();
    _ain.init(_fin);
  }
  
  private void writeVersion()
    throws IOException
  {
    _os.write('A');
    _os.write('M');
    _os.write('Q');
    _os.write('P');
    _os.write(0); // sasl is 3
    _os.write(1);
    _os.write(0);
    _os.write(0);
  }
  
  private void readVersion()
    throws IOException
  {
    int h0 = _is.read(); 
    int h1 = _is.read(); 
    int h2 = _is.read(); 
    int h3 = _is.read();
      
    int type = _is.read();
    int major = _is.read();
    int minor = _is.read();
    int version = _is.read();
    
    if (version < 0) {
      throw new EOFException(L.l("Unexpected EOF"));
    }
    
    if (h0 != 'A' || h1 != 'M' || h2 != 'Q' || h3 != 'P') {
      throw new IOException(L.l("Unknown protocol '{0}'",
                                "" + (char) h0 + (char) h1
                                + (char) h2 + (char) h3));
                            
    }
    
    if (type != 0) {
      throw new IOException(L.l("Unknown type '{0}'", type));
    }
    
    if (major != 1 || minor != 0 || version != 0) {
      throw new IOException(L.l("Unknown version '{0}.{1}.{2}'",
                                major, minor, version));
                                
    }
  }
  
  private void writeOpen()
    throws IOException
  {
    FrameOpen open = new FrameOpen();
    
    _fout.startFrame(0);
    open.write(_aout);
    _fout.finishFrame();
  }
  
  private void writeBegin()
    throws IOException
  {
    FrameBegin begin = new FrameBegin();
  
    writeFrame(begin);
  }
  
  private void writeFrame(AmqpAbstractFrame frame)
    throws IOException
  {
    _fout.startFrame(0);
    frame.write(_aout);
    _fout.finishFrame();
    
    _fout.flush();
  }

  private void readOpen()
    throws IOException
  {
    AmqpAbstractPacket value;
    
    if (! _fin.startFrame()) {
      throw new AmqpException(L.l("unexpected eof"));
    }
    
    value = _ain.readObject(AmqpAbstractPacket.class);
    
    _fin.finishFrame();
    
    if (! (value instanceof FrameOpen)) {
      throw new AmqpException(L.l("unexpected packet {0}", value));
    }
    
    FrameOpen open = (FrameOpen) value;

    System.out.println("CLIENT_OPEN: " + open);
  }
  
  void transmit(int handle, byte []buffer, int offset, int length)
  {
    try {
      _fout.startFrame(0);
    
      FrameTransfer transfer = new FrameTransfer();
      transfer.setHandle(handle);
      transfer.setDeliveryId(2);
      
      transfer.write(_aout);

      _fout.write(buffer, offset, length);

      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  void transmit(int handle, InputStream is)
    throws IOException
  {
    try {
      _fout.startFrame(0);
    
      FrameTransfer transfer = new FrameTransfer();
      transfer.setHandle(handle);
      transfer.setDeliveryId(2);
      
      transfer.write(_aout);

      _fout.write(is);

      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  /**
   * @param handle
   */
  public void flow(int handle, long deliveryCount, int credit)
  {
    try {
      _fout.startFrame(0);
    
      FrameFlow flow = new FrameFlow();
      flow.setHandle(handle);
      flow.setDeliveryCount(deliveryCount);
      flow.setLinkCredit(credit);
      
      flow.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  /**
   * @param handle
   */
  public void dispositionAccept(int handle)
  {
    try {
      _fout.startFrame(0);
    
      FrameDisposition disposition = new FrameDisposition();
      disposition.setState(DeliveryAccepted.VALUE);
      
      disposition.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  /**
   * @param handle
   */
  public void dispositionReject(int handle,
                                String errorMessage)
  {
    try {
      _fout.startFrame(0);
    
      FrameDisposition disposition = new FrameDisposition();
      
      DeliveryRejected rejected = new DeliveryRejected();
      
      if (errorMessage != null) {
        AmqpError error = new AmqpError();

        error.setCondition("rejected");
        error.setDescription(errorMessage);
        
        rejected.setError(error);
      }
      
      disposition.setState(rejected);
      
      disposition.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  /**
   * @param handle
   */
  public void dispositionModified(int handle,
                                  boolean isFailed,
                                  boolean isUndeliverableHere)
  {
    try {
      _fout.startFrame(0);
    
      FrameDisposition disposition = new FrameDisposition();
      
      DeliveryModified modified = new DeliveryModified();
      
      modified.setDeliveryFailed(isFailed);
      modified.setUndeliverableHere(isUndeliverableHere);
      
      disposition.setState(modified);
      
      disposition.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }

  /**
   * @param handle
   */
  public void dispositionRelease(int handle)
  {
    try {
      _fout.startFrame(0);
    
      FrameDisposition disposition = new FrameDisposition();
      disposition.setState(DeliveryReleased.VALUE);
      
      disposition.write(_aout);
    
      _fout.finishFrame();
      _fout.flush();
    } catch (IOException e) {
      throw new AmqpException(e);
    }
  }
  
  @Override
  public void onBegin(FrameBegin frameBegin) throws IOException
  {
    _sessions[0] = new AmqpSession();
    
    System.out.println("CLIENT_BEGIN: " + frameBegin);
  }
  
  @Override
  public void onAttach(FrameAttach frameAttach) throws IOException
  {
    int handle = frameAttach.getHandle();
    
    Link link = _links.get(handle);
    AmqpClientReceiver receiver = link.getReceiver();
    
    if (receiver != null) {
      receiver.setDeliveryCount(frameAttach.getInitialDeliveryCount());
    }
  }
  
  @Override
  public void onTransfer(AmqpFrameReader fin,
                         FrameTransfer frameTransfer)
    throws IOException
  {
    int handle = frameTransfer.getHandle();
    
    Link link = _links.get(handle);
    AmqpClientReceiver receiver = link.getReceiver();

    AmqpReader ain = new AmqpReader();
    ain.init(fin);
    
    receiver.receive(ain);
    
    /*
    long desc = ain.readDescriptor();
    
    byte []data = ain.readBinary();
    
    String value = new String(data);
    
    receiver.setValue(value);
    */
  }
  
  @Override
  public void onDisposition(FrameDisposition frameDisposition)
    throws IOException
  {
    System.out.println("CLIENT_DISPOSITION: " + frameDisposition);
  }
  
  @Override
  public void onFlow(FrameFlow frameFlow)
    throws IOException
  {
    System.out.println("CLIENT_FLOW: " + frameFlow);
  }
  
  @Override
  public void onDetach(FrameDetach frameDetach) throws IOException
  {
    System.out.println("CLIENT_DETACH: " + frameDetach);
  }
  
  @Override
  public void onEnd(FrameEnd frameEnd) throws IOException
  {
    _sessions[0] = null;
    
    System.out.println("CLIENT_END: " + frameEnd);
  }
  
  @Override
  public void onClose(FrameClose frameClose) throws IOException
  {
    disconnect();
  }

  public void onClose()
  {
    close();
  }

  public void close()
  {
    if (! _isClosed.compareAndSet(false, true)) {
      return;
    }
    try {
      closeSessions();
      
      closeConnection();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      disconnect();
    }
  }
  
  private void closeSessions()
    throws IOException
  {
    for (int i = 0; i < _sessions.length; i++) {
      AmqpSession session = _sessions[i];
      _sessions[i] = null;
      
      System.out.println("CS: " + session);
      
      if (session != null) {
        closeSession(i);
      }
    }
  }
  
  private void closeSession(int channel)
    throws IOException
  {
    FrameEnd end = new FrameEnd();
    
    writeFrame(end);
  }
  
  private void closeConnection()
    throws IOException
  {
    FrameClose close = new FrameClose();
    
    writeFrame(close);
  }
  
  
  public void disconnect()
  {
    try {
      disconnectImpl();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  private void disconnectImpl()
    throws IOException
  {
    _isDisconnected = true;
    
    QSocketWrapper s = _s;
    _s = null;
    
    ReadStream is = _is;
    _is = null;
    
    WriteStream os = _os;
    _os = null;
   
    if (s != null) {
      s.close();
    }
    
    if (is != null) {
      is.close();
    }
    
    if (os != null) {
      os.close();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _hostname + ":" + _port + "]";
  }
  
  static class Link {
    private FrameAttach _attach;
    private AmqpClientReceiver _receiver;
    
    Link(FrameAttach attach)
    {
      _attach = attach;
    }
    
    void setReceiver(AmqpClientReceiver receiver)
    {
      _receiver = receiver;
    }
    
    AmqpClientReceiver getReceiver()
    {
      return _receiver;
    }
  }
}
