/*
 * Copyright (c) 1998-2017 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(TM)
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
 * along with Resin; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.env.actor2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.caucho.env.actor.ActorProcessor;


/**
 * Blocking queue with a processor.
 */
public interface QueueRing<M>
  extends BlockingQueue<M>
{
  long head();

  /**
   * Offer a new message to the queue.
   *
   * A following {@code wake()} will be required because offer
   * does not automatically wake the consumer
   *
   * @param value the next message
   * @param timeout offer timeout
   * @param unit units for the offer timeout
   * @return true if the offer succeeds
   */
  @Override
  boolean offer(M value,
                long timeout,
                TimeUnit unit);

  /**
   * Wake the worker to process new messages.
   */
  void wake();

  /**
   * Deliver available messages to the delivery handler.
   *
   * @param deliver handler to process the message
   * @param outbox message context
   */
  void deliver(ActorProcessor<? super M> deliver)
    throws Exception;
}
