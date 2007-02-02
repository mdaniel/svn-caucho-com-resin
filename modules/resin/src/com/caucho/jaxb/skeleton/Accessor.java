/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import javax.xml.namespace.QName;

import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import javax.xml.XMLConstants;

import java.lang.annotation.Annotation;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.io.IOException;

import java.util.Map;

/** an Accessor is either a getter/setter pair or a field */
public abstract class Accessor {
  public static final L10N L = new L10N(Accessor.class);

  public static final String XML_SCHEMA_PREFIX = "xsd";
  public static final String XML_INSTANCE_PREFIX = "xsi";

  private static boolean _generateRICompatibleSchema = true;

  protected int _order = -1;
  protected JAXBContextImpl _context;
  protected Property _property;
  protected QName _qname = null;
  protected QName _typeQName = null;

  public static void setGenerateRICompatibleSchema(boolean compatible)
  {
    _generateRICompatibleSchema = compatible;
  }

  protected Accessor(JAXBContextImpl context)
  {
    _context = context;
  }

  public void setOrder(int order)
  {
    _order = order;
  }

  public boolean checkOrder(int order, ValidationEventHandler handler)
  {
    if (_order < 0 || _order == order)
      return true;

    ValidationEvent event = 
      new ValidationEventImpl(ValidationEvent.ERROR, 
                              L.l("ordering error"), 
                              new ValidationEventLocatorImpl());

    return handler.handleEvent(event);
  }

  public JAXBContextImpl getContext()
  {
    return _context;
  }

  // Output methods

  public void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    _property.write(m, out, obj, getQName());
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    _property.write(m, out, obj, getQName());
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj)
    throws JAXBException
  {
    return _property.bindTo(binder, node, obj, getQName());
  }

  // Input methods

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, getQName());
  }

  public Object read(Unmarshaller u, XMLEventReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, getQName());
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node)
    throws JAXBException
  {
    return _property.bindFrom(binder, node, getQName());
  }


  protected void writeStartElement(XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX
    XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);
    XmlElement element = getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && ! wrapper.nillable())
        return;

      if (wrapper.name().equals("##default"))
        out.writeStartElement(getName());
      else if (wrapper.namespace().equals("##default"))
        out.writeStartElement(wrapper.name());
      else
        out.writeStartElement(wrapper.namespace(), wrapper.name());

      if (obj == null) {
        out.writeAttribute(XML_INSTANCE_PREFIX, 
                           XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                           "nil", "true");
      }
    }
    else if (element != null) {
      if (obj == null && ! element.nillable())
        return;

      if (element.name().equals("##default"))
        out.writeStartElement(getName());
      else if (element.namespace().equals("##default"))
        out.writeStartElement(element.name());
      else
        out.writeStartElement(element.namespace(), element.name());

      if (obj == null) {
        out.writeAttribute(XML_INSTANCE_PREFIX, 
                           XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                           "nil", "true");
      }
    }
    else {
      if (obj == null) return;

      QName qname = getQName();

      if (qname.getNamespaceURI() == null || "".equals(qname.getNamespaceURI()))
        out.writeStartElement(qname.getLocalPart());
      else
        out.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
    }
  }

  protected void writeEndElement(XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);
    XmlElement element = getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && !wrapper.nillable())
        return;
    } 
    else if (element != null) {
      if (obj == null && !element.nillable())
        return;
    } 
    else {
      if (obj == null) return;
    }

    out.writeEndElement();
  }

  protected void writeStartElement(XMLEventWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    /* XXX convert to event based
    XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);
    XmlElement element = getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && ! wrapper.nillable())
        return;

      if (wrapper.name().equals("##default"))
        out.writeStartElement(getName());
      else if (wrapper.namespace().equals("##default"))
        out.writeStartElement(wrapper.name());
      else
        out.writeStartElement(wrapper.namespace(), wrapper.name());

      if (obj == null) {
        out.writeAttribute(XML_INSTANCE_PREFIX, 
                           XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                           "nil", "true");
      }
    }
    else if (element != null) {
      if (obj == null && ! element.nillable())
        return;

      if (element.name().equals("##default"))
        out.writeStartElement(getName());
      else if (element.namespace().equals("##default"))
        out.writeStartElement(element.name());
      else
        out.writeStartElement(element.namespace(), element.name());

      if (obj == null) {
        out.writeAttribute(XML_INSTANCE_PREFIX, 
                           XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
                           "nil", "true");
      }
    }
    else {
      if (obj == null) return;

      QName qname = getQName();

      if (qname.getNamespaceURI() == null || "".equals(qname.getNamespaceURI()))
        out.writeStartElement(qname.getLocalPart());
      else
        out.writeStartElement(qname.getNamespaceURI(), qname.getLocalPart());
    }*/
  }

  protected void writeEndElement(XMLEventWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    /* XXX convert to event based
    XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);
    XmlElement element = getAnnotation(XmlElement.class);

    if (wrapper != null) {
      if (obj == null && !wrapper.nillable())
        return;
    } 
    else if (element != null) {
      if (obj == null && !element.nillable())
        return;
    } 
    else {
      if (obj == null) return;
    }

    out.writeEndElement();*/
  }

  private QName getTypeQName()
  {
    if (_typeQName == null) {
      XmlType xmlType = getAnnotation(XmlType.class);

      String name = getName();
      String namespace = null; // XXX package namespace

      if (xmlType != null) {
        if (! xmlType.name().equals("#default"))
          name = xmlType.name();
        if (! xmlType.namespace().equals("#default"))
          namespace = xmlType.namespace();
      }

      if (namespace == null)
        _typeQName = new QName(name);
      else
        _typeQName = new QName(namespace, name);
    }

    return _typeQName;
  }

  protected QName getQName()
  {
    if (_qname == null) {
      XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);
      XmlElement element = getAnnotation(XmlElement.class);

      String name = getName();
      // XXX Namespace inheritance (@XmlSchema.elementFormDefault)
      String namespace = null;

      if (wrapper != null) {
        if (! wrapper.name().equals("##default"))
          name = wrapper.name();

        if (! wrapper.namespace().equals("##default"))
          namespace = wrapper.namespace();
      }
      else if (element != null) {
        if (! element.name().equals("##default"))
          name = element.name();

        if (! element.namespace().equals("##default"))
          namespace = element.namespace();
      }

      if (namespace == null)
        _qname = new QName(name);
      else
        _qname = new QName(namespace, name);
    }

    return _qname;
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    XmlAttribute attribute = getAnnotation(XmlAttribute.class);

    if (attribute != null) {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, "attribute", 
                            XMLConstants.W3C_XML_SCHEMA_NS_URI);

      // See http://forums.java.net/jive/thread.jspa?messageID=167171
      // Primitives are always required

      if (attribute.required() || 
          (_generateRICompatibleSchema && getType().isPrimitive()))
        out.writeAttribute("use", "required");
    }
    else {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                            XMLConstants.W3C_XML_SCHEMA_NS_URI);

      XmlElement element = getAnnotation(XmlElement.class);

      if (! _generateRICompatibleSchema || ! getType().isPrimitive()) {
        if (element != null) {
          if (element.required())
            out.writeAttribute("minOccurs", "1");
          else
            out.writeAttribute("minOccurs", "0");

          if (element.nillable())
            out.writeAttribute("nillable", "true");
        }
        else
          out.writeAttribute("minOccurs", "0");
      }

      if (_property.getMaxOccurs() != null)
        out.writeAttribute("maxOccurs", _property.getMaxOccurs());
    }

    out.writeAttribute("type", _property.getSchemaType());
    out.writeAttribute("name", getName());
  }

  public boolean isXmlPrimitiveType()
  {
    return _property.isXmlPrimitiveType();
  }

  public String getSchemaType()
  {
    return _property.getSchemaType();
  }

  public abstract Object get(Object o) throws JAXBException;
  public abstract void set(Object o, Object value) throws JAXBException;
  public abstract String getName();
  public abstract Class getType();
  public abstract Type getGenericType();
  public abstract <A extends Annotation> A getAnnotation(Class<A> c);
}
