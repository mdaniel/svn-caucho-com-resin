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

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 *
 **/
public class XmlInstanceWrapper extends Accessor {
  private static final Logger log 
    = Logger.getLogger(XmlInstanceWrapper.class.getName());
  private static final L10N L = new L10N(XmlInstanceWrapper.class);
  private static final QName XSI_TYPE_NAME 
    = new QName(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", "xsi");

  private final Iterator _attributes;
  private final String _type;

  public XmlInstanceWrapper(String type)
  {
    this(type, null);
  }

  public XmlInstanceWrapper(String type, Iterator attributes)
  {
    _attributes = attributes;
    _type = type;
  }

  // XXX this is an ugly hack
  public void writeAttribute(Marshaller m, XMLStreamWriter out, Object value)
    throws IOException, XMLStreamException, JAXBException
  {
    super.writeAttribute(m, out, value);

    if (_type.startsWith("xsd:"))
      out.writeNamespace("xsd", W3C_XML_SCHEMA_NS_URI);
  }

  public AccessorType getAccessorType()
    throws JAXBException
  {
    return AccessorType.ATTRIBUTE;
  }

  public Object get(Object o) 
    throws JAXBException
  {
    return _type;
  }

  public void set(Object o, Object value) 
    throws JAXBException
  {
  }

  protected QName getQName(Object obj)
    throws JAXBException
  {
    return XSI_TYPE_NAME;
  }

  public String getName()
  {
    return "";
  }

  public Class getType()
  {
    return String.class;
  }

  public Type getGenericType()
  {
    return String.class;
  }

  public <A extends Annotation> A getAnnotation(Class<A> c)
  {
    return null;
  }

  public <A extends Annotation> A getPackageAnnotation(Class<A> c)
  {
    return null;
  }

  public Iterator getExtendedIterator()
  {
    return new ExtendedIterator();
  }

  private class ExtendedIterator implements Iterator
  {
    private boolean _givenThis = false;

    public boolean hasNext()
    {
      return (_attributes != null && _attributes.hasNext()) || ! _givenThis;
    }

    public Object next()
    {
      if (_attributes != null && _attributes.hasNext())
        return _attributes.next();

      if (! _givenThis) {
        _givenThis = true;

        return XmlInstanceWrapper.this;
      }

      throw new NoSuchElementException();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
