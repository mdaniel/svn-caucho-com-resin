/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.http;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import javax.net.*;
import javax.net.ssl.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.server.*;

/**
 * The factory class for JDK 1.4-style SSL.
 */
public class SSLFactory {
  static L10N L = new L10N(SSLFactory.class);

  /**
   * Creates a new SSL-based ServerSocket.
   *
   * @param name the debugging name, e.g. "http".
   * @param node the configuration node.
   * @param host the host interface to listen to.
   * @param port the port to listen to.
   * @param listen accept listen backlog.
   */
  static ServerSocket
  getServerSocket(String name, RegistryNode node,
                  String host, int port, int listen)
    throws Exception
  {
    name = "ssl " + name;
    
    // set up key manager to do server authentication

    /*
    String keyStoreType = node.getString("key-store-type", "jks");
    
    String keyStoreFile = node.getString("key-store-file", null);
    if (keyStoreFile == null)
      throw Application.error(node, L.l("ssl expects key-store-file"));
    
    String keyStorePassword = node.getString("key-store-password", null);
    if (keyStorePassword == null)
      throw Application.error(node, L.l("ssl expects key-store-password"));

    boolean authenticateClient = node.getBoolean("authenticate-client", false);
    String verifyClient = node.getString("verify-client", null);

    if (verifyClient != null && ! verifyClient.equals("none"))
      authenticateClient = true;
    
    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
    
    InputStream is = Vfs.openRead(keyStoreFile);
    try {
      keyStore.load(is, keyStorePassword.toCharArray());
    } finally {
      is.close();
    }
    
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    
    kmf.init(keyStore, keyStorePassword.toCharArray());
      
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), null, null);

    SSLServerSocketFactory factory;
    factory = sslContext.getServerSocketFactory();

    ServerSocket serverSocket = null;
    
    try {
      if (host == null || host.equals("*")) {
        serverSocket = factory.createServerSocket(port, listen);
        System.out.println(name + " listening to *:" + port);
      }
      else {
        InetAddress hostAddr = InetAddress.getByName(host);
        serverSocket = factory.createServerSocket(port, listen, hostAddr);
        System.out.println(name + " listening to " + host + ":" + port);
      }
    } catch (BindException e) {
      if (host == null)
        System.out.println(name + " can't bind to port *:" + port +
                           ". Check for conflicting servers.");
      else {
        InetAddress hostAddr = InetAddress.getByName(host);
        
        System.out.println(name + " can't bind to port " + host + ":" + port +
                           ".");
        System.out.println("1) Check for conflicting servers.");
        System.out.println("2) Check that " + hostAddr + " is a valid interface for this machine.");
      }
      
      throw e;
    }

    if (authenticateClient)
      ((SSLServerSocket) serverSocket).setNeedClientAuth(true);

    return serverSocket;
    */

    return null;
  }
}
