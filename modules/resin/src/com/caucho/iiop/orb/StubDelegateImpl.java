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

import org.omg.CORBA.*;

import java.util.logging.Logger;
import java.io.*;

import com.caucho.iiop.*;
import com.caucho.vfs.*;

public class StubDelegateImpl extends org.omg.CORBA.portable.Delegate
{
  public static final Logger log
    = Logger.getLogger(StubDelegateImpl.class.getName());
  
  private ORBImpl _orb;
  private byte []_oid;

  StubDelegateImpl(ORBImpl orb)
  {
    _orb = orb;
  }

  StubDelegateImpl(ORBImpl orb, byte []oid)
  {
    _orb = orb;
    _oid = oid;
  }

  @Override
  public org.omg.CORBA.portable.OutputStream
    request(org.omg.CORBA.Object self,
	    String op,
	    boolean isResponseExpected)
  {
    try {
      Iiop10Writer writer = new Iiop10Writer();

      writer.setOrb(_orb);

      IiopSocketPool pool = _orb.getSocketPool();
      
      ReadWritePair pair = pool.open();

      MessageWriter out = new StreamMessageWriter(pair.getWriteStream());

      IiopReader is = new IiopReader(pool, pair);

      is.setOrb(_orb);
    
      writer.init(out, is);

      byte []oid;

      if (self instanceof StubImpl)
	oid = ((StubImpl) self).getOid();
      else {
	oid = _oid;
      }

      writer.startRequest(oid, 0, oid.length, op, 1);

      return writer;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public org.omg.CORBA.portable.InputStream
    invoke(org.omg.CORBA.Object self,
	   org.omg.CORBA.portable.OutputStream os)
  {
    try {
      IiopWriter writer = (IiopWriter) os;

      IiopReader reader = writer._call();

      return reader;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
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
    if (obj instanceof StubImpl) {
      String typeId = ((StubImpl) obj).getIOR().getTypeId();

      if (typeId.equals(repId))
	return true;
    }
    
    System.out.println("IS-A: " + obj + " " + repId);

    return false;
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
  }

  @Override
  public void releaseReply(org.omg.CORBA.Object self,
			   org.omg.CORBA.portable.InputStream is)
  {
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
