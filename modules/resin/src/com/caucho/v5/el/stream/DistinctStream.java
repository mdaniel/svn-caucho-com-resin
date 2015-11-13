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

/**
 * Represents a collection source for the stream()
 */
public class DistinctStream<S> extends AbstractStream<S> implements StreamPredicate<S>
{
  private final Stream<S> _source;
  
  private Set<S> _distinctSet = new HashSet<>();

  public DistinctStream(Stream<S> source)
  {
    _source = source;
  }

  @Override
  public Iterator<S> iterator()
  {
    return new PredicatedIterator<S>(_source.iterator(), this);
  }
  
  @Override
  public boolean isMatch(S item)
  {
    return _distinctSet.add(item);
  }
}
