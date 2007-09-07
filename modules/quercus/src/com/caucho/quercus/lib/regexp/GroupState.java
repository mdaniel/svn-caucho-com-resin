/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.regexp;

class GroupState
{
  int BIT_WIDTH = 32;
  
  private long []_set = new long[4];
  private GroupState _next;
  
  public boolean isSet(int group)
  {
    int i = group / BIT_WIDTH;
    
    if (i >= _set.length)
      throw new RuntimeException("out of range: " + group + " >= " + (BIT_WIDTH * _set.length));
    
    int shift = group - i * BIT_WIDTH;
    int bit = 1 << shift;
    
    return (_set[i] & bit) != 0; 
  }

  public void set(int group)
  {
    int i = group / BIT_WIDTH;
    
    if (i >= _set.length)
      throw new RuntimeException("out of range: " + group + " >= " + (BIT_WIDTH * _set.length));
    
    int shift = group - i * BIT_WIDTH;
    int bit = 1 << shift;
    
    _set[i] |= bit;
  }
  
  public void save()
  {
    GroupState state = new GroupState();
    for (int i = 0; i < _set.length; i++) {
      state._set[i] = _set[i];
    }

    if (_next != null)
      state._next = _next;

    _next = state;
  }
  
  public void restore()
  {
    if (_next == null)
      throw new RuntimeException("cannot restore without a saved state");

    _set = _next._set;
    
    _next = _next._next;
  }
  
  public void pop()
  {
    if (_next == null)
      throw new RuntimeException("cannot pop an empty state stack");
    
    _next = _next._next;
  }
  
  public void clear()
  {
    for (int i = 0; i < _set.length; i++) {
      _set[i] = 0;
    }
  }
}
