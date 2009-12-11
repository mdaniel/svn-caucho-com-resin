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

package com.caucho.ejb.session;

import java.util.logging.Logger;

import com.caucho.ejb.server.EjbProducer;
import com.caucho.util.FreeList;
import com.caucho.util.L10N;

/**
 * Pool of stateless session beans.
 */
public class StatelessPool<T> {
  private static final L10N L = new L10N(StatelessPool.class);

  private static Logger log
    = Logger.getLogger(StatelessPool.class.getName());
  
  private final FreeList<T> _freeList;

  private EjbProducer<T> _ejbProducer;
  
  StatelessPool(EjbProducer<T> ejbProducer)
  {
    _ejbProducer = ejbProducer;
    
    _freeList = new FreeList<T>(16);
  }
  
  public T allocate()
  {
    T bean = _freeList.allocate();
    
    if (bean == null) {
      bean = _ejbProducer.newInstance();
    }
    
    return bean;
  }

  public void free(T bean)
  {
    if (! _freeList.free(bean)) {
      destroy(bean);
    }
  }
  
  public void destroy(T bean)
  {
    _ejbProducer.destroyInstance(bean);
  }
  
  public void destroy()
  {
    T bean;
    
    while ((bean = _freeList.allocate()) != null) {
      destroy(bean);
    }
  }
}
