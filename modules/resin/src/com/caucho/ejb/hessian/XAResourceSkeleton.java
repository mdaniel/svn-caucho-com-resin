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

package com.caucho.ejb.hessian;

import com.caucho.ejb.protocol.Skeleton;
import com.caucho.ejb.xa.EjbTransactionManager;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.HessianProtocolException;

import javax.transaction.xa.XAResource;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Skeleton for XA Resource.
 */
public class XAResourceSkeleton extends Skeleton {
  protected static final Logger log
    = Logger.getLogger(XAResourceSkeleton.class.getName());

  private EjbTransactionManager _tm;
  
  XAResourceSkeleton(EjbTransactionManager tm)
  {
    _tm = tm;
  }

  /**
   * Services the request.
   */
  public void _service(InputStream is, OutputStream os)
    throws Exception
  {
    HessianInput in = new HessianReader(is);
    HessianOutput out = new HessianWriter(os);

    in.startCall();

    String method = in.getMethod();

    try {
      if (method.equals("commitOnePhase") ||
          method.equals("commitOnePhase_string") ||
          method.equals("commitOnePhase_1")) {
        executeCommitOnePhase(in, out);
      }
      else if (method.equals("commit") ||
	       method.equals("commit_string") ||
	       method.equals("commit_1")) {
        executeCommit(in, out);
      }
      else if (method.equals("rollback") ||
	       method.equals("rollback_string") ||
	       method.equals("rollback_1")) {
        executeRollback(in, out);
      }
      else if (method.equals("prepare") ||
	       method.equals("prepare_string") ||
	       method.equals("prepare_1")) {
        executePrepare(in, out);
      }
      else
        executeUnknown(method, in, out);
    } catch (HessianProtocolException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      out.startReply();
      out.writeFault("ServiceException", e.getMessage(), e);
      out.completeReply();
    }
  }

  private void executeCommitOnePhase(HessianInput in, HessianOutput out)
    throws Throwable
  {
    String xid = in.readString();
    in.completeCall();

    _tm.commitTransaction(xid);

    out.startReply();
    out.writeNull();
    out.completeReply();
  }

  private void executeCommit(HessianInput in, HessianOutput out)
    throws Throwable
  {
    String xid = in.readString();
    in.completeCall();

    _tm.commitTransaction(xid);

    out.startReply();
    out.writeNull();
    out.completeReply();
  }

  private void executePrepare(HessianInput in, HessianOutput out)
    throws Throwable
  {
    String xid = in.readString();
    in.completeCall();

    // XXX: _tm.commitTransaction(xid);

    out.startReply();
    out.writeInt(XAResource.XA_OK);
    out.completeReply();
  }

  private void executeRollback(HessianInput in, HessianOutput out)
    throws Throwable
  {
    String xid = in.readString();
    in.completeCall();

    _tm.rollbackTransaction(xid);
    
    out.startReply();
    out.writeNull();
    out.completeReply();
  }
	   
  /**
   * Executes an unknown method.
   *
   * @param method the method name to match.
   * @param in the hessian input stream
   * @param out the hessian output stream
   */
  protected void executeUnknown(String method,
                                HessianInput in, HessianOutput out)
    throws Exception
  {
    if (method.equals("_hessian_getAttribute")) {
      String key = in.readString();
      in.completeCall();

      out.startReply();

      /*
      if ("java.api.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.home.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.object.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("home-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("remote-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else
        out.writeNull();
      */
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
