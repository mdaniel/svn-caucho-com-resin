/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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

import java.math.*;
import java.util.*;

import javax.el.LambdaExpression;

import com.caucho.v5.el.*;

/**
 * Represents a collection source for the stream()
 */
abstract class AbstractStream<S> implements Stream<S>
{
  @Override
  public Stream<S> filter(LambdaExpression predicate)
  {
    return new FilterStream<S>(this, predicate);
  }

  @Override
  public <R> Stream<R> map(LambdaExpression mapper)
  {
    return new MapStream<S,R>(this, mapper);
  }

  public <R> Stream<R> flatMap(LambdaExpression mapper)
  {
    return new FlatMapStream<>(this, mapper);
  }

  public Stream<S> distinct()
  {
    return new DistinctStream<>(this);
  }

  public Stream<S> sorted()
  {
    return new SortedStream<>(this);
  }

  public Stream<S> sorted(LambdaExpression comparator)
  {
    return new SortedStream<>(this, comparator);
  }

  @Override
  public void forEach(LambdaExpression consumer)
  {
    for (S value : this) {
      consumer.invoke(value);
    }
  }

  @Override
  public Stream<S> peek(LambdaExpression consumer)
  {
    return new PeekStream<>(this, consumer);
  }

  @Override
  public Stream<S> limit(Number count)
  {
    return new RangeStream<>(this, 0, count.longValue());
  }

  @Override
  public Stream<S> substream(Number start)
  {
    return substream(start, Long.MAX_VALUE);
  }

  @Override
  public Stream<S> substream(Number start, Number end)
  {
    return new RangeStream<>(this, start.longValue(), end.longValue());
  }

  @Override
  public S []toArray()
  {
    return (S[]) toList().toArray();
  }

  @Override
  public List<S> toList()
  {
    ArrayList<S> list = new ArrayList<>();

    for (S value : this) {
      list.add(value);
    }

    return list;
  }

  @Override
  public Optional<S> reduce(LambdaExpression binaryOperator)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext()) {
      return new Optional<S>();
    }

    S value = iterator.next();

    while (iterator.hasNext()) {
      Object result = binaryOperator.invoke(value, iterator.next());
      value = (S) binaryOperator.getELContext().convertToType(result,
                                                              value.getClass());
    }

    return new Optional<S>(value);
  }

  @Override
  public S reduce(S seed, LambdaExpression binaryOperator)
  {
    S value = seed;

    Iterator<S> iterator = iterator();
    while (iterator.hasNext()) {
      Object result = binaryOperator.invoke(value, iterator.next());
      value = (S) binaryOperator.getELContext().convertToType(result,
                                                              value.getClass());
    }

    return value;
  }

  @Override
  public Optional<S> max()
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext()) {
      return new Optional<S>();
    }

    S max = iterator.next();

    while (iterator.hasNext()) {
      S next = iterator.next();
      if (StreamUtil.compare(max, next) < 0) {
        max = next;
      }
    }

    return new Optional<S>((S) max);
  }

  @Override
  public Optional<S> max(LambdaExpression comparator)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext()) {
      return new Optional<S>();
    }

    S max = iterator.next();

    while (iterator.hasNext()) {
      S next = iterator.next();
      Object result = comparator.invoke(max, next);
      if (StreamUtil.compare(result, 0) < 0) {
        max = next;
      }
    }

    return new Optional<S>((S)max);
  }

  @Override
  public Optional<S> min()
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext()) {
      return new Optional<S>();
    }

    S min = iterator.next();

    while (iterator.hasNext()) {
      S next = iterator.next();
      if (StreamUtil.compare(next, min) < 0) {
        min = next;
      }
    }

    return new Optional<S>((S)min);
  }

  @Override
  public Optional<S> min(LambdaExpression comparator)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext()) {
      return new Optional<S>();
    }

    S min = iterator.next();

    while (iterator.hasNext()) {
      S next = iterator.next();
      Object result = comparator.invoke(next, min);
      if (StreamUtil.compare(result, 0) < 0) {
        min = next;
      }
    }

    return new Optional<S>((S)min);
  }

  @Override
  public Optional<S> average()
  {
    Number sum = 0;
    int count = 0;

    Iterator<S> iterator = iterator();
    while (iterator.hasNext()) {
      S next = iterator.next();
      sum = StreamUtil.add(sum, next);
      count++;
    }

    if (count == 0) {
      return new Optional<>();
    }

    Number avg = null;

    if (sum instanceof BigDecimal) {
      avg = ((BigDecimal) sum).divide(Expr.toBigDecimal(count));
    }
    else if (sum instanceof BigInteger) {
      avg = Expr.toBigDecimal(sum).divide(Expr.toBigDecimal(count));
    }
    else {
      avg = Expr.toDouble(sum) / Expr.toDouble(count);
    }

    return new Optional(avg);
  }

  @Override
  public Number sum()
  {
    Number sum = 0;

    Iterator<S> iterator = iterator();
    while (iterator.hasNext()) {
      S next = iterator.next();
      sum = StreamUtil.add(sum, next);
    }

    return sum;
  }

  @Override
  public long count()
  {
    long count = 0;

    Iterator<S> iterator = iterator();
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }

    return count;
  }

  @Override
  public Optional<Boolean> anyMatch(LambdaExpression predicate)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext())
      return new Optional<>();

    while (iterator.hasNext()) {
      S next = iterator.next();
      Object result = predicate.invoke(next);
      if (Expr.toBoolean(result)) {
        return new Optional<Boolean>(Boolean.TRUE);
      }
    }

    return new Optional<Boolean>(Boolean.FALSE);
  }

  @Override
  public Optional<Boolean> allMatch(LambdaExpression predicate)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext())
      return new Optional<>();

    while (iterator.hasNext()) {
      S next = iterator.next();
      Object result = predicate.invoke(next);
      if (! Expr.toBoolean(result)) {
        return new Optional<Boolean>(Boolean.FALSE);
      }
    }

    return new Optional<Boolean>(Boolean.TRUE);
  }

  @Override
  public Optional<Boolean> noneMatch(LambdaExpression predicate)
  {
    Iterator<S> iterator = iterator();

    if (! iterator.hasNext())
      return new Optional<>();

    while (iterator.hasNext()) {
      S next = iterator.next();
      Object result = predicate.invoke(next);
      if (Expr.toBoolean(result)) {
        return new Optional<Boolean>(Boolean.FALSE);
      }
    }

    return new Optional<Boolean>(Boolean.TRUE);
  }

  @Override
  public Optional<S> findFirst()
  {
    Iterator<S> iterator = iterator();

    if (iterator.hasNext())
      return new Optional<S>(iterator.next());
    else
      return new Optional<>();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
