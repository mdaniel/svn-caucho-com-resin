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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializer for session data
 */
public class JavaSessionDeserializer extends SessionDeserializer {
  private static final Logger log
    = Logger.getLogger(JavaSessionSerializer.class.getName());
  
  private ObjectInputStream _in;

  public JavaSessionDeserializer(InputStream is,
                                 ClassLoader loader)
    throws IOException
  {
    // is = new DebugInputStream(is);
    
    _in = new ContextObjectInputStream(is, loader);
  }
  
  @Override
  public int readInt()
    throws IOException
  {
    return _in.readInt();
  }

  @Override
  public Object readObject()
    throws IOException, ClassNotFoundException
  {
    return _in.readObject();
  }

  @Override
  public void close()
  {
    ObjectInputStream in = _in;
    _in = null;

    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }

  static class ContextObjectInputStream extends ObjectInputStream {
    private ClassLoader _loader;
    
    ContextObjectInputStream(InputStream is, ClassLoader loader)
      throws IOException
    {
      super(is);

      _loader = loader;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass v)
      throws IOException, ClassNotFoundException
    {
      String name = v.getName();

      return Class.forName(name, false, _loader);
    }
  }
}
