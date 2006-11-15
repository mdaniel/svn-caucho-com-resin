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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.hessian;

import java.io.*;

import java.rmi.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.transaction.xa.Xid;

import com.caucho.vfs.*;

import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.TransactionImpl;

import com.caucho.hessian.io.*;

/**
 * Base class for generated object stubs.
 */
public abstract class HessianStub implements HessianRemoteObject {
  private static final Logger log
    = Logger.getLogger(HessianStub.class.getName());
  protected String _url;
  
  protected transient Path _urlPath;
  protected transient HessianClientContainer _client;
  protected transient HessianRemoteResolver _resolver;
  
  /**
   * Initializes the stub with its remote information.
   *
   * @param client the client container
   * @param handle the corresponding handle for the stub
   */
  void _init(String url, HessianClientContainer client)
  {
    _url = url;
    _urlPath = Vfs.lookup(url);

    _hessian_setClientContainer(client);
  }
  
  public String getHessianURL()
  {
    return _url;
  }

  void _hessian_setURL(String url)
  {
    _url = url;
  }

  void _hessian_setURLPath(Path urlPath)
  {
    _urlPath = urlPath;
  }

  void _hessian_setClientContainer(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
    if (resolver instanceof HessianClientContainer)
      _client = (HessianClientContainer) resolver;
  }

  protected HessianWriter _hessian_openWriter()
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

      HessianWriter out = new HessianWriter(is, os);

      out.setRemoteResolver(_resolver);

      return out;
    } catch (IOException e) {
      throw new RemoteException(String.valueOf(e));
    }
  }

  protected void _hessian_writeHeaders(HessianWriter out)
    throws IOException
  {
    try {
      TransactionImpl xa = (TransactionImpl) TransactionManagerImpl.getInstance().getTransaction();

      if (xa != null) {
	Xid xid = xa.getXid();

	String s = xidToString(xid.getGlobalTransactionId());

	out.writeHeader("xid");
	out.writeString(s);
      }
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  protected void _hessian_freeWriter(HessianWriter out)
    throws IOException
  {
    out.close();
  }

  private static String xidToString(byte []id)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < id.length; i++) {
      byte b = id[i];

      sb.append(toHex((b >> 4) & 0xf));
      sb.append(toHex(b & 0xf));
    }

    return sb.toString();
  }

  private static char toHex(int d)
  {
    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('a' + d - 10);
  }
}
