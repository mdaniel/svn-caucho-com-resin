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

package com.caucho.cloud.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import com.caucho.network.server.AbstractNetworkService;
import com.caucho.network.server.NetworkServer;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

/**
 * Interface for a service registered with the Resin Server.
 */
public class SecurityService extends AbstractNetworkService
{
  private static final L10N L = new L10N(SecurityService.class);
  
  private byte []_signatureSecret;
  
  public static SecurityService create()
  {
    NetworkServer server = NetworkServer.getCurrent();

    if (server == null) {
      throw new IllegalStateException(L.l("NetworkServer is not active in {0}",
                                          Thread.currentThread().getContextClassLoader()));
    }
    
    synchronized (server) {
      SecurityService service = server.getService(SecurityService.class);
      
      if (service == null) {
        service = new SecurityService();
        server.addService(service);
      }
      
      return service;
    }
  }
  
  public void setSignatureSecret(String text)
  {
    try {
      setSignatureSecret(text.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }
  
  public void setSignatureSecret(byte []secret)
  {
    _signatureSecret = secret;
  }
  
  public String sign(String uid, String nonce)
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      
      digest.update(uid.getBytes("UTF-8"));
      digest.update(nonce.getBytes("UTF-8"));

      if (_signatureSecret != null)
        digest.update(_signatureSecret);
      
      return Base64.encode(digest.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public byte [] sign(byte []data)
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      
      digest.update(data);

      if (_signatureSecret != null)
        digest.update(_signatureSecret);
      
      return digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
