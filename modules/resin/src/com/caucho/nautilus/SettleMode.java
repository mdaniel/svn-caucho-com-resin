/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.nautilus;

/**
 * Selects when a message is considered delivered and can be considered
 * settled and forgotten by the sender. The settle mode can range from
 * fire-and-forget (ALWAYS), to exactly-once (ACKNOWLEDGE).
 */
public enum SettleMode {
  /**
   * ALWAYS settles the message immediately without waiting for any
   * confirmation. 
   * 
   * ALWAYS is the fire-and-forget, at-most-once mode where
   * the client sender will not receive any notification on failure.
   */
  ALWAYS,
  
  /**
   * AT_LEAST_ONCE settles the message with the receiver has 
   * returned an ack.
   */
  AT_LEAST_ONCE,
  
  /**
   * EXACTLY_ONCE settles the message with the receiver has acknowledged
   * the message, and the sender has acknowledge receipt of the ack
   * (double ack.)
   */
  EXACTLY_ONCE;
}
