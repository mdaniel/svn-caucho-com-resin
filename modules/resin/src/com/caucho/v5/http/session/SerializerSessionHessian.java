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

package com.caucho.v5.http.session;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.hessian.io.Hessian2Output;
import com.caucho.v5.hessian.io.HessianDebugOutputStream;
import com.caucho.v5.hessian.io.SerializerFactory;
import com.caucho.v5.util.Murmur64OutputStream;

/**
 * Serializer for session data
 */
public class SerializerSessionHessian extends SerializerSession {
  private static final Logger log
    = Logger.getLogger(SerializerSessionHessian.class.getName());
  
  private Hessian2Output _out;
  private long _hash;

  private Murmur64OutputStream _dout;

  public SerializerSessionHessian(OutputStream os)
  {
    this(os, Thread.currentThread().getContextClassLoader());
  }

  public SerializerSessionHessian(OutputStream os,
                                  ClassLoader loader)
  {
    this(os, new SerializerFactory(loader));
  }

  public SerializerSessionHessian(OutputStream os,
                                  SerializerFactory factory)
  {
    _dout = new Murmur64OutputStream(os);
    
    os = _dout;
    
    if (log.isLoggable(Level.FINEST)) {
      os = new HessianDebugOutputStream(os, log, Level.FINEST);
    }
    
    _out = new Hessian2Output(os);
    _out.setSerializerFactory(factory);
  }

  public void setSerializeCollectionType(boolean isEnable)
  {
    _out.getSerializerFactory().setSendCollectionType(isEnable);
  }
  
  @Override
  public void writeInt(int v)
    throws IOException
  {
    _out.writeInt(v);
  }

  @Override
  public void writeObject(Object v)
    throws IOException
  {
    _out.writeObject(v);
  }
  
  @Override
  public long getHash()
  {
    flush();
    
    return _dout.getHash();
  }
  
  private void flush()
  {
    Hessian2Output out = _out;

    if (out != null) {
      try {
        out.flush();
      } catch (IOException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }

  @Override
  public void close()
  {
    Hessian2Output out = _out;
    _out = null;

    if (out != null) {
      try {
        out.close();
      } catch (IOException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }
}
