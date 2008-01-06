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

package com.caucho.iiop.orb;

import com.caucho.iiop.*;

import org.omg.CORBA.*;

import java.util.logging.Logger;

public class StubImpl extends javax.rmi.CORBA.Stub
{
  public static final Logger log
    = Logger.getLogger(StubImpl.class.getName());
  
  private ORBImpl _orb;
  private IOR _ior;

  StubImpl(ORBImpl orb)
  {
    _orb = orb;

    _set_delegate(orb.getStubDelegate());
  }

  public StubImpl(ORBImpl orb, IOR ior)
  {
    _orb = orb;
    _ior = ior;

    _set_delegate(new StubDelegateImpl(orb, ior.getOid()));
  }

  public String []_ids()
  {
    throw new UnsupportedOperationException();
  }

  public IOR getIOR()
  {
    return _ior;
  }

  public byte []getOid()
  {
    if (_ior != null)
      return _ior.getOid();
    else
      return "INIT".getBytes();
  }

  public ORBImpl getORBImpl()
  {
    return _orb;
  }

  public String toString()
  {
    if (_ior != null)
      return "StubImpl[" + _ior + "]";
    else
      return "StubImpl[]";
  }
}
