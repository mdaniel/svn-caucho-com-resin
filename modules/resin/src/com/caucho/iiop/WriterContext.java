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

package com.caucho.iiop;

import com.caucho.iiop.orb.*;
import com.caucho.iiop.any.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.*;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;

import org.omg.CORBA.Principal;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.IndirectionException;
import org.omg.SendingContext.RunTime;

import javax.rmi.CORBA.Util;
import javax.rmi.CORBA.ValueHandler;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public final class WriterContext
{
  private IntMap _savedStrings = new IntMap();
  private IdentityIntMap _refMap = new IdentityIntMap();
  private ValueHandler _valueHandler = Util.createValueHandler();
  private RunTime _runTime = _valueHandler.getRunTimeCodeBase();


  public int getString(String v)
  {
    return _savedStrings.get(v);
  }

  public void putString(String v, int offset)
  {
    _savedStrings.put(v, offset);
  }

  public void putRef(Object v, int offset)
  {
    _refMap.put(v, offset);
  }

  public int getRef(Object v)
  {
    return _refMap.get(v);
  }

  public ValueHandler getValueHandler()
  {
    return _valueHandler;
  }

  public RunTime getRunTime()
  {
    return _runTime;
  }
}
