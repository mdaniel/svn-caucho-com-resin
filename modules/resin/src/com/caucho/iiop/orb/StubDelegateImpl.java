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

import java.applet.Applet;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.rmi.*;
import javax.rmi.CORBA.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import org.omg.CORBA.*;

public class StubDelegateImpl extends org.omg.CORBA.portable.Delegate
{
  public static final Logger log
    = Logger.getLogger(StubDelegateImpl.class.getName());
  
  private ORBImpl _orb;

  StubDelegateImpl(ORBImpl orb)
  {
    _orb = orb;
  }

  @Override
  public Request create_request(org.omg.CORBA.Object obj,
				Context ctx,
				String op,
				NVList argList,
				NamedValue result)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Request create_request(org.omg.CORBA.Object obj,
				Context ctx,
				String op,
				NVList argList,
				NamedValue result,
				ExceptionList excList,
				ContextList ctxList)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public org.omg.CORBA.Object duplicate(org.omg.CORBA.Object obj)
  {
    return obj;
  }

  @Override
  public org.omg.CORBA.Object get_interface_def(org.omg.CORBA.Object self)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hash(org.omg.CORBA.Object obj, int max)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean is_a(org.omg.CORBA.Object obj, String repId)
  {
    System.out.println("IS-A: " + obj + " " + repId);
    Thread.dumpStack();

    return true;
  }

  @Override
  public boolean is_equivalent(org.omg.CORBA.Object obj,
			       org.omg.CORBA.Object other)
  {
    return obj == other;
  }

  @Override
  public boolean non_existent(org.omg.CORBA.Object obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ORB orb(org.omg.CORBA.Object obj)
  {
    return _orb;
  }

  @Override
  public void release(org.omg.CORBA.Object obj)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Request request(org.omg.CORBA.Object obj, String op)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "StubDelegateImpl[]";
  }
}
