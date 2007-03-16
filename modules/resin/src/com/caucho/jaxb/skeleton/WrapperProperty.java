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

import java.io.IOException;

import java.util.Iterator;

import javax.xml.XMLConstants;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import javax.xml.namespace.QName;

import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;

import com.caucho.util.L10N;

/**
 * represents a property in a skeleton; requires an Accessor to access it
 */
public class WrapperProperty extends Property {
  public static final L10N L = new L10N(WrapperProperty.class);

  private final String _name;
  private final String _namespace;
  private final QName _wrappedQName;
  private final Property _property;
  private final boolean _nillable;

  public WrapperProperty(Property property, 
                         XmlElementWrapper elementWrapper,
                         String wrappedNamespace, String wrappedName)
  {
    _wrappedQName = new QName(wrappedNamespace, wrappedName);

    if ("##default".equals(elementWrapper.name()))
      _name = wrappedName;
    else
      _name = elementWrapper.name();

    if ("##default".equals(elementWrapper.namespace()))
      _namespace = wrappedNamespace;
    else
      _namespace = elementWrapper.namespace();

    _property = property;
    _nillable = elementWrapper.nillable();
  }

  //
  // Schema generation methods
  // 
  public boolean isXmlPrimitiveType()
  {
    return _property.isXmlPrimitiveType(); // XXX
  }

  public String getMinOccurs()
  {
    return "0";
  }

  public String getMaxOccurs()
  {
    return null;
  }

  public boolean isNillable()
  {
    return false;
  }

  public String getSchemaType()
  {
    return _property.getSchemaType(); // XXX
  }

  //
  // R/W methods
  //

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    Object ret = null;

    in.nextTag(); // skip wrapper

    while (in.getEventType() == in.START_ELEMENT && 
           _wrappedQName.equals(in.getName()))
      ret = _property.read(u, in, previous);

    in.next();

    return ret;
  }
  
  public Object read(Unmarshaller u, XMLEventReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException,JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void write(Marshaller m, XMLStreamWriter out, Object value, QName name)
    throws IOException, XMLStreamException, JAXBException
  {
    if (_namespace != null)
      out.writeStartElement(_namespace, _name);
    else
      out.writeStartElement(_name);

    _property.write(m, out, value, name);

    out.writeEndElement();
  }

  public void write(Marshaller m, XMLEventWriter out, Object value, QName name)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public Node bindTo(BinderImpl binder, Node node, Object value, QName qname)
    throws IOException,JAXBException
  {
    throw new UnsupportedOperationException();
  }
}
