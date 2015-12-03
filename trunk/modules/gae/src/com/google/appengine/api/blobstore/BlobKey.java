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
 * @author Nam Nguyen
 */

package com.google.appengine.api.blobstore;

import java.io.Serializable;

public final class BlobKey implements Serializable, Comparable<BlobKey>
{
  private final String _key;

  public BlobKey(String blobKey)
  {
    _key = blobKey;
  }

  @Override
  public int compareTo(BlobKey blobKey)
  {
    return _key.compareTo(blobKey._key);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (! (obj instanceof BlobKey)) {
      return false;
    }

    BlobKey blobKey = (BlobKey) obj;

    return _key.equals(blobKey._key);
  }

  public String getKeyString()
  {
    return _key;
  }

  @Override
  public int hashCode()
  {
    return _key.hashCode();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _key + "]";
  }
}
