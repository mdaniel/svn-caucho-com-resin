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
 * @@author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import java.io.*;
import java.beans.*;
import java.lang.reflect.*;

import java.rmi.*;
import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.server.util.*;
import com.caucho.ejb.*;

import com.caucho.burlap.io.BurlapInput;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianRemoteResolver;

/**
 * Factory for creating Burlap client stubs.  The returned stub will
 * call the remote object for all methods.
 *
 * <pre>
 * String url = "http://localhost:8080/ejb/hello";
 * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
 * </pre>
 *
 * After creation, the stub can be like a regular Java class.  Because
 * it makes remote calls, it can throw more exceptions than a Java class.
 * In particular, it may throw protocol exceptions.
 */

public class BurlapStubFactory implements HessianRemoteResolver {
  private HessianRemoteResolver _resolver;
  private Path _workPath;
  
  public BurlapStubFactory()
  {
    _resolver = this;
  }

  /**
   * Returns the remote resolver.
   */
  public HessianRemoteResolver getRemoteResolver()
  {
    return _resolver;
  }

  public void setWorkPath(Path path)
  {
    _workPath = path;
  }

  public Path getWorkPath()
  {
    return _workPath;
  }

  /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @@param api the interface the proxy class needs to implement
   * @@param url the URL where the client object is located.
   *
   * @@return a proxy to the object with the specified interface.
   */
  public Object create(Class api, String url)
    throws Exception
  {
    StubGenerator gen = new StubGenerator();
    gen.setClassDir(getWorkPath());
      
    Class cl = gen.createStub(api);

    BurlapStub stub = (BurlapStub) cl.newInstance();

    stub._burlap_setURLPath(Vfs.lookup(url));
    stub._burlap_setClientContainer(this);

    return stub;
  }

  public AbstractHessianInput getBurlapInput(InputStream is)
  {
    AbstractHessianInput in = new BurlapInput(is);
    in.setRemoteResolver(_resolver);

    return in;
  }

  public AbstractHessianOutput getBurlapOutput(OutputStream os)
  {
    return new BurlapWriter(os);
  }
  
  /**
   * Looks up a proxy object.
   */
  public Object lookup(String type, String url)
    throws IOException
  {
    try {
      Class api = CauchoSystem.loadClass(type);

      return create(api, url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      IOException e1 = new IOException(e.toString());
      e1.initCause(e);

      throw e1;
    }
  }
}
