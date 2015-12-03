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

package com.caucho.cloud.hmtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.broker.PassthroughBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.hemp.servlet.ClientStubManager;
import com.caucho.hemp.servlet.ServerGatewayBroker;
import com.caucho.hemp.servlet.ServerProxyBroker;
import com.caucho.hmtp.HmtpWebSocketReader;
import com.caucho.hmtp.HmtpWebSocketWriter;
import com.caucho.network.listen.AbstractProtocolConnection;
import com.caucho.network.listen.SocketLink;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Handles HMTP requests from a peer server.
 * 
 * If the request does not match one of the HMTP codes, it the HmtpRequest
 * will forward to the HMUX request for compatibility.
 */
public class HmtpRequest extends AbstractProtocolConnection
{
  private static final Logger log = Logger.getLogger(HmtpRequest.class.getName());
  
  private static final L10N L = new L10N(HmtpRequest.class);

  public static final int HMUX_TO_UNIDIR_HMTP = '7';
  public static final int HMUX_SWITCH_TO_HMTP = '8';
  public static final int HMUX_HMTP_OK        = '9';
  
  private SocketLink _conn;
  private BamSystem _bamService;
  
  private ReadStream _rawRead;
  private WriteStream _rawWrite;
  
  private boolean _isFirst;

  private HmtpWebSocketReader _hmtpReader;
  private HmtpWebSocketWriter _hmtpWriter;

  private ClientStubManager _clientManager;
  private Broker _toLinkBroker;
  private Broker _proxyBroker;
  
  private HmtpLinkActor _linkActor;

  public HmtpRequest(SocketLink conn,
                     BamSystem bamService)
  {
    _conn = conn;
    _bamService = bamService;
    
    if (conn == null)
      throw new NullPointerException();
    
    if (bamService == null)
      throw new NullPointerException();

    _rawRead = conn.getReadStream();
    _rawWrite = conn.getWriteStream();
  }

  @Override
  public boolean isWaitForRead()
  {
    return true;
  }
  
  @Override
  public void onStartConnection()
  {
    _isFirst = true;
  }

  @Override
  public boolean handleRequest()
    throws IOException
  {
    try {
      if (_isFirst) {
        return handleInitialRequest();
      }
      else {
        return dispatchHmtp();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    }
  }

  private boolean handleInitialRequest()
    throws IOException
  {
    _isFirst = false;
    
    ReadStream is = _rawRead;
    
    int ch = is.read();

    if (ch < 0)
      return false;
    
    boolean isUnidir = false;
    
    if (ch == HMUX_TO_UNIDIR_HMTP)
      isUnidir = true;
    else if (ch == HMUX_SWITCH_TO_HMTP)
      isUnidir = false;
    else
      throw new UnsupportedOperationException(L.l("0x{0} is an invalid HMUX code.",
                                                  Integer.toHexString(ch)));

    int len = (is.read() << 8) + is.read();
    int adminCode = is.read();
    boolean isAdmin = adminCode != 0;

    InputStream rawIs = is;
    
    is.skip(len - 1);

    _hmtpReader = new HmtpWebSocketReader(rawIs);

    _hmtpWriter = new HmtpWebSocketWriter(_rawWrite);
    // _hmtpWriter.setId(getRequestId());
    // _hmtpWriter.setAutoFlush(true);

    ManagedBroker broker = _bamService.getBroker();

    _hmtpWriter.setAddress("hmtp-server-" + _conn.getId() + "-hmtp");

    Mailbox toLinkMailbox = new MultiworkerMailbox(_hmtpWriter.getAddress(), _hmtpWriter, broker, 1);
    _toLinkBroker = new PassthroughBroker(toLinkMailbox);
    
    _clientManager = new ClientStubManager(broker, toLinkMailbox);

    _linkActor = new HmtpLinkActor(_toLinkBroker,
                                   _clientManager,
                                   _bamService.getLinkManager(),
                                   _conn.getRemoteHost());

    if (isUnidir) {
      _proxyBroker = new ServerGatewayBroker(broker,
                                             _clientManager, 
                                             _linkActor.getActor());
    }
    else {
      _proxyBroker = new ServerProxyBroker(broker,
                                           _clientManager, 
                                           _linkActor.getActor());
    }

    return dispatchHmtp();
  }

  private boolean dispatchHmtp()
    throws IOException
  {
    HmtpWebSocketReader in = _hmtpReader;

    do {
      Broker broker = _proxyBroker;

      if (! in.readPacket(broker)) {
        return false;
      }
      
      _bamService.addExternalMessageRead();
    } while (in.isDataAvailable());

    return true;
  }

  /**
   * Close when the socket closes.
   */
  @Override
  public void onCloseConnection()
  {
    HmtpLinkActor linkActor = _linkActor;
    _linkActor = null;

    Broker linkBroker = _toLinkBroker;
    _toLinkBroker = null;
    
    if (linkBroker != null)
      linkBroker.close();

    if (linkActor != null) {
      linkActor.onCloseConnection();
    }

    /*
    if (linkStream != null)
      linkStream.close();
      */

    HmtpWebSocketWriter writer = _hmtpWriter;

    if (writer != null)
      writer.close();
  }

  protected String getRequestId()
  {
    return "hmtp" + ":" + _conn.getId();
  }

  public final String dbgId()
  {
    return "Hmtp[" + _conn.getId() + "] ";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + dbgId();
  }
}
