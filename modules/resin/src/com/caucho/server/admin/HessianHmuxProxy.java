/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.server.admin;

import java.io.*;
import java.util.*;

import java.lang.reflect.*;

import com.caucho.hessian.io.*;
import com.caucho.hessian.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Proxy implementation for Hessian clients.  Applications will generally
 * use HessianProxyFactory to create proxy clients.
 */
public class HessianHmuxProxy implements InvocationHandler {
  private Path _path;
  
  private HessianHmuxProxy(Path url)
  {
    _path = url;
  }
  
  public static <X> X create(Path url, Class<X> api)
  {
    Thread thread = Thread.currentThread();
    
    return (X) Proxy.newProxyInstance(thread.getContextClassLoader(),
				      new Class[] { api },
				      new HessianHmuxProxy(url));
  }

  /**
   * Handles the object invocation.
   *
   * @param proxy the proxy object to invoke
   * @param method the method to call
   * @param args the arguments to the proxy object
   */
  public Object invoke(Object proxy, Method method, Object []args)
    throws Throwable
  {
    String methodName = method.getName();
    Class []params = method.getParameterTypes();

    // equals and hashCode are special cased
    if (methodName.equals("equals") &&
        params.length == 1 && params[0].equals(Object.class)) {
      Object value = args[0];
      if (value == null || ! Proxy.isProxyClass(value.getClass()))
        return new Boolean(false);

      HessianHmuxProxy handler = (HessianHmuxProxy) Proxy.getInvocationHandler(value);

      return new Boolean(_path.equals(handler._path));
    }
    else if (methodName.equals("hashCode") && params.length == 0)
      return new Integer(_path.hashCode());
    else if (methodName.equals("getHessianType"))
      return proxy.getClass().getInterfaces()[0].getName();
    else if (methodName.equals("getHessianURL"))
      return _path.toString();
    else if (methodName.equals("toString") && params.length == 0)
      return "HessianHmuxProxy[" + _path + "]";

    ReadStream is = null;
    
    try {
      if (args != null)
        methodName = methodName + "__" + args.length;
      else
        methodName = methodName + "__0";

      is = sendRequest(methodName, args);

      String code = (String) is.getAttribute("status");

      if (! "200".equals(code)) {
	CharBuffer sb = new CharBuffer();

	while (is.readLine(sb)) {
	}

	throw new HessianProtocolException(code + ": " + sb);
      }

      AbstractHessianInput in = new Hessian2Input(is);

      return in.readReply(method.getReturnType());
    } catch (HessianProtocolException e) {
      throw new RuntimeException(e);
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (Throwable e) {
      }
    }
  }

  private ReadStream sendRequest(String methodName, Object []args)
    throws IOException
  {
    ReadWritePair pair = _path.openReadWrite();

    ReadStream is = pair.getReadStream();
    WriteStream os = pair.getWriteStream();

    try {
      AbstractHessianOutput out = new HessianOutput(os);

      out.call(methodName, args);
      out.flush();

      return is;
    } catch (IOException e) {
      try {
	os.close();
      } catch (Exception e1) {
      }
      
      try {
	is.close();
      } catch (Exception e1) {
      }

      throw e;
    } catch (RuntimeException e) {
      try {
	os.close();
      } catch (Exception e1) {
      }
      
      try {
	is.close();
      } catch (Exception e1) {
      }

      throw e;
    }
  }
}
