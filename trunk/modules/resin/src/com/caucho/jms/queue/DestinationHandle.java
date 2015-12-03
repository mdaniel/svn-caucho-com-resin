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
 * @author Scott Ferguson
 */

package com.caucho.jms.queue;

import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.jms.memory.*;

import com.caucho.util.*;

/**
 * Implements an abstract queue.
 */
public class DestinationHandle implements Destination, java.io.Serializable
{
  private String _name;
  
  public DestinationHandle()
  {
  }
  
  public DestinationHandle(String name)
  {
    _name = name;
  }

  public Object readResolve()
  {
    MemoryQueue queue = new MemoryQueue();
    queue.setName(_name);
    
    return queue;
  }

  @Override
  public String toString()
  {
    return "DestinationHandle[" + _name + "]";
  }
}

