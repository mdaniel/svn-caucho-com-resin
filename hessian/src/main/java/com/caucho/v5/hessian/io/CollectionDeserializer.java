/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.hessian.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Deserializing a JDK 1.2 Collection.
 */
public class CollectionDeserializer extends AbstractListDeserializer {
  private Class<?> _type;
  private Supplier<Collection<?>> _supplier;
  
  public CollectionDeserializer(Class<?> type)
  {
    _type = type;
    
    _supplier = introspectSupplier(type);
  }
  
  @Override
  public Class<?> getType()
  {
    return _type;
  }
  
  @Override
  public Object readList(AbstractHessianInput in, int length)
    throws IOException
  {
    Collection list = _supplier.get();

    in.addRef(list);

    while (! in.isEnd()) {
      list.add(in.readObject());
    }

    in.readEnd();

    return list;
  }
  
  @Override
  public Object readLengthList(AbstractHessianInput in, int length)
    throws IOException
  {
    Collection list = _supplier.get();

    in.addRef(list);

    for (; length > 0; length--) {
      list.add(in.readObject());
    }

    return list;
  }

  private static Supplier<Collection<?>> introspectSupplier(Class<?> type)
  {
    if (type == null) {
      return ArrayListSupplier.SUPPLIER;
    }
    else if (type.isInterface()) {
    }
    else if (type.isMemberClass() && type.getName().startsWith("java.lang")) {
    }
    else  {
      Collection<?> list = null;
      
      try {
        list = (Collection<?>) type.newInstance();
      } catch (Exception e) {
      }
      
      if (list != null) {
        return new InstanceSupplier(type);
      }
    }

    if (SortedSet.class.isAssignableFrom(type)) {
      return TreeSetSupplier.SUPPLIER;
    }
    else if (Set.class.isAssignableFrom(type)) {
      return HashSetSupplier.SUPPLIER;
    }
    else if (List.class.isAssignableFrom(type)) {
      return ArrayListSupplier.SUPPLIER;
    }
    else if (Collection.class.isAssignableFrom(type)) {
      return ArrayListSupplier.SUPPLIER;
    }
    else {
      return new InstanceSupplier(type);
    }
  }

  private static class ArrayListSupplier implements Supplier<Collection<?>>
  {
    private static final ArrayListSupplier SUPPLIER = new ArrayListSupplier();
    
    @Override
    public Collection<?> get()
    {
      return new ArrayList<Object>();
    }
  }

  private static class TreeSetSupplier implements Supplier<Collection<?>>
  {
    private static final TreeSetSupplier SUPPLIER = new TreeSetSupplier();
    
    @Override
    public Collection<?> get()
    {
      return new TreeSet<Object>();
    }
  }

  private static class HashSetSupplier implements Supplier<Collection<?>>
  {
    private static final HashSetSupplier SUPPLIER = new HashSetSupplier();
    
    @Override
    public Collection<?> get()
    {
      return new HashSet<Object>();
    }
  }

  private static class InstanceSupplier implements Supplier<Collection<?>>
  {
    private final Class<?> _cl;
    
    InstanceSupplier(Class<?> cl)
    {
      _cl = cl;
    }
    
    @Override
    public Collection<?> get()
    {
      try {
        return (Collection<?>) _cl.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
  }
}


