/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.nautilus;

import java.lang.ref.SoftReference;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import com.caucho.nautilus.spi.ContainerProvider;

/**
 * Message facade for creating a connection
 */
public class Nautilus
{
  private static WeakHashMap<ClassLoader,SoftReference<ContainerProvider>>
  _containerMap = new WeakHashMap<>();
  
  private Nautilus() {}
  
  public static <M> ReceiverBuilder<M> receiver()
  {
    return getContainer().receiver();
  }
  
  public static <M> SenderBuilder<M> sender()
  {
    return getContainer().sender();
  }
  
  private static ContainerProvider getContainer()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    synchronized (_containerMap) {
      SoftReference<ContainerProvider> containerRef
        = _containerMap.get(loader);
    
      ContainerProvider container;
    
      if (containerRef != null) {
        container = containerRef.get();
      
        if (container != null) {
          return container;
        }
      }
      
      
      container = ServiceLoader.load(ContainerProvider.class).iterator().next();
      
      containerRef = new SoftReference<>(container);
      
      _containerMap.put(loader, containerRef);
      
      return container;
    }
  }
}
