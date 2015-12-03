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

package com.caucho.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.bam.broker.Broker;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.broker.PassthroughBroker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.stream.MessageStream;
import com.caucho.config.Admin;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.servlet.ClientStubManager;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.hemp.servlet.ServerLinkActor;
import com.caucho.hemp.servlet.ServerProxyBroker;
import com.caucho.hmtp.HmtpReader;
import com.caucho.hmtp.HmtpWebSocketContextWriter;
import com.caucho.hmtp.HmtpWriter;
import com.caucho.security.Authenticator;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.util.L10N;
import com.caucho.websocket.AbstractWebSocketListener;
import com.caucho.websocket.WebSocketContext;
import com.caucho.websocket.WebSocketListener;

/**
 * Main protocol handler for the HTTP version of BAM.
 */
@SuppressWarnings("serial")
public class HmtpServlet extends GenericServlet {
  private static final Logger log
    = Logger.getLogger(HmtpServlet.class.getName());
  private static final L10N L = new L10N(HmtpServlet.class);

  private boolean _isAdmin;
  private boolean _isAuthenticationRequired = true;

  private @Inject Instance<Authenticator> _authInstance;
  private @Inject @Admin Instance<Authenticator> _adminInstance;

  private ManagedBroker _broker;
  private Authenticator _auth;
  private ServerAuthManager _authManager;

  public void setAdmin(boolean isAdmin)
  {
    _isAdmin = isAdmin;
  }

  public void setAuthenticationRequired(boolean isAuthRequired)
  {
    _isAuthenticationRequired = isAuthRequired;
  }

  public boolean isAuthenticationRequired()
  {
    return _isAuthenticationRequired;
  }

  public void init()
  {
    String authRequired = getInitParameter("authentication-required");

    if ("false".equals(authRequired))
      _isAuthenticationRequired = false;

    String admin = getInitParameter("admin");

    if ("true".equals(admin))
      _isAdmin = true;

    // _authManager = new ServerAuthManager(_auth);
    _authManager = new ServerAuthManager();
    _authManager.setAuthenticationRequired(_isAuthenticationRequired);

    try {
      if (_isAdmin)
        _auth = _adminInstance.get();
      else
        _auth = _authInstance.get();
    } catch (Exception e) {
      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.",
                                 this), e);
      }
      else if (_authManager.isClusterSystemKey()) {
      }
      else if (_isAuthenticationRequired){
        // network/03f0
        log.info(L.l("{0} requires an active com.caucho.security.Authenticator because HMTP messaging requires authenticated login for security.  In the resin.xml, add an <sec:AdminAuthenticator>",
                   this));
      }
    }

    if (_isAdmin)
      _broker = ServletService.getCurrent().getAdminBroker();
    else
      _broker = HempBroker.getCurrent();
    
    log.fine(L.l("{0} starting with broker={1}", this, _broker));
  }

  /**
   * Service handling
   */
  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequestImpl req = (HttpServletRequestImpl) request;
    HttpServletResponseImpl res = (HttpServletResponseImpl) response;

    String upgrade = req.getHeader("Upgrade");

    if (! "WebSocket".equals(upgrade)) {
      // eventually can use alt method
      res.sendError(400, "Upgrade denied:" + upgrade);
      return;
    }
    
    String ipAddress = req.getRemoteAddr();
    
    WebSocketHandler handler
      = new WebSocketHandler(ipAddress);
    
    WebSocketContext webSocket = req.startWebSocket(handler);

    webSocket.setTimeout(30 * 60 * 1000L);
  }
  
  protected ManagedBroker getBroker()
  {
    return _broker;
  }
  
  class WebSocketHandler extends AbstractWebSocketListener {
    private String _ipAddress;
    
    private HmtpReader _in;
    private HmtpWebSocketContextWriter _out;
    
    private Broker _linkStream;
    private Broker _broker;
    
    private ServerLinkActor _linkService;

    private ClientStubManager _clientManager;

    WebSocketHandler(String ipAddress)
    {
      _ipAddress = ipAddress;
    }
    
    @Override
    public void onStart(WebSocketContext context) throws IOException
    {
      _in = new HmtpReader();
      _out = new HmtpWebSocketContextWriter(context);
      
      ManagedBroker broker = getBroker();
      Mailbox toLinkMailbox = new MultiworkerMailbox(_out.getAddress(), _out, 
                                                     broker, 1);
      
      _linkStream = new PassthroughBroker(toLinkMailbox);
      _clientManager = new ClientStubManager(broker, toLinkMailbox);
      _linkService = new ServerLinkActor(_linkStream, _clientManager, 
                                         _authManager, _ipAddress);
      _broker = new ServerProxyBroker(broker, _clientManager,
                                      _linkService.getActor());
    }

    @Override
    public void onDisconnect(WebSocketContext context) throws IOException
    {
      ClientStubManager clientManager = _clientManager;
      _clientManager = null;
      
      if (clientManager != null) {
        clientManager.logout();
      }
    }

    @Override
    public void onReadBinary(WebSocketContext context,
                             InputStream is)
      throws IOException
    {
      _in.readPacket(is, _broker);
    }

    @Override
    public void onTimeout(WebSocketContext context) throws IOException
    {
    }

    /* (non-Javadoc)
     * @see com.caucho.servlet.WebSocketListener#onReadText(com.caucho.servlet.WebSocketContext, java.io.Reader)
     */
    @Override
    public void onReadText(WebSocketContext context, Reader is)
        throws IOException
    {
      // TODO Auto-generated method stub
      
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _ipAddress + "]";
    }
  }
}
