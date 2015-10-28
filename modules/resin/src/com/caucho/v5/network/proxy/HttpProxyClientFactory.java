/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Paul Cowan
 */

package com.caucho.v5.network.proxy;

import java.lang.reflect.Constructor;
import java.util.logging.*;

import com.caucho.v5.http.proxy.LoadBalanceManager;

public class HttpProxyClientFactory
{
  private static Logger log
    = Logger.getLogger(HttpProxyClientFactory.class.getName());

  public HttpProxyClient create(LoadBalanceManager loadBalancer)
  {
    try {
      Class<?> cls = Class.forName("com.caucho.network.proxy.ProHttpProxyClient");
      Constructor<?> cons = cls.getConstructor(LoadBalanceManager.class);
      return (HttpProxyClient) cons.newInstance(loadBalancer);
    } catch (Throwable e) {
      log.log(Level.FINEST, e.toString(), e);
      return new HttpProxyClient(loadBalancer);
    }
  }
}
