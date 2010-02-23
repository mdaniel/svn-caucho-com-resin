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

package com.caucho.hmtp;

import com.caucho.hessian.io.*;
import com.caucho.util.Hex;

import java.io.*;
import java.lang.reflect.Method;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyRep;
import java.security.PublicKey;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Manages links on the client
 */

public class ClientAuthManager {
  private static final Logger log
    = Logger.getLogger(ClientAuthManager.class.getName());

  private static Method _keyRepReadResolve;

  private KeyPair _authKeyPair; // authentication key pair

  public PublicKey getPublicKey(GetPublicKeyQuery query)
  {
    return getPublicKey(query.getAlgorithm(),
			query.getFormat(),
			query.getEncoded());
  }
  
  public PublicKey getPublicKey(String algorithm,
				String format,
				byte []encData)
  {
    try {
      KeyRep keyRep = new KeyRep(KeyRep.Type.PUBLIC,
				 algorithm,
				 format,
				 encData);

      PublicKey publicKey = (PublicKey) _keyRepReadResolve.invoke(keyRep);

      return publicKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Secret generateSecret()
  {
    try {
      KeyGenerator gen = KeyGenerator.getInstance("AES");

      SecretKey key = gen.generateKey();

      return new Secret(key, "AES");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EncryptedObject encrypt(Secret secret,
				 PublicKey publicKey,
				 Object object)
  {
    byte []encKey = wrapSecret(secret.getKey(), publicKey);
    byte []encData = encode(secret.getKey(), object);

    return new EncryptedObject(secret.getAlgorithm(),
			       encKey,
			       encData);
  }

  public byte []wrapSecret(SecretKey secretKey, PublicKey publicKey)
  {
    try {
      Cipher keyCipher = Cipher.getInstance("RSA");
      
      keyCipher.init(Cipher.WRAP_MODE, publicKey);

      byte []encKey = keyCipher.wrap(secretKey);

      return encKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public byte []encode(SecretKey key, Object value)
  {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Hessian2Output out = new Hessian2Output(bos);

      out.writeObject(value);
      
      out.close();

      byte []plainData = bos.toByteArray();

      Cipher cipher = Cipher.getInstance("AES");

      cipher.init(Cipher.ENCRYPT_MODE, key);

      byte []encData = cipher.doFinal(plainData);

      return encData;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /*
  public void decode()
  {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Hessian2Output out = new Hessian2Output(bos);

      out.writeObject(credentials);
      
      out.close();

      byte []plainData = bos.toByteArray();
      
      KeyGenerator gen = KeyGenerator.getInstance("AES");

      SecretKey key = gen.generateKey();
      Cipher cipher = Cipher.getInstance("AES");

      cipher.init(Cipher.ENCRYPT_MODE, key);

      byte []encData = cipher.doFinal(plainData);

      Cipher keyCipher = Cipher.getInstance("RSA");
      keyCipher.init(Cipher.WRAP_MODE, publicKey);

      byte []encKey = keyCipher.wrap(key);

      AuthenticateQuery auth = new AuthenticateQuery("AES", encKey, encData);

      
      stream.querySet(0, "cluster@" + _server.getBamAdminName(), "", auth);

      Object result = stream.readQueryResult(0);
  }
  */

  public static class Secret {
    private final SecretKey _key;
    private final String _algorithm;

    Secret(SecretKey key, String algorithm)
    {
      _key = key;
      _algorithm = algorithm;
    }

    public SecretKey getKey()
    {
      return _key;
    }

    public String getAlgorithm()
    {
      return _algorithm;
    }
  }
  
  static {
    for (Method method : KeyRep.class.getDeclaredMethods()) {
      if (method.getName().equals("readResolve")) {
	_keyRepReadResolve = method;
	method.setAccessible(true);
      }
    }
  }
}
