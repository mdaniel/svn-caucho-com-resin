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

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;

import java.util.ArrayList;

public class RecursiveIteratorIterator
  implements OuterIterator, Traversable, Iterator
{
  public static final int LEAVES_ONLY = 0;
  public static final int SELF_FIRST = 1;
  public static final int CHILD_FIRST = 2;
  public static final int CATCH_GET_CHILD = 16;

  private final Iterator _iterator;
  private ArrayList<Iterator> _iteratorStack;

  private Value _currentValue;
  private Value _currentKey;

  private final int _mode;

  public RecursiveIteratorIterator(Env env,
                                   Iterator iterator,
                                   @Optional int mode,  //LEAVES_ONLY
                                   @Optional int flags)
  {
    _iterator = iterator;
    _iteratorStack = new ArrayList<Iterator>();

    _mode = mode;

    rewind(env);
  }

  @Override
  public Iterator getInnerIterator()
  {
    return _iterator;
  }

  /**
   * Returns the current value.
   */
  @Override
  public Value current(Env env)
  {
    return _currentValue;
  }

  /**
   * Returns the current key.
   */
  @Override
  public Value key(Env env)
  {
    return _currentKey;
  }

  /**
   * Advances to the next row.
   */
  @Override
  public void next(Env env)
  {
    _currentKey = null;
    _currentValue = null;

    boolean isJustPopped = false;

    while (true) {
      Iterator iter = getCurrentIterator();

      if (iter == null) {
        break;
      }

      boolean oldIsJustPopped = isJustPopped;
      isJustPopped = false;

      if (! iter.valid(env)) {
        pop();

        isJustPopped = true;

        continue;
      }
      else if (oldIsJustPopped && (_mode & CHILD_FIRST) == CHILD_FIRST) {
        _currentKey = iter.key(env);
        _currentValue = iter.current(env);

        iter.next(env);

        break;
      }
      else if (recurse(env, iter)) {
        break;
      }
    }
  }

  /**
   * Rewinds the iterator so it is at the first row.
   */
  @Override
  public void rewind(Env env)
  {
    init(env);
  }

  private void init(Env env)
  {
    Iterator iter = _iterator;

    if (iter == null) {
      return;
    }

    iter.rewind(env);
    _iteratorStack.clear();

    push(iter);
    recurse(env, iter);
  }

  /**
   * Returns true if _currentValue is set by method.
   */
  private boolean recurse(Env env, Iterator iterator)
  {
    if (! iterator.valid(env)) {
      return false;
    }

    Value currentKey = iterator.key(env);
    Value currentValue = iterator.current(env);

    Object obj = currentValue.toJavaObject();

    if (obj instanceof RecursiveIterator) {
      RecursiveIterator r = (RecursiveIterator) obj;

      push(r);

      if ((_mode & SELF_FIRST) == SELF_FIRST) {
        _currentKey = currentKey;
        _currentValue = currentValue;

        iterator.next(env);

        return true;
      }
      else if ((_mode & CHILD_FIRST) == CHILD_FIRST) {
        return recurse(env, r);
      }
      else {
        iterator.next(env);

        return recurse(env, r);
      }
    }
    else {
      _currentKey = currentKey;
      _currentValue = currentValue;

      iterator.next(env);

      return true;
    }
  }

  /**
   * Returns true if the iterator currently points to a valid row.
   */
  @Override
  public boolean valid(Env env)
  {
    return _currentKey != null;
  }

  private Iterator getCurrentIterator()
  {
    int size = _iteratorStack.size();

    if (size == 0) {
      return null;
    }

    return _iteratorStack.get(size - 1);
  }

  private void push(Iterator iterator)
  {
    _iteratorStack.add(iterator);
  }

  private void pop()
  {
    int size = _iteratorStack.size();

    _iteratorStack.remove(size - 1);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _iterator + "]";
  }

  static class Entry
  {
    private final Iterator _iterator;

    private boolean _isVisited;

    public Entry(Iterator iterator)
    {
      _iterator = iterator;
    }

    public Iterator getIterator()
    {
      return _iterator;
    }

    public boolean isVisited()
    {
      return _isVisited;
    }

    public void setVisited()
    {
      _isVisited = true;
    }
  }
}
