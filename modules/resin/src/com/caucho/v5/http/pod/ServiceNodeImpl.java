/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.pod;

import io.baratine.service.ServiceNode;

import com.caucho.v5.bartender.pod.NodePodAmp;


/**
 * The service node metagata.
 */
public class ServiceNodeImpl implements ServiceNode
{
  private NodePodAmp _node;

  public ServiceNodeImpl(NodePodAmp node)
  {
    _node = node;
  }

  @Override
  public String podName()
  {
    return _node.pod().name();
  }

  @Override
  public int nodeCount()
  {
    return _node.pod().nodeCount();
  }

  @Override
  public int nodeIndex()
  {
    return _node.nodeIndex();
  }
  
  /*
  @Override
  public int hash(String path)
  {
    return _node.hash(path);
  }
  */
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _node.podName() + ":" + _node.nodeIndex() + "]"; 
  }
}
