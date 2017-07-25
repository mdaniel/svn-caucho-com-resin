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

interface ArrayRing<T>
{
  int getLength();

  T get(long index);

  void set(long index, T value);

  void setLazy(long index, T value);

  /**
   * Polls the array item until a value is available, then clears
   * the array.
   */
  T takeAndClear(long index);

   /**
   * Polls the array item. If a value is available, clear the array.
   */
  T pollAndClear(long tailAlloc);

  int getIndex(long tail);

  void clear(long start, long end);
}
