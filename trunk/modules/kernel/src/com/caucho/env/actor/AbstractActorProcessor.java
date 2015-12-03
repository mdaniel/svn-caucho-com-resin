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

package com.caucho.env.actor;

/**
 * Processes an actor item.
 */
abstract public class AbstractActorProcessor<T> implements ActorProcessor<T>
{
  /**
   * Returns the current thread name.
   */
  @Override
  public String getThreadName()
  {
    return getClass().getSimpleName() + "-" + Thread.currentThread().getId();
  }

  /**
   * Called when all items in the queue are processed. This can be
   * used to flush buffers.
   */
  @Override
  public void onProcessStart() throws Exception
  {
  }

  /**
   * Process a single item.
   */
  abstract public void process(T item) throws Exception;

  /**
   * Called when all items in the queue are processed. This can be
   * used to flush buffers.
   */
  @Override
  public void onProcessComplete() throws Exception
  {
  }
}
