/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamException;
import com.caucho.bam.actor.ActorHolder;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.client.LinkClient;
import com.caucho.bam.query.QueryCallback;
import com.caucho.bam.query.QueryManager;
import com.caucho.bam.stream.NullActor;
import com.caucho.cloud.security.SecurityService;
import com.caucho.remote.websocket.WebSocketClient;
import com.caucho.security.DigestCredentials;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.websocket.WebSocketListener;

/**
 * HMTP client protocol
 */
public class HmtpClient implements ActorSender {
  private static final L10N L = new L10N(HmtpClient.class);
  
  private static final Logger log
    = Logger.getLogger(HmtpClient.class.getName());
  
  private String _url;
  private String _address;
  private String _virtualHost;

  private ActorHolder _actor;
  
  private HmtpLinkFactory _linkFactory;
  private LinkClient _linkClient;
  
  private WebSocketClient _webSocketClient;
  
  private WebSocketListener _webSocketHandler;
  
  private BamException _connException;

  private Broker _linkBroker;
  
  private boolean _isMasked;
  
  private ClientAuthManager _authManager = new ClientAuthManager();
  
  public HmtpClient(String url, ActorHolder actor)
    throws IOException
  {
    _actor = actor;
    _url = url;
    
    init();
  }
  
  public HmtpClient(String url)
  {
    _actor = new NullActor("HmtpClient@" + url);
    _url = url;
    
    init();
  }

  public void setVirtualHost(String host)
  {
    _linkFactory.setVirtualHost(host);
  }

  public void setEncryptPassword(boolean isEncrypt)
  {
  }
  
  @Override
  public Broker getBroker()
  {
    return _linkClient.getBroker();
  }

  public void connect()
  {
    _linkFactory.connect();
  }

  public void connect(String user, String password)
  {
    _linkFactory.connect();
    
    loginImpl(user, password);
  }

  public void connect(String user, Serializable credentials)
  {
    _linkFactory.connect();
    
    loginImpl(user, credentials);
  }

  private void init()
  {
    _linkFactory = new HmtpLinkFactory();
    _linkFactory.setUrl(_url);
    _linkFactory.setMasked(_isMasked);
    
    _linkClient = new LinkClient(_linkFactory, _actor);
    
    _actor.setBroker(_linkClient.getBroker());
  }
  
  protected void connectImpl()
  {
    try {
      _linkClient.start();
      /*
      if (_actor != null)
        _webSocketHandler = new HmtpWebSocketListener(_actor);
      else
        _webSocketHandler = new HmtpWebSocketListener(new NullActorStream());
        
      _webSocketClient = new WebSocketClient(_url, _webSocketHandler);
      
      _webSocketClient.connect();
        */
    } catch (BamException e) {
      _connException = e;

      throw _connException;
      /*
    } catch (IOException e) {
      _connException = new RemoteConnectionFailedException("Failed to connect to server at " + _url + "\n  " + e, 
                                                           e);

      throw _connException;
      */
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
        
        String clientNonce = String.valueOf(Alarm.getCurrentTime());
        
        NonceQuery nonceQuery = new NonceQuery("", uid, clientNonce);
        NonceQuery nonceResult = (NonceQuery) query(null, nonceQuery);
        
        String serverAlgorithm = nonceResult.getAlgorithm();
        String serverNonce = nonceResult.getNonce();
        String serverSignature = nonceResult.getSignature();
        String testSignature = _authManager.sign(serverAlgorithm, 
                                                 uid, 
                                                 clientNonce,
                                                 password);

        if (testSignature.equals(serverSignature)) {
        }
        else if ("".equals(uid)) {
          throw new BamException(L.l("{0} resin-system-auth-key does not match the server's value",
                                      this));
        }

        String signature = _authManager.sign(serverAlgorithm, 
                                             uid, 
                                             serverNonce, 
                                             password);

        SecurityService security = SecurityService.getCurrent();
        
        if ("".equals(uid))
          credentials = new SignedCredentials(uid, serverNonce, signature);
        else
          credentials = new DigestCredentials(uid, serverNonce, signature);
        /*
        else if (security != null)
          credentials = security.createCredentials(uid, password, serverNonce);
        else {
          security = new SecurityService();
          credentials = security.createCredentials(uid, password, serverNonce);
        }
        */
      }

      AuthResult result = (AuthResult) query(null, new AuthQuery(uid, credentials));

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
    
    HmtpLinkFactory linkFactory = _linkFactory;
    _linkFactory = null;
    
    if (linkFactory != null)
      linkFactory.close();

    if (_webSocketClient != null)
      _webSocketClient.close();
   }

  /* (non-Javadoc)
   * @see com.caucho.bam.actor.ActorSender#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public long nextQueryId()
  {
    return _linkClient.getSender().nextQueryId();
  }

  @Override
  public void message(String to, Serializable payload)
  {
    _linkClient.getSender().message(to, payload);
  }

  @Override
  public Serializable query(String to, Serializable payload)
  {
    return _linkClient.getSender().query(to, payload);
  }

  @Override
  public Serializable query(String to, Serializable payload, long timeout)
  {
    return _linkClient.getSender().query(to, payload, timeout);
  }

  @Override
  public void query(String to, Serializable payload, QueryCallback callback)
  {
    _linkClient.getSender().query(to, payload, callback);
  }
  
  @Override
  public QueryManager getQueryManager()
  {
    return _linkClient.getSender().getQueryManager();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "," + _actor + "]";
  }

  @Override
  protected void finalize()
  {
    close();
  }
}
