/*
 * Copyright (c) 1998-2016 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

public class CurlX509KeyManager implements X509KeyManager
{
  private X509KeyManager _keyManager;
  private String _clientAlias;
  
  public CurlX509KeyManager(X509KeyManager keyManager, String clientAlias)
  {
    _keyManager = keyManager;
    _clientAlias = clientAlias;
  }

  @Override
  public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
  {
    return _clientAlias;
  }

  @Override
  public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
  {
    return _keyManager.chooseServerAlias(keyType, issuers, socket);
  }

  @Override
  public X509Certificate[] getCertificateChain(String alias)
  {
    return _keyManager.getCertificateChain(alias);
  }

  @Override
  public String[] getClientAliases(String keyType, Principal[] issuers)
  {
    return _keyManager.getClientAliases(keyType, issuers);
  }

  @Override
  public PrivateKey getPrivateKey(String alias)
  {
    return _keyManager.getPrivateKey(alias);
  }

  @Override
  public String[] getServerAliases(String keyType, Principal[] issuers)
  {
    return _keyManager.getServerAliases(keyType, issuers);
  }
}
