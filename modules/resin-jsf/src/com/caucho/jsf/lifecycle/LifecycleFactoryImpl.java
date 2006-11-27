/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.lifecycle;

import java.util.*;
import javax.faces.lifecycle.*;

/**
 * Factory class for returning JSF lifecycle.
 */
public class LifecycleFactoryImpl extends LifecycleFactory {
  private HashMap<String,Lifecycle> _lifecycleMap
    = new HashMap<String,Lifecycle>();

  public LifecycleFactoryImpl()
  {
    _lifecycleMap.put(DEFAULT_LIFECYCLE, new LifecycleImpl());
  }

  /**
   * Adds a named lifecycle to the factory.
   */
  public void addLifecycle(String name, Lifecycle lifecycle)
  {
    _lifecycleMap.put(name, lifecycle);
  }

  /**
   * Returns the named lifecycle.
   */
  public Lifecycle getLifecycle(String name)
  {
    return _lifecycleMap.get(name);
  }

  /**
   * Returns the list of the lifecycle names.
   */
  public Iterator<String> getLifecycleIds()
  {
    return _lifecycleMap.keySet().iterator();
  }

  public String toString()
  {
    return "LifecycleFactoryImpl[]";
  }
}
