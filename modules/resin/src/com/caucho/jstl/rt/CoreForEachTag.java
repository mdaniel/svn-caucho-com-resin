/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.jstl.rt;

import com.caucho.util.L10N;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.jstl.core.LoopTagSupport;
import java.util.Iterator;

public class CoreForEachTag extends LoopTagSupport {
  private static L10N L = new L10N(CoreForEachTag.class);

  protected int _begin;
  protected int _end;
  
  protected Object _items;
  protected boolean _hasItems;

  // runtime values
  protected Iterator _iterator;

  /**
   * Sets the collection expression.
   */
  public void setItems(Object items)
  {
    _items = items;
    _hasItems = true;
  }

  /**
   * Sets the beginning value
   */
  public void setBegin(int begin)
  {
    _begin = begin;
    this.begin = begin;
    this.beginSpecified = true;
  }

  /**
   * Sets the ending value
   */
  public void setEnd(int end)
  {
    _end = end;
    this.end = end;
    this.endSpecified = true;
  }

  /**
   * Sets the step value
   */
  public void setStep(int step)
  {
    this.step = step;
    this.stepSpecified = true;
  }

  /**
   * Prepares the iterator.
   */
  public void prepare()
    throws JspTagException
  {
    if (_hasItems) {
      _iterator = com.caucho.jstl.el.ForEachTag.getIterator(_items);
    }
    else {
      this.beginSpecified = false;
      this.begin = 0;
      this.endSpecified = false;
      this.end = -1;

      _iterator = new RangeIterator(_begin, _end);
    }
  }

  /**
   * Returns true if there are more items.
   */
  public boolean hasNext()
  {
    return _iterator.hasNext();
  }

  /**
   * Returns the next item
   */
  public Object next()
  {
    return _iterator.next();
  }

  public static class RangeIterator implements Iterator {
    private int _end;
    private int _i;

    RangeIterator(int begin, int end)
    {
      _i = begin;
      _end = end;
    }
    
    public boolean hasNext()
    {
      return _i <= _end;
    }
    
    public Object next()
    {
      if (_i <= _end)
        return new Integer(_i++);
      else
        return null;
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
