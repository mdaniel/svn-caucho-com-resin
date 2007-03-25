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

package com.caucho.iiop.marshal;

import java.lang.reflect.Array;

/**
 * Proxy implementation for ORB clients.
 */
public class ArrayMarshal extends Marshal
{
  private final Class _component;
  private final Marshal _subMarshal;

  public ArrayMarshal(Class component, Marshal subMarshal)
  {
    _component = component;
    _subMarshal = subMarshal;
  }

  @Override
  public void marshal(org.omg.CORBA_2_3.portable.OutputStream os,
                      Object value)
  {
    Object []obj = (Object []) value;

    int len = obj.length;

    os.write_long(len);
    for (int i = 0; i < len; i++) {
      _subMarshal.marshal(os, obj[i]);
    }
  }

  @Override
  public Object unmarshal(org.omg.CORBA_2_3.portable.InputStream is)
  {
    try {
      //((com.caucho.iiop.IiopReader) is).alignMethodArgs();
      //int junk = is.read_long();  // XXX: encapsulated sequence length?
      int len = is.read_long();

      if (len < 0 || len >= 65536)
	throw new RuntimeException("sequence too long:" + len);

      Object []obj = (Object []) Array.newInstance(_component, len);

      for (int i = 0; i < len; i++) {
	obj[i] = _subMarshal.unmarshal(is);
      }

      return obj;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
