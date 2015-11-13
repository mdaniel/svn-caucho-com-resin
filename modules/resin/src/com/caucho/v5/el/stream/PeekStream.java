/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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
 * @author Paul Cowan
 */

package com.caucho.v5.el.stream;

import java.util.*;

import javax.el.LambdaExpression;

/**
 * Represents a collection source for the stream()
 */
public class PeekStream<S> extends AbstractStream<S>
{
  private final Stream<S> _source;
  private final LambdaExpression _consumer;

  public PeekStream(Stream<S> source, LambdaExpression consumer)
  {
    _source = source;
    _consumer = consumer;
  }

  @Override
  public Iterator<S> iterator()
  {
    return new PeekIterator(_source.iterator());
  }
  
  private class PeekIterator implements Iterator<S>
  {
    private Iterator<S> _source;
    
    PeekIterator(Iterator<S> source)
    {
      _source = source;
    }

    @Override
    public boolean hasNext()
    {
      return _source.hasNext();
    }

    @Override
    public S next()
    {
      S item = _source.next();
      _consumer.invoke(item);
      return item;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
}
