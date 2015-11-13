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

import java.util.List;

import javax.el.LambdaExpression;

/**
 * Interface for the stream
 */
public interface Stream<S> extends Iterable<S>
{
  Stream<S> filter(LambdaExpression predicate);
  
  <R> Stream<R> map(LambdaExpression mapper);

  <R> Stream<R> flatMap(LambdaExpression mapper);
  
  Stream<S> distinct();

  Stream<S> sorted();

  Stream<S> sorted(LambdaExpression comparator);
  
  void forEach(LambdaExpression consumer);
  
  Stream<S> peek(LambdaExpression consumer);
  
  Stream<S> limit(Number count);
  
  Stream<S> substream(Number start);

  Stream<S> substream(Number start, Number end);

  S []toArray();

  List<S> toList();
  
  Optional<S> reduce(LambdaExpression binaryOperator);
  
  S reduce(S seed, LambdaExpression binaryOperator);
  
  Optional<S> max();
  
  Optional<S> max(LambdaExpression comparator);
  
  Optional<S> min();
  
  Optional<S> min(LambdaExpression comparator);
  
  Optional<S> average();
  
  Number sum();
  
  long count();
  
  Optional<Boolean> anyMatch(LambdaExpression predicate);
  
  Optional<Boolean> allMatch(LambdaExpression predicate);
  
  Optional<Boolean> noneMatch(LambdaExpression predicate);
  
  Optional<S> findFirst();
}
