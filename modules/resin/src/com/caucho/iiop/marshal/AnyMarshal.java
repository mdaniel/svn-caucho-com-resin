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

import javax.rmi.CORBA.Util;

/**
 * Proxy implementation for ORB clients.
 */
public class AnyMarshal extends Marshal {
  public static final Marshal MARSHAL = new AnyMarshal();

  @Override
  public void marshal(org.omg.CORBA_2_3.portable.OutputStream os,
                      Object value)
  {
    // ejb/1261
    //Util.writeAny(os, value);
    os.write_value((java.io.Serializable) value);
  }

  @Override
  public Object unmarshal(org.omg.CORBA_2_3.portable.InputStream is)
  {
    //return Util.readAny(is);
    return is.read_value();
  }
}
