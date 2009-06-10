/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.event.Observer;

/**
 * Internal implementation for a producer Bean
 */
public interface ObserverMethod<X,T> extends Observer<T>
{
  /**
   * Returns the declaring bean
   */
  public Bean<X> getParentBean();

  /**
   * Returns the observed event type
   */
  public Type getObservedEventType();
  
  /**
   * Returns the annotated method
   */
  public AnnotatedParameter<? super X> getEventParameter();

  /**
   * Returns the observed event bindings
   */
  public Set<Annotation> getObservedEventBindings();

  /**
   * Sends an event
   */
  public Listener<T> getListener();

  /**
   * Sends an event
   */
  public void setListener(Listener<T> listener);
}
