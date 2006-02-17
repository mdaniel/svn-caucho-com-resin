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

package com.caucho.ejb.burlap;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.log.Log;

import com.caucho.ejb.*;

import com.caucho.ejb.protocol.Skeleton;
import com.caucho.ejb.protocol.EjbProtocolManager;

import com.caucho.burlap.io.*;
import com.caucho.hessian.io.HessianRemoteResolver;

/**
 * Base class for any bean skeleton capable of handling a Burlap request.
 *
 * <p/>Once selected, the calling servlet will dispatch the request through
 * the <code>_service</code> call.  After parsing the request headers,
 * <code>_service</code> calls the generated entry <code>_execute</code>
 * to execute the request.
 */
abstract public class BurlapSkeleton extends Skeleton {
  protected static Logger log = Log.open(BurlapSkeleton.class);

  private AbstractServer _server;
  private HessianRemoteResolver _resolver;

  void _setServer(AbstractServer server)
  {
    _server = server;
  }
  
  /**
   * Sets the burlap resolver.
   */
  void _setResolver(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
  }

  abstract protected void _setObject(Object object);

  public void _service(InputStream is, OutputStream os)
    throws Exception
  {
    BurlapInput in = new BurlapReader(is);
    BurlapOutput out = new BurlapWriter(os);

    in.setRemoteResolver(_resolver);
    in.startCall();

    String method = in.getMethod();
    CharBuffer cb = new CharBuffer();
    cb.append(method);

    String oldProtocol = EjbProtocolManager.setThreadProtocol("burlap");
    
    try {
      _execute(cb, in, out);
    } catch (BurlapProtocolException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      out.startReply();
      out.writeFault("ServiceException", e.getMessage(), e);
      out.completeReply();
    } finally {
      EjbProtocolManager.setThreadProtocol(oldProtocol);
    }
  }

  abstract protected void _execute(CharBuffer method,
                                   BurlapInput in,
                                   BurlapOutput out)
    throws Throwable;
  
  protected void _executeUnknown(CharBuffer method,
                                 BurlapInput in,
                                 BurlapOutput out)
    throws Exception
  {
    if (method.matches("_burlap_getAttribute")) {
      String key = in.readString();
      in.completeCall();

      out.startReply();

      if ("java.api.class".equals(key))
        out.writeString(_server.getRemoteHomeClass().getName());
      else if ("java.home.class".equals(key))
        out.writeString(_server.getRemoteHomeClass().getName());
      else if ("java.object.class".equals(key))
        out.writeString(_server.getRemoteObjectClass().getName());
      else if ("home-class".equals(key))
        out.writeString(_server.getRemoteHomeClass().getName());
      else if ("remote-class".equals(key))
        out.writeString(_server.getRemoteObjectClass().getName());
      else
        out.writeNull();
      
      out.completeReply();
    }
    else {
      out.startReply();
      out.writeFault("NoMethod", "no such method: " + method, null);
      out.completeReply();
    }
  }
}


