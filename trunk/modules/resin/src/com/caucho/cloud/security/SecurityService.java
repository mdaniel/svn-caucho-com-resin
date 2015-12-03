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

package com.caucho.cloud.security;

import java.security.MessageDigest;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.AdminLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.service.*;
import com.caucho.security.*;
import com.caucho.util.Base64;

public class SecurityService extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = 30;
  
  private String _signatureSecret;
  private Authenticator _authenticator;
  
  public SecurityService()
  {
  }
  
  public static SecurityService createAndAddService()
  {
    ResinSystem system = preCreate(SecurityService.class);
    
    SecurityService service = new SecurityService();
    system.addService(SecurityService.class, service);
    
    return service;
  }
  
  public static SecurityService getCurrent()
  {
    return ResinSystem.getCurrentService(SecurityService.class);
  }
  
  public void setSignatureSecret(String secret)
  {
    if ("".equals(secret))
      secret = null;
    
    _signatureSecret = secret;
  }
  
  public boolean isSystemAuthKey()
  {
    return _signatureSecret != null;
  }
  
  public void setAuthenticator(Authenticator auth)
  {
    _authenticator = auth;
  }
  
  public Authenticator getAuthenticator()
  {
    return _authenticator;
  }
  
  public String getAlgorithm(String uid)
  {
    if (_authenticator != null)
      return _authenticator.getAlgorithm(new BasicPrincipal(uid));
    else
      return "plain";
  }
  
  public String signSystem(String uid, String nonce)
  {
    try {
      String password = null;
      
      password = _signatureSecret;
      
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      
      if (uid != null)
        digest.update(uid.getBytes("UTF-8"));
      
      digest.update(nonce.getBytes("UTF-8"));

      if (password != null)
        digest.update(password.getBytes("UTF-8"));
      
      return Base64.encode(digest.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public String sign(String algorithm,
                     String uid, String password, 
                     String nonce)
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      
      if (uid != null)
        digest.update(uid.getBytes("UTF-8"));
      
      digest.update(nonce.getBytes("UTF-8"));
      
      if (password != null) {
        char []pwDigest = DigestBuilder.getDigest(new BasicPrincipal(uid),
                                                  algorithm,
                                                  password.toCharArray(),
                                                  algorithm.toCharArray());
          
        if (pwDigest != null)
          password = new String(pwDigest);

        digest.update(password.getBytes("UTF-8"));
      }

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
        digest.update(_signatureSecret.getBytes("UTF-8"));
      
      return digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public DigestCredentials createCredentials(String algorithm,
                                             String user,
                                             String password,
                                             String nonce)
  {
    String digest = sign(algorithm, user, password, nonce);
    
    DigestCredentials cred = new DigestCredentials(user, nonce, digest);
    cred.setRealm("resin");
    
    return cred;
  }
  
  public byte []createDigest(String user, 
                             String password, 
                             String nonce)
  {
    try {
      String realm = "resin";
      
      MessageDigest md = MessageDigest.getInstance("MD5");
      
      if (user != null)
        md.update(user.getBytes("UTF-8"));
      
      md.update((byte) ':');
      md.update(realm.getBytes("UTF-8"));
      md.update((byte) ':');
      
      if (password != null)
        md.update(password.getBytes("UTF-8"));
      
      byte []digest = md.digest();
      
      md.reset();
      
      updateHex(md, digest);
      md.update((byte) ':');
      md.update(nonce.getBytes("UTF-8"));
      
      return md.digest();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  
  private void updateHex(MessageDigest md, byte []digest)
  {
    for (int i = 0; i < digest.length; i++) {
      updateHex(md, digest[i] >> 4);
      updateHex(md, digest[i]);
    }
  }
  
  private void updateHex(MessageDigest md, int digit)
  {
    digit = digit & 0xf;
    
    if (digit < 10)
      md.update((byte) (digit + '0'));
    else
      md.update((byte) (digit - 10 + 'a'));
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    InjectManager cdiManager = InjectManager.getCurrent();
    
    if (_authenticator == null) {
      Bean<Authenticator> bean = findAuthenticator(cdiManager);
      
      if (bean != null) {
        CreationalContext<Authenticator> env 
          = cdiManager.createCreationalContext(bean);
        
        _authenticator = (Authenticator)
           cdiManager.getReference(bean, Authenticator.class, env);
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  private Bean<Authenticator> findAuthenticator(InjectManager cdiManager)
  {
    Set<Bean<?>> beans = cdiManager.getBeans(Authenticator.class,
                                             new AdminLiteral());
   
    if (beans.size() > 0) {
      return (Bean<Authenticator>) cdiManager.resolve(beans);
    }
    
    return null;
  }
}
