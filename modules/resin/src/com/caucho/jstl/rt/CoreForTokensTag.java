/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.jstl.core.*;

import com.caucho.vfs.*;
import com.caucho.util.*;
import com.caucho.jsp.*;
import com.caucho.jsp.el.*;

public class CoreForTokensTag extends LoopTagSupport {
  private static L10N L = new L10N(CoreForTokensTag.class);

  protected String _items;
  protected String _delims;
  protected Iterator _iterator;

  /**
   * Sets the collection expression.
   */
  public void setItems(String items)
  {
    _items = items;
  }

  /**
   * Sets the delimiters expression.
   */
  public void setDelims(String delims)
  {
    _delims = delims;
  }

  /**
   * Sets the beginning value
   */
  public void setBegin(int begin)
  {
    this.begin = begin;
    this.beginSpecified = true;
  }

  /**
   * Sets the ending value
   */
  public void setEnd(int end)
  {
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
    _iterator = new TokenIterator(_items, _delims);
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

  public static class TokenIterator implements Iterator {
    private String _value;
    private char []_delims;
    private int _length;
    private int _i;
    private CharBuffer _cb = new CharBuffer();

    TokenIterator(String value, String delims)
    {
      _value = value;
      _delims = delims.toCharArray();
      _length = value.length();
    }
    
    public boolean hasNext()
    {
      return _i < _length;
    }
    
    public Object next()
    {
      _cb.clear();

      char ch = 0;
      int startDelims = _delims.length - 1;
      loop:
      for (; _i < _length; _i++) {
        ch = _value.charAt(_i);
        
        for (int j = startDelims; j >= 0; j--) {
          if (_delims[j] == ch)
            break loop;
        }
        
        _cb.append(ch);
      }

      _i++;

      return _cb.toString();
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
