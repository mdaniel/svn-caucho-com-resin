/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.util.L10N;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

public class JAXBElementSkeleton<C> extends ClassSkeleton<JAXBElement<C>> {
  private static final Logger log 
    = Logger.getLogger(JAXBElementSkeleton.class.getName());
  private static final L10N L = new L10N(JAXBElementSkeleton.class);
  private static final Object[] SINGLE_NULL_ARG = new Object[] {null};

  private final Class<C> _contents;
  private final Method _createMethod;
  private final Object _factory;

  public JAXBElementSkeleton(JAXBContextImpl context, Class<C> cl,
                             Method createMethod, Object factory)
    throws JAXBException
  {
    super(context);
    _contents = cl;
    _createMethod = createMethod;
    _factory = factory;
    _value = new ContentsAccessor(context);
  }

  public JAXBElement<C> newInstance()
    throws JAXBException
  {
    try {
      // passing a null makes invoke think that it is receiving a null instance
      // of an Object[].  need an explicit array with a null.
      return (JAXBElement<C>) _createMethod.invoke(_factory, SINGLE_NULL_ARG);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  private class ContentsAccessor extends Accessor {
    public ContentsAccessor(JAXBContextImpl context)
      throws JAXBException
    {
      super(context);

      _property = _context.createProperty(_contents);
    }

    public QName getQName()
    {
      return _elementName;
    }

    public Object get(Object o) 
      throws JAXBException
    {
      JAXBElement<C> element = (JAXBElement<C>) o;

      return element.getValue();
    }

    public void set(Object o, Object value) 
      throws JAXBException
    {
      JAXBElement<C> element = (JAXBElement<C>) o;
      element.setValue((C) value);
    }

    public String getName()
    {
      return null;
    }

    public Class getType()
    {
      return _contents;
    }

    public Type getGenericType()
    {
      return _contents;
    }

    public <A extends Annotation> A getAnnotation(Class<A> c)
    {
      return null;
    }

    public <A extends Annotation> A getPackageAnnotation(Class<A> c)
    {
      return null;
    }

    public Package getPackage()
    {
      return null;
    }

    public String toString()
    {
      return "JAXBElementSkeleton.ContentsAccessor[" + _contents + "]";
    }
  }

  public String toString()
  {
    return "JAXBElementSkeleton[" + _contents + "]";
  }
}
