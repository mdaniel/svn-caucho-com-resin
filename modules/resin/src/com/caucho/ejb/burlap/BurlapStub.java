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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import com.caucho.hessian.io.HessianRemoteObject;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.log.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.ReadWritePair;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for generated object stubs.
 */
public abstract class BurlapStub implements HessianRemoteObject {
  private static final Logger log = Log.open(BurlapStub.class);
  
  protected String _url;
  protected transient Path _urlPath;
  protected transient BurlapClientContainer _client;
  protected transient HessianRemoteResolver _resolver;

  /**
   * Initializes the stub with its remote information.
   *
   * @param client the client container
   * @param handle the corresponding handle for the stub
   */
  void _init(String url, BurlapClientContainer client)
  {
    _url = url;
    _urlPath = Vfs.lookup(url);

    _burlap_setClientContainer(client);

    if (log.isLoggable(Level.FINER))
      log.finer("burlap stub:" + _urlPath + " " + this);
  }

  /* XXX:
  public String getBurlapURL()
  {
    return _url;
  }
  */
  public String getHessianURL()
  {
    return _url;
  }

  void _burlap_setURLPath(Path urlPath)
  {
    _urlPath = urlPath;
    
    if (log.isLoggable(Level.FINER))
      log.finer("burlap setURL:" + _urlPath + " " + this);

    _url = _urlPath.getURL();
  }

  void _burlap_setClientContainer(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
    if (resolver instanceof BurlapClientContainer)
      _client = (BurlapClientContainer) resolver;
  }

  protected BurlapWriter _burlap_openWriter()
    throws RemoteException
  {
    try {
      ReadWritePair pair = _urlPath.openReadWrite();
      ReadStream is = pair.getReadStream();
      WriteStream os = pair.getWriteStream();

      if (_client != null) {
	String auth = _client.getBasicAuthentication();
	if (auth != null)
	  os.setAttribute("Authorization", auth);
      }

      BurlapWriter out = new BurlapWriter(is, os);

      out.setRemoteResolver(_resolver);

      return out;
    } catch (IOException e) {
      throw new RemoteException(String.valueOf(e));
    }
  }

  protected void _burlap_freeWriter(BurlapWriter out)
    throws IOException
  {
    out.close();
  }
}
