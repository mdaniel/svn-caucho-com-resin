/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.bam.BamError;
import com.caucho.bam.BamStream;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleBamService;
import com.caucho.bam.hmtp.AuthQuery;
import com.caucho.bam.hmtp.AuthResult;
import com.caucho.bam.hmtp.GetPublicKeyQuery;
import com.caucho.hessian.io.*;
import com.caucho.util.Hex;

import java.io.*;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.logging.Logger;
import javax.crypto.Cipher;

/**
 * Manages links on the server
 */

public class ServerLinkManager {
  private static final Logger log
    = Logger.getLogger(ServerLinkManager.class.getName());

  private KeyPair _authKeyPair; // authentication key pair

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
}
