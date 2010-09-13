/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.env.deploy;

public class ExpandVersion {
  private final String _key;
  private final String _baseKey;
  private final String _version;

  ExpandVersion(String key,
                String baseKey,
                String version)
  {
    _key = key;
    _baseKey = baseKey;
    _version = version;
  }

  public String getKey()
  {
    return _key;
  }
  
  public String getBaseKey()
  {
    return _baseKey;
  }
  
  public String getVersion()
  {
    return _version;
  }
  
  int compareTo(ExpandVersion version)
  {
    if (getKey().equals(version.getKey()))
      return 0;
    
    String versionA = getVersion();
    String versionB = version.getVersion();

    // versioned are always preferred
    if (versionB.isEmpty())
      return 1;
    else if (versionA.isEmpty())
      return -1;
    
    return versionA.compareTo(versionB);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getKey() + "]";
  }
}
