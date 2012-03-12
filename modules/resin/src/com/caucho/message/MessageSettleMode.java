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

package com.caucho.message;

/**
 * Selects when a message is considered delivered and can be considered
 * settled and forgotten by the sender. The settle mode can range from
 * fire-and-forget (ALWAYS), to exactly-once (ACKNOWLEDGE).
 */
public enum MessageSettleMode {
  /**
   * ALWAYS settles the message immediately without waiting for any
   * confirmation. 
   * 
   * ALWAYS is the fire-and-forget, at-most-once mode where
   * the client sender will not receive any notification on failure.
   */
  ALWAYS,
  
  /**
   * AT_LEAST_ONCE settles the message with the receiver has read the
   * complete message from the network and put it in its prefetch queue.
   */
  NETWORK_AT_LEAST_ONCE,
  
  /**
   * AT_MOST_ONCE settles the message with the receiver has read the
   * complete message from the network and put it in its prefetch queue.
   */
  NETWORK_EXACTLY_ONCE,
  
  /**
   * RECEIVER_TAKE settles the message when the receiver takes the message
   * from its prefetch queue. This is equivalent to most auto-ack modes.
   */
  RECEIVER_TAKE_AT_LEAST_ONCE,
  
  /**
   * RECEIVER_TAKE settles the message when the receiver takes the message
   * from its prefetch queue, but before it it delivered to the client.
   * This is equivalent to most auto-ack modes.
   */
  RECEIVER_TAKE_EXACTLY_ONCE,
  
  /**
   * Application acknowledgement.
   */
  ACKNOWLEDGE_AT_LEAST_ONCE,
  
  /**
   * Application acknowledgement.
   */
  ACKNOWLEDGE_EXACTLY_ONCE;
}
