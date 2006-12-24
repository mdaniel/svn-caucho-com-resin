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

package com.caucho.iiop.orb;

import com.caucho.iiop.marshal.ArrayMarshal;
import com.caucho.iiop.marshal.HelperMarshal;
import com.caucho.iiop.marshal.BooleanMarshal;
import com.caucho.iiop.marshal.CharMarshal;
import com.caucho.iiop.marshal.ByteMarshal;
import com.caucho.iiop.marshal.ShortMarshal;
import com.caucho.iiop.marshal.IntMarshal;
import com.caucho.iiop.marshal.LongMarshal;
import com.caucho.iiop.marshal.FloatMarshal;
import com.caucho.iiop.marshal.DoubleMarshal;
import com.caucho.iiop.marshal.PrincipalMarshal;
import com.caucho.iiop.marshal.RemoteMarshal;
import com.caucho.iiop.marshal.AnyMarshal;
import com.caucho.iiop.marshal.VoidMarshal;
import com.caucho.iiop.marshal.Marshal;

import java.util.HashMap;
import org.omg.CORBA.portable.IDLEntity;


/**
 * Proxy implementation for ORB clients.
 */
public class MarshalFactory {
  private static MarshalFactory _factory = new MarshalFactory();

  private static HashMap<Class,Marshal> _classMap
    = new HashMap<Class,Marshal>();

  public static MarshalFactory create()
  {
    return _factory;
  }

  /**
   * Creates the marshal object for an object.
   *
   * @param cl the class to marshal
   * @param isIdl if true, handle arrays as IDL sequences
   */
  public Marshal create(Class cl, boolean isIdl)
  {
    Marshal marshal = _classMap.get(cl);

    if (marshal != null)
      return marshal;
    
    if (cl.isArray() && isIdl) {
      Class compType = cl.getComponentType();
      
      Marshal subMarshal = create(compType, isIdl);

      return new ArrayMarshal(compType, subMarshal);
    }

    if (IDLEntity.class.isAssignableFrom(cl))
      return new HelperMarshal(cl);

    if (org.omg.CORBA.portable.Streamable.class.isAssignableFrom(cl))
      return new StreamableMarshal(cl);

    if (java.rmi.Remote.class.isAssignableFrom(cl))
      return new RemoteMarshal(cl);

    if (java.io.Serializable.class.isAssignableFrom(cl))
      return SerializableMarshal.MARSHAL;

    return AnyMarshal.MARSHAL;
  }

  static {
    _classMap.put(void.class, VoidMarshal.MARSHAL);
    
    _classMap.put(boolean.class, BooleanMarshal.MARSHAL);
    _classMap.put(char.class, CharMarshal.MARSHAL);
    _classMap.put(byte.class, ByteMarshal.MARSHAL);
    _classMap.put(short.class, ShortMarshal.MARSHAL);
    _classMap.put(int.class, IntMarshal.MARSHAL);
    _classMap.put(long.class, LongMarshal.MARSHAL);
    _classMap.put(float.class, FloatMarshal.MARSHAL);
    _classMap.put(double.class, DoubleMarshal.MARSHAL);
    
    _classMap.put(String.class, StringMarshal.MARSHAL);
    
    _classMap.put(org.omg.CORBA.Object.class, CorbaObjectMarshal.MARSHAL);
  }
}
