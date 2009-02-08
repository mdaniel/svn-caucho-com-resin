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

package com.caucho.ejb.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.hessian.io.HessianRemote;
import com.caucho.hessian.io.HessianSerializerOutput;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.*;

public class HessianWriter extends HessianSerializerOutput {
  private static final Logger log
    = Logger.getLogger(HessianWriter.class.getName());
  
  private InputStream _is;
  private HessianRemoteResolver _resolver;
  
  /**
   * Creates a new Hessian output stream, initialized with an
   * underlying output stream.
   *
   * @param os the underlying output stream.
   */
  public HessianWriter(InputStream is, OutputStream os)
  {
    super(os);

    _is = is;
  }
  
  /**
   * Creates a new Hessian output stream, initialized with an
   * underlying output stream.
   *
   * @param os the underlying output stream.
   */
  public HessianWriter(OutputStream os)
  {
    super(os);
  }

  /**
   * Creates an uninitialized Hessian output stream.
   */
  public HessianWriter()
  {
  }
  
  /**
   * Initializes the output
   */
  public void init(OutputStream os)
  {
    _serializerFactory = new QSerializerFactory();

    super.init(os);
  }

  public void setRemoteResolver(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
  }

  public Hessian2Input doCall()
    throws Throwable
  {
    completeCall();

    if (! (_is instanceof ReadStream))
      throw new IllegalStateException("Hessian call requires ReadStream");

    ReadStream is = (ReadStream) _is;

    String status = (String) is.getAttribute("status");

    if (! "200".equals(status)) {
      CharBuffer cb = new CharBuffer();

      int ch;
      while ((ch = is.readChar()) >= 0)
        cb.append((char) ch);

      throw new HessianProtocolException("exception: " + cb);
    }

    Hessian2Input in = new HessianReader();
    in.setSerializerFactory(_serializerFactory);
    in.setRemoteResolver(_resolver);
    in.init(_is);

    in.startReply();

    String header;

    while ((header = in.readHeader()) != null) {
      Object value = in.readObject();

      if (header.equals("xa-resource")) {
	TransactionImpl xa = (TransactionImpl) TransactionManagerImpl.getInstance().getTransaction();

	if (xa != null) {
	  HessianXAResource xaRes = new HessianXAResource((String) value);

	  xa.enlistResource(xaRes);
	}
      }
    }

    return in;
  }

  public void close()
  {
    try {
      super.close();

      if (_is != null)
	_is.close();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  /**
   * Applications which override this can do custom serialization.
   *
   * @param object the object to write.
   */
  public void writeObjectImpl(Object obj)
    throws IOException
  {
    if (obj instanceof EJBObject) {
      EJBObject ejbObject = (EJBObject) obj;
      EJBHome ejbHome = ejbObject.getEJBHome();
      
      Handle handle = ejbObject.getHandle();
      
      if (handle instanceof HessianHandle) {
        HessianHandle hessianHandle = (HessianHandle) handle;

        Class api = ejbHome.getEJBMetaData().getRemoteInterfaceClass();

	HessianRemote remote
	  = new HessianRemote(api.getName(), hessianHandle.getURL());

        writeObjectImpl(remote);
        return;
      }
    }
    else if (obj instanceof EJBHome) {
      EJBHome ejbHome = (EJBHome) obj;
      
      HomeHandle handle = ejbHome.getHomeHandle();
      
      if (handle instanceof HessianHomeHandle) {
        HessianHomeHandle hessianHandle = (HessianHomeHandle) handle;

        Class api = ejbHome.getEJBMetaData().getHomeInterfaceClass();

	HessianRemote remote
	  = new HessianRemote(api.getName(), hessianHandle.getURL("hessian"));

        writeObjectImpl(remote);
	
        return;
      }
    }
    
    super.writeObjectImpl(obj);
  }
}
