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
 * @author Scott Ferguson
 */

package com.caucho.osgi;

class OsgiVersionRange {
  private final OsgiVersion _min;
  private final boolean _isMinEqual;
  
  private final OsgiVersion _max;
  private final boolean _isMaxEqual;

  OsgiVersionRange(OsgiVersion min, boolean isMinEqual,
		   OsgiVersion max, boolean isMaxEqual)
  {
    _min = min;
    _isMinEqual = isMinEqual;
    
    _max = max;
    _isMaxEqual = isMaxEqual;
  }

  public static OsgiVersionRange create(String version)
  {
    boolean isMinEqual = true;

    if (version == null || version.length() == 0)
      return null;
    
    if (version.charAt(0) == '[') {
      isMinEqual = true;
    }
    else if (version.charAt(0) == '(') {
      isMinEqual = false;
    }
    else {
      OsgiVersion value = OsgiVersion.create(version);

      return new OsgiVersionRange(value, true, OsgiVersion.INF, true);
    }

    boolean isMaxEqual = true;

    if (version.endsWith("]"))
      isMaxEqual = true;
    else if (version.endsWith(")"))
      isMaxEqual = false;

    int p = version.indexOf(',');

    if (p < 0) {
      OsgiVersion value = OsgiVersion.create(version);
      
      return new OsgiVersionRange(value, isMinEqual, OsgiVersion.INF, true);
    }

    String minString = version.substring(1, p).trim();
    String maxString = version.substring(p + 1).trim();

    OsgiVersion min = OsgiVersion.create(minString);
    OsgiVersion max = OsgiVersion.create(maxString);

    return new OsgiVersionRange(min, isMinEqual, max, isMaxEqual);
  }

  public OsgiVersion getMin()
  {
    return _min;
  }

  public OsgiVersion getMax()
  {
    return _max;
  }

  public boolean isMatch(OsgiVersion version)
  {
    int cmp = _min.compareTo(version);

    if (cmp > 0)
      return false;
    else if (! _isMinEqual && cmp == 0)
      return false;
    
    cmp = _max.compareTo(version);

    if (cmp < 0)
      return false;
    else if (! _isMaxEqual && cmp == 0)
      return false;

    return true;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());

    if (_isMinEqual)
      sb.append("[");
    else
      sb.append("(");

    _min.toString(sb);

    sb.append(",");
    
    _max.toString(sb);

    if (_isMaxEqual)
      sb.append("]");
    else
      sb.append(")");

    return sb.toString();
  }
}
