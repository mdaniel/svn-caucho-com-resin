/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.hessian;

import java.io.*;
import java.beans.*;
import java.lang.reflect.*;

import java.rmi.*;
import javax.ejb.*;

import com.caucho.vfs.*;
import com.caucho.java.*;
import com.caucho.util.*;
import com.caucho.ejb.*;
import com.caucho.hessian.io.*;

/**
 * Factory for creating Hessian client stubs.  The returned stub will
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

public class HessianStubFactory implements HessianRemoteResolver {
  private HessianRemoteResolver resolver;
  private Path workPath;
  
  public HessianStubFactory()
  {
    resolver = this;
  }

  /**
   * Returns the remote resolver.
   */
  public HessianRemoteResolver getRemoteResolver()
  {
    return resolver;
  }

  public void setWorkPath(Path path)
  {
    this.workPath = path;
  }

  public Path getWorkPath()
  {
    return workPath;
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

    HessianStub stub = (HessianStub) cl.newInstance();

    stub._hessian_setURLPath(Vfs.lookup(url));
    stub._hessian_setClientContainer(this);

    return stub;
  }

  public AbstractHessianInput getHessianInput(InputStream is)
  {
    AbstractHessianInput in = new HessianSerializerInput(is);
    in.setRemoteResolver(resolver);

    return in;
  }

  public HessianOutput getHessianOutput(OutputStream os)
  {
    return new HessianWriter(os);
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
    } catch (Exception e) {
      throw new IOException(String.valueOf(e));
    }
  }
}
