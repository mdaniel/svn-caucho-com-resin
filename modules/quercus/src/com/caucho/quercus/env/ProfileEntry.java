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

package com.caucho.quercus.env;

/**
 * A profile entry
 */
public final class ProfileEntry
{
  private final int _id;
  private final int _parentId;

  private final ProfileEntry _hashNext;

  private long _count;
  private long _nanos;

  public ProfileEntry(int id, int parentId, ProfileEntry hashNext)
  {
    _id = id;
    _parentId = parentId;
    _hashNext = hashNext;
  }

  public final int getId()
  {
    return _id;
  }

  public final int getParentId()
  {
    return _parentId;
  }

  public final ProfileEntry getHashNext()
  {
    return _hashNext;
  }

  public long getCount()
  {
    return _count;
  }

  public long getNanos()
  {
    return _nanos;
  }

  public final void add(long nanos)
  {
    _count++;
    _nanos += nanos;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + _parentId + "]";
  }
}

