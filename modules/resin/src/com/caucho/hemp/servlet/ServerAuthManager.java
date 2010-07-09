/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PublicKey;

import javax.crypto.Cipher;

import com.caucho.bam.NotAuthorizedException;
import com.caucho.cloud.security.SecurityService;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hmtp.GetPublicKeyQuery;
import com.caucho.hmtp.NonceQuery;
import com.caucho.hmtp.SignedCredentials;
import com.caucho.security.Authenticator;
import com.caucho.security.BasicPrincipal;
import com.caucho.security.PasswordCredentials;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Manages links on the server
 */

public class ServerAuthManager {
  private static final L10N L = new L10N(ServerAuthManager.class);
  
  private Server _server;
  private SecurityService _security;
  private Authenticator _auth;
  private KeyPair _authKeyPair; // authentication key pair
  private boolean _isAuthenticationRequired = true;
  private boolean _isRequireEncryptedPassword = true;
  
  private LruCache<String,String> _nonceMap
    = new LruCache<String,String>(4096);
  
  public ServerAuthManager()
  {
    _server = Server.getCurrent();
    _security = SecurityService.create();
  }
  
  public ServerAuthManager(Authenticator auth)
  {
    _auth = auth;
    _security = SecurityService.create();
  }
  
  public ServerAuthManager(Server server)
  {
    _server = server;
    _security = SecurityService.create();
  }
  
  private Authenticator getAuth()
  {
    if (_auth != null)
      return _auth;
    else if (_server != null)
      return _server.getAdminAuthenticator();
    else
      return null;
  }

  //
  // authentication
  //

  /**
   * Asks for the server's public key
   */
  public GetPublicKeyQuery getPublicKey()
  {
    try {
      if (_authKeyPair == null) {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");

        _authKeyPair = gen.generateKeyPair();
      }

      PublicKey publicKey = _authKeyPair.getPublic();

      GetPublicKeyQuery result
        = new GetPublicKeyQuery(publicKey.getAlgorithm(),
                                publicKey.getFormat(),
                                publicKey.getEncoded());
      
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Key decryptKey(String keyAlgorithm, byte []encKey)
  {
    try {
      Cipher cipher = Cipher.getInstance("RSA");

      cipher.init(Cipher.UNWRAP_MODE, _authKeyPair.getPrivate());

      Key key = cipher.unwrap(encKey, keyAlgorithm, Cipher.SECRET_KEY);

      return key;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public Object decrypt(String msgAlgorithm,
                        byte []msgKey,
                        byte []data)
  {
    try {
      Key key = decryptKey(msgAlgorithm, msgKey);

      return decrypt(key, data);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public Object decrypt(Key key, byte []data)
  {
    try {
      Cipher aes = Cipher.getInstance("AES");

      aes.init(Cipher.DECRYPT_MODE, key);

      byte []plainData = aes.doFinal(data);

      ByteArrayInputStream bis = new ByteArrayInputStream(plainData);
      Hessian2Input in = new Hessian2Input(bis);
      Object value = in.readObject();
      in.close();

      return value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  void authenticate(String uid, Object credentials, String ipAddress)
  {
    Authenticator auth = getAuth();
    
    if (credentials instanceof SignedCredentials) {
      SignedCredentials signedCred = (SignedCredentials) credentials;

      String nonce = signedCred.getNonce();
      String signature = signedCred.getSignature();
      
      /*
      String savedNonce = _nonceMap.get(uid);
      
      if (savedNonce == null)
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
                                             */
      
      String serverSignature = _security.sign(uid, nonce);
      
      if (! serverSignature.equals(signature)) {
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
      }
    }
    else if (auth == null && ! _isAuthenticationRequired) {
    }
    else if (auth == null) {
      throw new NotAuthorizedException(L.l("{0} does not have a configured authenticator",
                                             this));
    }
    else if (credentials instanceof String) {
      String password = (String) credentials;
    
      Principal user = new BasicPrincipal(uid);
      PasswordCredentials pwdCred = new PasswordCredentials(password);
    
      if (auth.authenticate(user, pwdCred, null) == null) {
        throw new NotAuthorizedException(L.l("'{0}' has invalid credentials",
                                             uid));
      }
    }
    /*
    else if (server.getAdminCookie() == null && credentials == null) {
      if (! "127.0.0.1".equals(ipAddress)) {
        throw new NotAuthorizedException(L.l("'{0}' is an invalid local address for '{1}', because no password credentials are available",
                                             ipAddress, uid));
      }
    }
    */
    else {
      throw new NotAuthorizedException(L.l("'{0}' is an unknown credential",
                                           credentials));
    }
  }
  
  NonceQuery generateNonce(NonceQuery query)
  {
    String uid = query.getUid();
    String clientNonce = query.getNonce();

    String clientSignature = _security.sign(uid, clientNonce);
    System.out.println("GENNONCE: " + query + " " + clientSignature);
    
    String nonce = String.valueOf(Alarm.getCurrentTime());
    
    System.out.println("GENNONCE->: " + nonce);
    
    return new NonceQuery(uid, nonce, clientSignature);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _auth + "]";
  }  
}
