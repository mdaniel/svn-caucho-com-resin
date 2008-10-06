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

class OsgiVersion {
  static final OsgiVersion ZERO = new OsgiVersion(0, 0, 0, "");
  static final OsgiVersion INF = new OsgiVersion(Integer.MAX_VALUE, 0, 0, "");
  
  private final int _major;
  private final int _minor;
  private final int _micro;
  private final String _qualifier;

  OsgiVersion(int major, int minor, int micro, String qualifier)
  {
    _major = major;
    _minor = minor;
    _micro = micro;

    if (qualifier != null)
      _qualifier = qualifier;
    else
      _qualifier = "";
  }

  static OsgiVersion create(String version)
  {
    if (version == null)
      return new OsgiVersion(0, 0, 0, "");
    
    int i = 0;
    int len = version.length();

    int major = 0;
    char ch;

    for (; i < len && '0' <= (ch = version.charAt(i)) && ch <= '9'; i++) {
      major = 10 * major + ch - '0';
    }

    for (; i < len && '.' == (ch = version.charAt(i)); i++) {
    }

    int minor = 0;
    
    for (; i < len && '0' <= (ch = version.charAt(i)) && ch <= '9'; i++) {
      minor = 10 * minor + ch - '0';
    }

    for (; i < len && '.' == (ch = version.charAt(i)); i++) {
    }

    int micro = 0;
    
    for (; i < len && '0' <= (ch = version.charAt(i)) && ch <= '9'; i++) {
      micro = 10 * micro + ch - '0';
    }

    for (; i < len && '.' == (ch = version.charAt(i)); i++) {
    }

    StringBuilder qualifier = new StringBuilder();
    
    for (; i < len
	   && ('a' <= (ch = version.charAt(i)) && ch <= 'z'
	       || 'A' <= ch && ch <= 'Z'
	       || '-' == ch
	       || '_' == ch);
	 i++) {
      qualifier.append(ch);
    }

    return new OsgiVersion(major, minor, micro, qualifier.toString());
  }

  public int compareTo(OsgiVersion version)
  {
    if (_major < version._major)
      return -1;
    else if (version._major < _major)
      return 1;
    
    if (_minor < version._minor)
      return -1;
    else if (version._minor < _minor)
      return 1;
    
    if (_micro < version._micro)
      return -1;
    else if (version._micro < _micro)
      return 1;

    return _qualifier.compareTo(version._qualifier);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    toString(sb);

    sb.append("]");

    return sb.toString();
  }

  void toString(StringBuilder sb)
  {
    if (_major == Integer.MAX_VALUE) {
      sb.append("inf");
    }
    else if (_major == Integer.MIN_VALUE) {
      sb.append("-inf");
    }
    else {
      sb.append(_major);
      sb.append(".").append(_minor);
      sb.append(".").append(_micro);

      if (_qualifier.length() > 0) {
	sb.append(".").append(_qualifier);
      }
    }
  }
}
