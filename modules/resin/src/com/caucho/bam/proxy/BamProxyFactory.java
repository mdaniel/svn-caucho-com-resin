/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.bam.proxy;

import java.lang.reflect.Proxy;

import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.broker.Broker;

/**
 * The Skeleton introspects and dispatches messages for a
 * {@link com.caucho.bam.actor.SimpleActor}
 * or {@link com.caucho.bam.actor.SkeletonActorFilter}.
 */
public class BamProxyFactory
{
  public static <T> T createProxy(Class<T> api, 
                                  String to,
                                  String from,
                                  Broker broker)
  {
    BamProxyHandler handler = new BamProxyHandler(api, to, from, broker);
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return (T) Proxy.newProxyInstance(loader, 
                                      new Class[] { api },
                                      handler);
  }
  
  public static <T> T createProxy(Class<T> api,
                                  String to,
                                  ActorSender sender)
  {
    BamProxyHandler handler = new BamProxyHandler(api, to, sender);
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    return (T) Proxy.newProxyInstance(loader, 
                                      new Class[] { api },
                                      handler);
  }
}