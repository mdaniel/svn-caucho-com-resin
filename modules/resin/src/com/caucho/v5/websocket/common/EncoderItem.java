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

package com.caucho.v5.websocket.common;

import javax.websocket.Encoder;


/**
 * websocket client container
 */

public class EncoderItem<T,E extends Encoder>
{
  private Class<T> _type;
  private E _coder;
  
  public EncoderItem(E coder, Class<E> coderType)
  {
    _coder = coder;
    
    _type = (Class<T>) TypeUtil.getParameterType(coder.getClass(), coderType);
  }
  
  public E getCoder()
  {
    return _coder;
  }
  
  public int getCost(Class<?> implClass)
  {
    return getCost(implClass, 0);
  }
  
  public int getCost(Class<?> implClass, int depth)
  {
    if (implClass == null) {
      return Integer.MAX_VALUE;
    }
    
    if (implClass.equals(_type)) {
      return depth;
    }
    
    int cost = getCost(implClass.getSuperclass(), depth + 1);
    
    for (Class<?> api : implClass.getInterfaces()) {
      cost = Math.min(cost, getCost(api, depth + 1));
    }
    
    return cost;
  }

  public void destroy()
  {
    _coder.destroy();
  }
}
