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

import java.util.Iterator;

import javax.el.*;

import com.caucho.v5.util.L10N;

/**
 * Represents a collection source for the stream()
 */
public class FlatMapStream<S,R> extends AbstractStream<R>
{
  protected static final L10N L = new L10N(FlatMapStream.class);

  private final Stream<S> _source;
  private final LambdaExpression _mapper;
  
  public FlatMapStream(Stream<S> source, LambdaExpression mapper)
  {
    _source = source;
    _mapper = mapper;
  }

  @Override
  public Iterator<R> iterator()
  {
    return new FlatMapIterator(_source.iterator());
  }
  
  private class FlatMapIterator implements Iterator<R>
  {
    private Iterator<S> _sourceIterator;
    private Iterator<R> _currentIterator;
    
    FlatMapIterator(Iterator<S> sourceIterator)
    {
      _sourceIterator = sourceIterator;
    }
    
    @Override
    public boolean hasNext()
    {
      if (_currentIterator == null) {
        if (! _sourceIterator.hasNext()) {
          return false;
        }
        
        Object result = _mapper.invoke(_sourceIterator.next());
        if (! (result instanceof Stream)) {
          throw new ELException(L.l("flatmap mapper must produce a stream"));
        }
        
        _currentIterator = ((Stream)result).iterator();
        return hasNext();
      } 
      else {
        if (_currentIterator.hasNext()) {
          return true;
        }
        
        _currentIterator = null;
        return hasNext();
      }
    }

    @Override
    public R next()
    {
      if (_currentIterator == null)
        return null;
      return _currentIterator.next();
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
    
  }
}
