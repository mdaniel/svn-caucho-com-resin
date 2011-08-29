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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

/**
 * FreeList provides a simple class to manage free objects.  This is useful
 * for large data structures that otherwise would gobble up huge GC time.
 *
 * <p>The free list is bounded.  Freeing an object when the list is full will
 * do nothing.
 */
public final class FreeList<T> {
  private T _freeStack[];
  private int _top;

  /**
   * Create a new free list.
   *
   * @param initialSize maximum number of free objects to store.
   */
  public FreeList(int size)
  {
    _freeStack = (T []) new Object[size];
  }
  /**
   * Try to get an object from the free list.  Returns null if the free list
   * is empty.
   *
   * @return the new object or null.
   */
  public T allocate()
  {
    synchronized (_freeStack) {
      if (_top > 0)
        return _freeStack[--_top];
      else
        return null;
    }
  }
  /**
   * Frees the object.  If the free list is full, the object will be garbage
   * collected.
   *
   * @param obj the object to be freed.
   */
  public boolean free(T obj)
  {
    synchronized (_freeStack) {
      if (_top < _freeStack.length) {
        _freeStack[_top++] = obj;
        return true;
      }
      else
        return false;
    }
  }

  public boolean allowFree(T obj)
  {
    return _top < _freeStack.length;
  }

  /**
   * Frees the object.  If the free list is full, the object will be garbage
   * collected.
   *
   * @param obj the object to be freed.
   */
  public void freeCareful(T obj)
  {
    if (checkDuplicate(obj))
      throw new IllegalStateException("tried to free object twice: " + obj);

    free(obj);
  }

  /**
   * Debugging to see if the object has already been freed.
   */
  public boolean checkDuplicate(T obj)
  {
    synchronized (_freeStack) {
      int top = _top;

      for (int i = _top - 1; i >= 0; i--) {
        if (_freeStack[i] == obj)
          return true;
      }
    }

    return false;
  }
}
