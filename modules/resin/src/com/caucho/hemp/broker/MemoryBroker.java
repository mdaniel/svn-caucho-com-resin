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

package com.caucho.hemp.broker;

import java.util.*;
import java.util.logging.*;

import com.caucho.hemp.*;

/**
 * Manages destinations
 */
public class MemoryBroker implements Broker {
  private static final Logger log
    = Logger.getLogger(MemoryBroker.class.getName());
  
  private HashMap<String,Target> _targetMap
    = new HashMap<String,Target>();
  
  /**
   * Sends a message to a target
   */
  public void sendMessage(Message message)
  {
    Target target;

    synchronized (_targetMap) {
      target = _targetMap.get(message.getTo());
    }

    if (target != null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " send to=" + target + " " + message);
      target.sendMessage(message);
    }
  }

  /**
   * Registers a target
   */
  public void register(String name, Target target)
  {
    synchronized (_targetMap) {
      _targetMap.put(name, target);
    }
  }

  /**
   * Unregisters a target
   */
  public void unregister(String name)
  {
    synchronized (_targetMap) {
      _targetMap.remove(name);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
