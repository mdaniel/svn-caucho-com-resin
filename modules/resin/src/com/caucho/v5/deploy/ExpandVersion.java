/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

import java.util.Comparator;

public class ExpandVersion {
  private static final VersionNameComparator _comparator
    = new VersionNameComparator();
  
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
    return _comparator.compare(version.getVersion(), getVersion());
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof ExpandVersion))
      return false;
    
    ExpandVersion version = (ExpandVersion) o;
    
    return getKey().equals(version.getKey());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getKey() + "]";
  }

  static class VersionNameComparator implements Comparator<String>
  {
    public int compare(String versionA, String versionB)
    {
      /*
      String versionA = a.getVersion();
      String versionB = b.getVersion();
      */
      
      if (versionA.equals(versionB))
        return 0;
      
      if (versionA.isEmpty())
        return 1;
      else if (versionB.isEmpty())
        return -1;

      int lengthA = versionA.length();
      int lengthB = versionB.length();

      int indexA = 0;
      int indexB = 0;

      while (indexA < lengthA && indexB < lengthB) {
        int valueA = 0;
        int valueB = 0;
        char chA;
        char chB;

        for (;
             indexA < lengthA
               && '0' <= (chA = versionA.charAt(indexA)) && chA <= '9';
             indexA++) {
          valueA = 10 * valueA + chA - '0';
        }

        for (;
             indexB < lengthB
               && '0' <= (chB = versionB.charAt(indexB)) && chB <= '9';
             indexB++) {
          valueB = 10 * valueB + chB - '0';
        }

        if (valueA < valueB)
          return 1;
        else if (valueB < valueA)
          return -1;

        while (indexA < lengthA && indexB < lengthB
               && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9')
               && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9')) {

          if (chA < chB)
            return 1;
          else if (chB < chA)
            return -1;

          indexA++;
          indexB++;
        }

        if (indexA < lengthA
            && ! ('0' <= (chA = versionA.charAt(indexA)) && chA <= '9'))
          return 1;
        else if (indexB < lengthB
                 && ! ('0' <= (chB = versionB.charAt(indexB)) && chB <= '9'))
          return -1;
      }

      if (indexA != lengthA)
        return 1;
      else if (indexB != lengthB)
        return -1;
      else
        return 0;
    }
  }
}
