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
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.*;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;

/**
 * a Map property
 */
public class MapProperty extends Property {
  private static final QName _keyName = new QName("key");
  private static final QName _valueName = new QName("value");

  private Class _mapType;
  private Property _keyProperty; 
  private Property _valueProperty;

  public MapProperty(Class mapType, 
                     Property keyProperty, 
                     Property valueProperty)
  {
    _mapType = mapType;
    _keyProperty = keyProperty;
    _valueProperty = valueProperty;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    in.nextTag();

    Object obj = previous;
     
    if (obj == null) {
      try {
        obj = _mapType.newInstance();
      }
      catch (IllegalAccessException e) {
        throw new JAXBException(e);
      }
      catch (InstantiationException e) {
        throw new JAXBException(e);
      }
    }

    Map<Object,Object> map = (Map<Object,Object>) obj;

    while (in.getEventType() == in.START_ELEMENT && 
           "key".equals(in.getLocalName())) {
      Object key = _keyProperty.read(u, in, null);

      if (in.getEventType() != in.START_ELEMENT ||
          ! "value".equals(in.getLocalName()))
        throw new JAXBException("Key without value while reading map");

      Object value = _valueProperty.read(u, in, null);

      map.put(key, value);
    }

    in.nextTag();

    return map;
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    XMLEvent event = in.nextEvent();

    event = in.nextEvent();

    Object obj = previous;
     
    if (obj == null) {
      try {
        obj = _mapType.newInstance();
      }
      catch (IllegalAccessException e) {
        throw new JAXBException(e);
      }
      catch (InstantiationException e) {
        throw new JAXBException(e);
      }
    }

    Map<Object,Object> map = (Map<Object,Object>) obj;

    while (event.isStartElement() &&
           "key".equals(((StartElement) event).getName().getLocalPart())) {
      Object key = _keyProperty.read(u, in, null);

      if (! event.isStartElement() || 
          ! "value".equals(((StartElement) event).getName().getLocalPart()))
        throw new JAXBException("Key without value while reading map");

      Object value = _valueProperty.read(u, in, null);

      map.put(key, value);

      event = in.peek();
    }

    return map;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Map<Object,Object> map = (Map<Object,Object>) obj;

      writeQNameStartElement(out, qname);

      for (Map.Entry<Object,Object> entry : map.entrySet()) {
        _keyProperty.write(m, out, entry.getKey(), _keyName);
        _valueProperty.write(m, out, entry.getValue(), _valueName);
      }

      writeQNameEndElement(out, qname);
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Map<Object,Object> map = (Map<Object,Object>) obj;

      out.add(JAXBUtil.EVENT_FACTORY.createStartElement(qname, null, null));

      for (Map.Entry<Object,Object> entry : map.entrySet()) {
        _keyProperty.write(m, out, entry.getKey(), _keyName);
        _valueProperty.write(m, out, entry.getValue(), _valueName);
      }

      out.add(JAXBUtil.EVENT_FACTORY.createEndElement(qname, null));
    }
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName qname)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void generateSchema(XMLStreamWriter out)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public String getSchemaType()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isXmlPrimitiveType()
  {
    return false;
  }
}
