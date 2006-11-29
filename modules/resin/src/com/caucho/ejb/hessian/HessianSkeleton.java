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

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.Skeleton;
import com.caucho.ejb.xa.TransactionContext;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for any bean skeleton capable of handling a Hessian request.
 *
 * <p/>Once selected, the calling servlet will dispatch the request through
 * the <code>_service</code> call.  After parsing the request headers,
 * <code>_service</code> calls the generated entry <code>_execute</code>
 * to execute the request.
 */
abstract public class HessianSkeleton extends Skeleton {
  protected static final Logger log = Log.open(HessianSkeleton.class);

  private AbstractServer _server;
  private HessianRemoteResolver _resolver;

  private boolean _isDebug;

  void _setServer(AbstractServer server)
  {
    _server = server;
  }

  public void setDebug(boolean isDebug)
  {
    _isDebug = isDebug;
  }
  
  /**
   * Sets the hessian resolver.
   */
  void _setResolver(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
  }

  abstract protected void _setObject(Object object);

  public void _service(InputStream is, OutputStream os)
    throws Exception
  {
    java.io.StringWriter debugWriter = null;

    if (_isDebug) {
      debugWriter = new java.io.StringWriter();
      is = new HessianDebugInputStream(is, new PrintWriter(debugWriter));
    }
    
    HessianInput in = new HessianReader(is);
    HessianOutput out = new HessianWriter(os);

    in.setRemoteResolver(_resolver);
    in.readCall();

    String xid = null;
    String header;
    while ((header = in.readHeader()) != null) {
      Object value = in.readObject();

      if ("xid".equals(header)) {
	xid = (String) value;
      }
    }

    String method = in.readMethod();

    CharBuffer cb = new CharBuffer();
    cb.append(method);
    
    String oldProtocol = EjbProtocolManager.setThreadProtocol("hessian");
    
    try {
      TransactionContext xa = null;
      
      if (xid != null)
	xa = _server.getTransactionManager().startTransaction(xid);
	
      _execute(cb, in, out, xa);
    } catch (HessianProtocolException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);

      out.startReply();
      out.writeFault("ServiceException", e.getMessage(), e);
      out.completeReply();
    } finally {
      EjbProtocolManager.setThreadProtocol(oldProtocol);

      if (debugWriter != null)
	log.fine(debugWriter.toString());
      
      if (xid != null)
	_server.getTransactionManager().finishTransaction(xid);
    }
  }

  protected void startReply(HessianOutput out, TransactionContext xa)
    throws IOException
  {
    out.startReply();

    if (xa != null && ! xa.isEmpty()) {
      EjbProtocolManager pm = _server.getServerManager().getProtocolManager();
      HessianProtocol hessian = (HessianProtocol) pm.getProtocol("hessian");

      if (hessian != null) {
	out.writeHeader("xa-resource");
	out.writeString(hessian.calculateURL("/_ejb_xa_resource"));
      }
    }
  }

  abstract protected void _execute(CharBuffer method,
                                   HessianInput in,
                                   HessianOutput out,
				   TransactionContext xa)
    throws Throwable;
  
  protected void _executeUnknown(CharBuffer method,
                                 HessianInput in,
                                 HessianOutput out)
    throws Exception
  {
    if (method.matches("_hessian_getAttribute")) {
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


