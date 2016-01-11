/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.bartender.hamp.LinkHamp;

/**
 * HMTP client protocol
 */
public class LinkServer extends LinkHamp
{
  private static final Logger log
    = Logger.getLogger(LinkServer.class.getName());

  private LinkChildServiceImpl _serverLinkImpl;

  public LinkServer(LinkChildServiceImpl serverLinkImpl,
                    InputStream is, 
                    OutputStream os)
    throws IOException
  {
    super(is, os);

    _serverLinkImpl = serverLinkImpl;
    
    Objects.requireNonNull(serverLinkImpl);
    
    serverLinkImpl.initLink(this);
    
    newService(serverLinkImpl).address("public:///server")
                           .ref();
  }

  /**
   * Receive messages from the client
   */
  @Override
  public void run()
  {
    try {
      Thread.currentThread().setName("server-main-link");
      // ClassLoader loader = ClassLoader.getSystemClassLoader();
      // Thread.currentThread().setContextClassLoader(loader);

      super.run();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " finishing main thread");
      }
      
      close();
    }
  }
  
  @Override
  public void close()
  {
    try {
      super.close();
    } finally {
      _serverLinkImpl.destroy();
    }
  }
}
