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

package com.caucho.hmtp;

import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamException;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.RemoteListenerUnavailableException;
import com.caucho.bam.actor.AbstractActorSender;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.actor.SimpleActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.client.LinkConnection;
import com.caucho.bam.client.LinkConnectionFactory;
import com.caucho.bam.stream.MessageStream;
import com.caucho.cloud.security.SecurityService;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.websocket.WebSocketListener;

/**
 * HMTP client protocol
 */
class HmtpLinkFactory implements LinkConnectionFactory {
  private static final L10N L = new L10N(HmtpLinkFactory.class);
  
  private static final Logger log
    = Logger.getLogger(HmtpLinkFactory.class.getName());
  
  private String _url;
  private String _address;
  private String _virtualHost;

  private WebSocketClient _webSocketClient;
  private WebSocketListener _webSocketHandler;
  
  private String _user;
  private String _password;
  private Serializable _credentials;
  
  private BamException _connException;
  
  private ClientAuthManager _authManager = new ClientAuthManager();
  private boolean _isMasked;

  public HmtpLinkFactory()
  {
  }
  
  public void setUrl(String url)
  {
    _url = url;
  }

  public void setVirtualHost(String host)
  {
    _virtualHost = host;
  }

  public void setEncryptPassword(boolean isEncrypt)
  {
  }
  
  public void setMasked(boolean isMasked)
  {
    _isMasked = isMasked;
  }

  public void connect()
  {
  }

  public void connect(String user, String password)
  {
    _user = user;
    _password = password;
  }

  public void connect(String user, Serializable credentials)
  {
    _user = user;
    _credentials = credentials;
  }

  @Override
  public LinkConnection open(Broker broker)
  {
    try {
      HmtpWebSocketListener webSocketHandler = new HmtpWebSocketListener(broker);
        
      WebSocketClient oldClient = _webSocketClient;

      _webSocketClient = new WebSocketClient(_url, webSocketHandler);
      
      if (oldClient != null) {
        oldClient.close(1000, "ok");
      }
      
      if (_virtualHost != null)
        _webSocketClient.setVirtualHost(_virtualHost);
      _webSocketClient.setConnectTimeout(15000);
      
      _webSocketClient.setMasked(_isMasked);
      _webSocketClient.connect();
      
      return new HmtpLinkConnection(_webSocketClient, webSocketHandler);
    } catch (RuntimeException e) {
      throw e;
    } catch (ConnectException e) {
      String msg = "Cannot connect to " + _url + "\n  " + e;
      
      throw new RemoteConnectionFailedException(msg, e);
    } catch (IOException e) {
      String msg = "Cannot establish HTTP protocol connection to " + _url + "\n  " + e;
      
      throw new RemoteListenerUnavailableException(msg, e);
    }
  }
      
  /**
   * Login to the server
   */
  protected void loginImpl(String uid, Serializable credentials)
  {
    try {
      if (uid == null)
        uid = "";
      
      if (credentials == null)
        credentials = "";
      
      if (credentials instanceof SignedCredentials) {
      }
      else if (credentials instanceof String) {
        String password = (String) credentials;
        
        String clientNonce = String.valueOf(CurrentTime.getCurrentTime());
        
        NonceQuery nonceQuery = new NonceQuery("", uid, clientNonce);
        NonceQuery nonceResult = null;
        //          = (NonceQuery) query(null, nonceQuery);
        
        String algorithm = nonceResult.getAlgorithm();
        String serverNonce = nonceResult.getNonce();
        String serverSignature = nonceResult.getSignature();
        
        String testSignature = _authManager.sign(algorithm, uid, clientNonce, password);
        
        if (! testSignature.equals(serverSignature) && "".equals(uid))
          throw new BamException(L.l("{0} server signature does not match",
                                      this));

        String signature = _authManager.sign(algorithm, uid, serverNonce, password);

        SecurityService security = SecurityService.getCurrent();
        
        if ("".equals(uid))
          credentials = new SignedCredentials(uid, serverNonce, signature);
        else {
          credentials = security.createCredentials(algorithm, 
                                                   uid, password, 
                                                   serverNonce);
        }
      }

      AuthResult result = null;
      // result = (AuthResult) query(null, new AuthQuery(uid, credentials));

      _address = result.getAddress();

      if (log.isLoggable(Level.FINE))
        log.fine(this + " login");
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the address
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * Returns the broker address
   */
  public String getBrokerAddress()
  {
    String address = getAddress();

    if (address == null)
      return null;

    int p = address.indexOf('@');
    int q = address.indexOf('/');

    if (p >= 0 && q >= 0)
      return address.substring(p + 1, q);
    else if (p >= 0)
      return address.substring(p + 1);
    else if (q >= 0)
      return address.substring(0, q);
    else
      return address;
  }

  public void flush()
    throws IOException
  {
    /*
    ClientToLinkStream stream = _linkStream;

    if (stream != null)
      stream.flush();
      */
  }

  public void close()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close");

    // super.close();

    if (_webSocketClient != null)
      _webSocketClient.close(1000, "ok");
   }

  /* (non-Javadoc)
   * @see com.caucho.bam.client.LinkConnectionFactory#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }
}
