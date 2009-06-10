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
 * @author Scott Ferguson;
 */

package com.caucho.config.event;

import java.lang.annotation.Annotation;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observer;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.TypeLiteral;

public class EventImpl<T> implements Event<T>
{
  private final BeanManager _manager;
  private final Class _eventType;
  private final Annotation []_bindings;

  public EventImpl(BeanManager manager,
		   Class eventType,
		   Annotation []bindings)
  {
    _manager = manager;
    _eventType = eventType;
    _bindings = bindings;
  }
  
  public void fire(T event, Annotation... bindings)
  {
    if (_bindings.length == 0)
      _manager.fireEvent(event, bindings);
    else if (bindings == null || bindings.length == 0)
      _manager.fireEvent(event, _bindings);
    else {
      Annotation []fullBindings
	= new Annotation[_bindings.length + bindings.length];

      System.arraycopy(_bindings, 0, fullBindings, 0, _bindings.length);
      System.arraycopy(bindings, 0, fullBindings, _bindings.length,
		       bindings.length);

      _manager.fireEvent(event, fullBindings);
    }
  }

  public void addObserver(Observer<T> observer)
  {
    _manager.addObserver(observer);
  }

  public void removeObserver(Observer<T> observer)
  {
    _manager.removeObserver(observer);
  }

  public Event<T> select(Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public <U extends T> Event<U> select(Class<U> subtype,
				       Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public <U extends T> Event<U> select(TypeLiteral<U> subtype,
				       Annotation... bindings)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
