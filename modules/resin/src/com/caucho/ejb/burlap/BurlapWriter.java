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
import java.lang.reflect.*;

import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.util.*;

import com.caucho.hessian.io.HessianRemoteResolver;

import com.caucho.burlap.io.BurlapInput;
import com.caucho.burlap.io.BurlapOutput;

public class BurlapWriter extends BurlapOutput {
  private ReadStream _is;
  private HessianRemoteResolver _resolver;
  
  /**
   * Creates a new Burlap output stream, initialized with an
   * underlying output stream.
   *
   * @param os the underlying output stream.
   */
  public BurlapWriter(ReadStream is, OutputStream os)
  {
    super(os);

    _is = is;
  }
  
  /**
   * Creates a new Burlap output stream, initialized with an
   * underlying output stream.
   *
   * @param os the underlying output stream.
   */
  public BurlapWriter(OutputStream os)
  {
    super(os);
  }

  /**
   * Creates an uninitialized Burlap output stream.
   */
  public BurlapWriter()
  {
  }
  
  /**
   * Initializes the output
   */
  public void init(OutputStream os)
  {
    _serializerFactory = new BurlapSerializerFactory();

    super.init(os);
  }

  public void setRemoteResolver(HessianRemoteResolver resolver)
  {
    _resolver = resolver;
  }

  public BurlapInput doCall()
    throws Throwable
  {
    completeCall();

    String status = (String) _is.getAttribute("status");

    if (! "200".equals(status)) {
      CharBuffer cb = new CharBuffer();

      int ch;
      while ((ch = _is.readChar()) >= 0)
        cb.append((char) ch);

      throw new BurlapProtocolException("exception: " + cb);
    }

    BurlapInput in = new BurlapReader();
    in.setSerializerFactory(_serializerFactory);
    in.setRemoteResolver(_resolver);
    in.init(_is);

    in.startReply();

    return in;
  }

  public void close()
  {
    try {
      os.close();
      _is.close();
    } catch (Exception e) {
    }
  }
}
