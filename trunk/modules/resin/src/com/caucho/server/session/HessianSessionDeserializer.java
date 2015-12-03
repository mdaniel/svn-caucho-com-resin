/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.session;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianDebugInputStream;
import com.caucho.hessian.io.SerializerFactory;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializer for session data
 */
public class HessianSessionDeserializer extends SessionDeserializer {
  private static final Logger log
    = Logger.getLogger(HessianSessionSerializer.class.getName());
  
  private Hessian2Input _in;

  public HessianSessionDeserializer(InputStream is)
  {
    this(is, Thread.currentThread().getContextClassLoader());
  }
                                    
  public HessianSessionDeserializer(InputStream is,
                                    ClassLoader loader)
  {
    if (log.isLoggable(Level.FINEST)) {
      HessianDebugInputStream dis
        = new HessianDebugInputStream(is, log, Level.FINEST);
      log.finest("session serialized load data:");
      dis.setDepth(2);
      is = dis;
    }
  
    _in = new Hessian2Input(is);
    _in.setSerializerFactory(new SerializerFactory(loader));
  }
  
  public int readInt()
    throws IOException
  {
    return _in.readInt();
  }

  public Object readObject()
    throws IOException
  {
    return _in.readObject();
  }

  public void close()
  {
    Hessian2Input in = _in;
    _in = null;

    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }
}
