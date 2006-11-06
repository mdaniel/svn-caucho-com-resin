/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.vfs;

import java.io.*;
import java.net.*;

import java.security.Key;
import java.security.KeyStore;
import java.security.GeneralSecurityException;

import java.security.cert.Certificate;

import javax.annotation.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;

import com.caucho.util.L10N;

import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
public class JsseSSLFactory implements SSLFactory {
  private static final L10N L = new L10N(JsseSSLFactory.class);
  
  private Path _keyStoreFile;
  private String _alias;
  private String _password;
  private String _verifyClient;
  private String _keyStoreType = "jks";
  private String _keyManagerFactory = "SunX509";
  private String _sslContext = "TLS";

  private KeyStore _keyStore;
  
  /**
   * Creates a ServerSocket factory without initializing it.
   */
  public JsseSSLFactory()
  {
  }

  /**
   * Sets the key store
   */
  public void setKeyStoreFile(Path keyStoreFile)
  {
    _keyStoreFile = keyStoreFile;
  }

  /**
   * Returns the certificate file.
   */
  public Path getKeyStoreFile()
  {
    return _keyStoreFile;
  }

  /**
   * Sets the password.
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Returns the key file.
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Sets the certificate alias
   */
  public void setAlias(String alias)
  {
    _alias = alias;
  }

  /**
   * Returns the alias.
   */
  public String getAlias()
  {
    return _alias;
  }

  /**
   * Sets the verifyClient.
   */
  public void setVerifyClient(String verifyClient)
  {
    _verifyClient = verifyClient;
  }

  /**
   * Returns the key file.
   */
  public String getVerifyClient()
  {
    return _verifyClient;
  }

  /**
   * Sets the key-manager-factory
   */
  public void setKeyManagerFactory(String keyManagerFactory)
  {
    _keyManagerFactory = keyManagerFactory;
  }

  /**
   * Sets the ssl-context
   */
  public void setSSLContext(String sslContext)
  {
    _sslContext = sslContext;
  }

  /**
   * Sets the key-store
   */
  public void setKeyStoreType(String keyStore)
  {
    _keyStoreType = keyStore;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ConfigException, IOException, GeneralSecurityException
  {
    if (_keyStoreFile == null)
      throw new ConfigException(L.l("`key-store-file' is required for JSSE."));
    if (_password == null)
      throw new ConfigException(L.l("`password' is required for JSSE."));

    _keyStore = KeyStore.getInstance(_keyStoreType);
    
    InputStream is = _keyStoreFile.openRead();
    try {
      _keyStore.load(is, _password.toCharArray());
    } finally {
      is.close();
    }

    if (_alias != null) {
      Key key = _keyStore.getKey(_alias, _password.toCharArray());

      if (key == null)
	throw new ConfigException(L.l("JSSE alias '{0}' does not have a corresponding key.",
				  _alias));

      Certificate []certChain = _keyStore.getCertificateChain(_alias);
      
      if (certChain == null)
	throw new ConfigException(L.l("JSSE alias '{0}' does not have a corresponding certificate chain.",
				  _alias));

      _keyStore = KeyStore.getInstance(_keyStoreType);
      _keyStore.load(null, _password.toCharArray());

      _keyStore.setKeyEntry(_alias, key, _password.toCharArray(), certChain);
    }
  }

  /**
   * Creates the SSL ServerSocket.
   */
  public QServerSocket create(InetAddress host, int port)
    throws IOException, GeneralSecurityException
  {
    if (_keyStore == null)
      throw new IOException(L.l("key store is missing"));

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(_keyManagerFactory);
    
    kmf.init(_keyStore, _password.toCharArray());
      
    SSLContext sslContext = SSLContext.getInstance(_sslContext);

    sslContext.init(kmf.getKeyManagers(), null, null);

    SSLServerSocketFactory factory;
    factory = sslContext.getServerSocketFactory();

    ServerSocket serverSocket;

    int listen = 100;

    if (host == null)
      serverSocket = factory.createServerSocket(port, listen);
    else
      serverSocket = factory.createServerSocket(port, listen, host);

    SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
    
    if ("required".equals(_verifyClient))
      sslServerSocket.setNeedClientAuth(true);

    /*
    boolean hasRestriction = false;
    ArrayList<String> protocols = new ArrayList();
    if (node.getBoolean("tls1", true)) {
      protocols.add("TLSv1");
      protocols.add("TLS");
    }
    else
      hasRestriction = true;
    
    if (node.getBoolean("ssl2", true)) {
      protocols.add("SSLv2");
    }
    else
      hasRestriction = true;
    
    if (node.getBoolean("ssl3", true)) {
      protocols.add("SSLv3");
    }
    else
      hasRestriction = true;

    if (hasRestriction)
      sslServerSocket.setEnabledProtocols((String []) protocols.toArray(new String[protocols.size()]));
    */

    return new QServerSocketWrapper(serverSocket);
  }
}

