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

package com.caucho.loader.ivy;

import java.util.*;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * Pattern used in ivy
 */
public class IvyRevision {
  private String _rev;
  
  private String []_minSegments;
  private String []_maxSegments;

  public IvyRevision(String rev)
  {
    _rev = rev;

    if (rev.indexOf(',') > 0) {
      int p = rev.indexOf(',');
      
      String min = rev.substring(0, p);
      String max = rev.substring(p + 1);
      
      _minSegments = min.split("\\.");
      _maxSegments = max.split("\\.");
    }
    else {
      _minSegments = rev.split("\\.");
      _maxSegments = _minSegments;
    }
  }

  public int compareTo(IvyRevision rev)
  {
    int i = 0;
    for (; i < _minSegments.length; i++) {
      if (rev._minSegments.length <= i)
        return 1;
      
      String a = _minSegments[i];
      String b = rev._minSegments[i];
      
      if (a.equals(b))
        continue;

      try {
        int intA = Integer.parseInt(a);
        int intB = Integer.parseInt(b);

        return intA - intB;
      } catch (Exception e) {
        return a.compareTo(b);
      }
    }

    if (i < rev._minSegments.length)
      return -1;
    else
      return 0;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _rev + "]";
  }
}
