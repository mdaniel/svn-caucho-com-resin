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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** an Accessor is either a getter/setter pair or a field */
public abstract class Accessor implements Namer {
  public static final L10N L = new L10N(Accessor.class);

  public static final String XML_SCHEMA_PREFIX = "xsd";
  public static final String XML_INSTANCE_PREFIX = "xsi";
  public static final String XML_MIME_NS = "http://www.w3.org/2005/05/xmlmime";

  public static final Comparator<Accessor> nameComparator 
    = new Comparator<Accessor>() {
      public int compare(Accessor a1, Accessor a2) 
      {
        return a1.getName().compareTo(a2.getName());
      }

      public boolean equals(Object o)
      {
        return this == o;
      }
    };

  private static boolean _generateRICompatibleSchema = true;

  protected int _order = -1;
  protected JAXBContextImpl _context;

  protected Property _property;
  protected String _name;

  protected QName _qname = null;
  protected HashMap<Class,QName> _qnameMap = null;

  protected QName _typeQName = null;
  protected AccessorType _accessorType = AccessorType.UNSET;

  public static void setGenerateRICompatibleSchema(boolean compatible)
  {
    _generateRICompatibleSchema = compatible;
  }

  protected Accessor()
  {
  }

  protected Accessor(JAXBContextImpl context)
  {
    _context = context;
  }

  protected void init()
    throws JAXBException
  {
    // XXX wrapper

    XmlElement element = getAnnotation(XmlElement.class);

    if (element != null && ! "##default".equals(element.name()))
      _name = element.name();
    else {
      XmlAttribute attribute = getAnnotation(XmlAttribute.class);
      
      if (attribute != null && ! "##default".equals(attribute.name()))
        _name = attribute.name();
    }

    XmlID xmlID = getAnnotation(XmlID.class);

    // jaxb/02d1
    if (xmlID != null && ! String.class.equals(getType()))
      throw new JAXBException(L.l("Fields or properties annotated with XmlID must have type String: {0}", this));
    
    XmlMimeType xmlMimeType = getAnnotation(XmlMimeType.class);
    String mimeType = null;
    
    if (xmlMimeType != null)
      mimeType = xmlMimeType.value();

    switch (getAccessorType()) {
      case ELEMENT: 
        {
          // XXX respect the type from the XmlElement annotation

          boolean xmlList = (getAnnotation(XmlList.class) != null);

          _property = 
            _context.createProperty(getGenericType(), false, mimeType, xmlList);

          if (element != null)
            _qname = qnameFromXmlElement(element);
          else {
            XmlSchema xmlSchema = getPackageAnnotation(XmlSchema.class);

            if (xmlSchema != null &&
                xmlSchema.elementFormDefault() == XmlNsForm.QUALIFIED)
              _qname = new QName(xmlSchema.namespace(), getName());
            else
              _qname = new QName(getName());

            if (! _property.isXmlPrimitiveType())
              _context.createSkeleton(getType());
          }

          XmlElementWrapper wrapper = getAnnotation(XmlElementWrapper.class);

          if (wrapper != null) {
            WrapperProperty wrapperProperty = 
              new WrapperProperty(_property, wrapper, 
                                  _qname.getNamespaceURI(), 
                                  _qname.getLocalPart());

            _property = wrapperProperty;
            _qname = wrapperProperty.getWrapperQName();
          }

          break;
        }

      case ATTRIBUTE: 
        {
          XmlAttribute attribute = getAnnotation(XmlAttribute.class);

          String name = getName();
          String namespace = null;

          if (attribute != null) {
            if (! attribute.name().equals("##default"))
              name = attribute.name();

            if (! attribute.namespace().equals("##default"))
              namespace = attribute.namespace();
          }

          if (namespace == null)
            _qname = new QName(name);
          else
            _qname = new QName(namespace, name);

          // XXX XmlList value?
          _property =
            _context.createProperty(getGenericType(), false, mimeType);

          break;
        }

      case ANY_ATTRIBUTE:
        {
          if (! Map.class.isAssignableFrom(getType()))
            throw new JAXBException(L.l("Fields or properties annotated with @XmlAnyAttribute must be Maps"));
          break;
        }

      case VALUE:
        {
          // XXX
          _qname = new QName(getName());

          _property = _context.createProperty(getGenericType(), false, mimeType,
                                              true, true);

          if (! _property.isXmlPrimitiveType() && 
              ! Collection.class.isAssignableFrom(getType()))
            throw new JAXBException(L.l("XmlValue must be either a collection or a simple type"));

          break;
        }

      case ELEMENT_REF:
        break;

      case ELEMENTS:
        {
          if (getAnnotation(XmlList.class) != null)
            throw new JAXBException(L.l("@XmlList cannot be used with @XmlElements"));

          XmlElements elements = getAnnotation(XmlElements.class);

          if (elements.value().length == 0) {
            // XXX special case : equivalent to unannotated
          }

          if (elements.value().length == 1) {
            // XXX special case : equivalent to @XmlElement
          }

          _qnameMap = new LinkedHashMap<Class,QName>();
          Map<QName,Property> qnameToPropertyMap = 
            new LinkedHashMap<QName,Property>();
          Map<Class,Property> classToPropertyMap = 
            new LinkedHashMap<Class,Property>();

          for (int i = 0; i < elements.value().length; i++) {
            element = elements.value()[i];

            if (XmlElement.DEFAULT.class.equals(element.type()))
              throw new JAXBException(L.l("@XmlElement annotations in @XmlElements must specify a type"));

            QName qname = qnameFromXmlElement(element);
            Property property = _context.createProperty(element.type());

            qnameToPropertyMap.put(qname, property);
            classToPropertyMap.put(element.type(), property);
            _qnameMap.put(element.type(), qname);

            if (! property.isXmlPrimitiveType())
              _context.createSkeleton(element.type());
          }

          _property = new MultiProperty(qnameToPropertyMap, classToPropertyMap);

          if (List.class.isAssignableFrom(getType()))
            _property = new ListProperty(_property);
          else if (getType().isArray()) {
            Class cType = getType().getComponentType();
            _property = ArrayProperty.createArrayProperty(_property, cType);
          }

          // XXX Wrapper

          break;
        }

      case ANY_TYPE_ELEMENT_LAX:
        _qname = new QName(getName());
        _property = _context.createProperty(getGenericType(), true);
        break;

      case ANY_TYPE_ELEMENT:
        _qname = new QName(getName());
        _property = _context.createProperty(getGenericType(), false); // XXX?
        break;
    }
  }

  public void putQNames(HashMap<QName,Accessor> map)
    throws JAXBException
  {
    if (_qname != null) {
      if (map.containsKey(_qname))
        throw new JAXBException(L.l("Class contains two elements with the same QName {0}", _qname));

      map.put(_qname, this);
    }
    else if (getAccessorType() == AccessorType.ELEMENT_REF) {
      // if discovery isn't done yet, then we may not get all the root
      // elements when we call _context.getRootElements(), so we should
      // abort at this point
      if (! _context.isDiscoveryFinished())
        return;

      Class cl = getType();

      // XXX process this
      XmlElementRef elementRef = getAnnotation(XmlElementRef.class);

      if (Collection.class.isAssignableFrom(cl)) {
        if (getGenericType() instanceof ParameterizedType) {
          ParameterizedType ptype = (ParameterizedType) getGenericType();
          Type[] args = ptype.getActualTypeArguments();

          if (args.length != 1)
            throw new JAXBException(L.l("Collections annotated with @XmlElementRef must be parameterized: {0}", getName()));
          else if (args[0] instanceof Class)
            cl = (Class) args[0];
          else
            throw new JAXBException(L.l("Unknown type {0} on field or property {1}", args[0], getName()));
        }
        else
          throw new JAXBException(L.l("Collections annotated with @XmlElementRef must be parameterized: {0}", getName()));
      }
      else if (cl.isArray())
        cl = cl.getComponentType();

      List<ClassSkeleton> skeletons = _context.getRootElements(cl);

      if (skeletons.size() == 0)
        throw new JAXBException(L.l("The type ({0}) of field {1} is unknown to this context", cl, getName()));

      Map<QName,Property> qnameToPropertyMap = new HashMap<QName,Property>();
      Map<Class,Property> classToPropertyMap = new HashMap<Class,Property>();

      for (int i = 0; i < skeletons.size(); i++) {
        ClassSkeleton skeleton = skeletons.get(i);
        map.put(skeleton.getElementName(), this);

        QName qname = skeleton.getElementName();
        Property property = _context.createProperty(skeleton.getType());

        qnameToPropertyMap.put(qname, property);
        classToPropertyMap.put(skeleton.getType(), property);
      }

      _property = new MultiProperty(qnameToPropertyMap, classToPropertyMap);

      if (List.class.isAssignableFrom(getType()))
        _property = new ListProperty(_property);
      else if (getType().isArray()) {
        Class cType = getType().getComponentType();
        _property = ArrayProperty.createArrayProperty(_property, cType);
      }
    }
    else {
      for (QName qname : _qnameMap.values()) {
        if (map.containsKey(qname))
          throw new JAXBException(L.l("Class contains two elements with the same QName {0}", qname));

        map.put(qname, this);
      }
    }
  }

  public void setOrder(int order)
  {
    _order = order;
  }

  public int getOrder()
  {
    return _order;
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

  public void writeAttribute(Marshaller m, XMLStreamWriter out, Object value)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      QName name = getQName(value);

      if (name.getNamespaceURI() == null || "".equals(name.getNamespaceURI()))
        out.writeAttribute(name.getLocalPart(), value.toString());
      else if (name.getPrefix() == null || "".equals(name.getPrefix())) {
        out.writeAttribute(name.getNamespaceURI(), name.getLocalPart(), 
                           value.toString());
      }
      else {
        out.writeAttribute(name.getPrefix(), 
                           name.getNamespaceURI(), 
                           name.getLocalPart(), 
                           value.toString());
      }
    }
  }

  public void writeAnyAttribute(Marshaller m, XMLStreamWriter out, Map map)
    throws IOException, XMLStreamException, JAXBException
  {
    if (map != null) {
      for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
        QName name = (QName) entry.getKey();
        String value = (String) entry.getValue();

        if (name.getNamespaceURI() == null || "".equals(name.getNamespaceURI()))
          out.writeAttribute(name.getLocalPart(), value.toString());
        else if (name.getPrefix() == null || "".equals(name.getPrefix())) {
          out.writeAttribute(name.getNamespaceURI(), name.getLocalPart(), 
                             value.toString());
        }
        else {
          out.writeAttribute(name.getPrefix(), 
                             name.getNamespaceURI(), 
                             name.getLocalPart(), 
                             value.toString());
        }
      }
    }
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = get(obj);

    switch (getAccessorType()) {
      case ATTRIBUTE:
        writeAttribute(m, out, value);
        break;
        
      case ANY_ATTRIBUTE:
        writeAnyAttribute(m, out, (Map) value);
        break;

      default:
        _property.write(m, out, value, this, obj);
        break;
    }
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object obj, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = get(obj);

    _property.write(m, out, value, this, obj, attributes);
  }

  public void writeAttribute(Marshaller m, XMLEventWriter out, Object value)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      QName name = getQName(value);
      out.add(JAXBUtil.EVENT_FACTORY.createAttribute(name, value.toString()));
    }
  }

  public void write(Marshaller m, XMLEventWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = get(obj);

    if (getAccessorType() == AccessorType.ATTRIBUTE)
      writeAttribute(m, out, value);
    else
      _property.write(m, out, value, this, obj);
  }

  public void write(Marshaller m, XMLEventWriter out, 
                    Object obj, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = get(obj);

    _property.write(m, out, value, this, obj, attributes);
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj)
    throws IOException, JAXBException
  {
    Object value = get(obj);

    return _property.bindTo(binder, node, value, this);
  }

  // Input methods.  Contract: input stream or node iterator will be at
  // a start element.
  

  public Object readAttribute(XMLStreamReader in, int i, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    if (getAccessorType() == AccessorType.ANY_ATTRIBUTE) {
      Map map = (Map) get(obj);

      if (map == null)
        map = new HashMap(); // XXX other maps

      map.put(in.getAttributeName(i), in.getAttributeValue(i));

      return map;
    }
    else 
      return _property.readAttribute(in, i);
  }

  public Object readAttribute(Attribute attribute, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.readAttribute(attribute);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object parent, 
                     ClassSkeleton attributed)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, get(parent), attributed, parent);
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object parent,
                     ClassSkeleton attributed)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, get(parent), attributed, parent);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, get(parent));
  }

  public Object read(Unmarshaller u, XMLEventReader in, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    return _property.read(u, in, get(parent));
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object parent)
    throws IOException, JAXBException
  {
    return _property.bindFrom(binder, node, get(parent));
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

      QName qname = getQName(obj);

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
    // XXX choice
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

  private QName qnameFromXmlElementWrapper(XmlElementWrapper wrapper)
  {
    String name = getName();
    // XXX Namespace inheritance (@XmlSchema.elementFormDefault)
    String namespace = null;

    if (! wrapper.name().equals("##default"))
      name = wrapper.name();

    if (! wrapper.namespace().equals("##default"))
      namespace = wrapper.namespace();

    if (namespace == null)
      return new QName(name);
    else
      return new QName(namespace, name);
  }

  private QName qnameFromXmlElement(XmlElement element)
  {
    String name = getName();
    String namespace = null;

    XmlSchema xmlSchema = getPackageAnnotation(XmlSchema.class);

    if (xmlSchema != null &&
        xmlSchema.elementFormDefault() == XmlNsForm.QUALIFIED)
      namespace = xmlSchema.namespace();

    if (! element.name().equals("##default"))
      name = element.name();

    if (! element.namespace().equals("##default"))
      namespace = element.namespace();

    if (namespace == null)
      return new QName(name);
    else
      return new QName(namespace, name);
  }

  public QName getQName(Object obj)
    throws JAXBException
  {
    if (_qname != null)
      return _qname;

    if (_qnameMap != null)
      return _qnameMap.get(obj.getClass());

    if (getAccessorType() == AccessorType.ELEMENT_REF) {
      if (obj instanceof JAXBElement) {
        JAXBElement element = (JAXBElement) obj;

        return element.getName();
      }
      else {
        ClassSkeleton skeleton = _context.findSkeletonForObject(obj);

        if (skeleton == null || skeleton.getElementName() == null)
          throw new JAXBException(L.l("Cannot find root element name for object {0}", obj));

        return skeleton.getElementName();
      }
    }

    throw new JAXBException(L.l("Internal error: Unable to find QName for object {0} in {1}", obj, this));
  }

  protected void setQName(QName qname)
  {
    _qname = qname;
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    switch (getAccessorType()) {
      case ELEMENTS:
        {
          out.writeStartElement(XML_SCHEMA_PREFIX, "choice", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);
          out.writeAttribute("minOccurs", "0");

          if (_property.getMaxOccurs() != null)
            out.writeAttribute("maxOccurs", _property.getMaxOccurs());

          MultiProperty multiProperty = null;

          if (_property instanceof ListProperty) {
            ListProperty listProperty = (ListProperty) _property;
            multiProperty = (MultiProperty) listProperty.getComponentProperty();
          }
          else
            multiProperty = (MultiProperty) _property;

          Collection<Property> properties = multiProperty.getProperties();

          XmlElements xmlElements = getAnnotation(XmlElements.class);
          XmlElement[] elements = xmlElements.value();

          int i = 0; 

          for (Property property : properties) {
            out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                                  XMLConstants.W3C_XML_SCHEMA_NS_URI);

            out.writeAttribute("type", property.getSchemaType());

            if ("##default".equals(elements[i].name()))
              out.writeAttribute("name", getName());
            else 
              // XXX namespace
              out.writeAttribute("name", elements[i].name());

            i++;
          }

          out.writeEndElement(); // choice
        }

        break;

      case ANY_ATTRIBUTE:
        {
          out.writeStartElement(XML_SCHEMA_PREFIX, "anyAttribute", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);
          out.writeAttribute("namespace", "##other");
          out.writeAttribute("processContents", "skip");
        }

        break;

      case ATTRIBUTE:
        {
          out.writeEmptyElement(XML_SCHEMA_PREFIX, "attribute", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);

          // See http://forums.java.net/jive/thread.jspa?messageID=167171
          // Primitives are always required

          XmlAttribute attribute = getAnnotation(XmlAttribute.class);

          if (attribute.required() || 
              (_generateRICompatibleSchema && getType().isPrimitive()))
            out.writeAttribute("use", "required");

          XmlID xmlID = getAnnotation(XmlID.class);

          if (xmlID != null)
            out.writeAttribute("type", "xsd:ID"); // jaxb/22d0
          else
            out.writeAttribute("type", _property.getSchemaType());

          out.writeAttribute("name", getName());
        }

        break;

      case ANY_TYPE_ELEMENT:
        {
          out.writeEmptyElement(XML_SCHEMA_PREFIX, "any", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);
          out.writeAttribute("processContents", "skip");

          if (_property.getMaxOccurs() != null)
            out.writeAttribute("maxOccurs", _property.getMaxOccurs());
        }

        break;

      case ANY_TYPE_ELEMENT_LAX:
        {
          out.writeEmptyElement(XML_SCHEMA_PREFIX, "any", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);
          out.writeAttribute("processContents", "lax");

          if (_property.getMaxOccurs() != null)
            out.writeAttribute("maxOccurs", _property.getMaxOccurs());
        }

        break;

      case ELEMENT_REF:
        {
          out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                                XMLConstants.W3C_XML_SCHEMA_NS_URI);

          XmlElementRef elementRef = getAnnotation(XmlElementRef.class);

          if (_property.getMaxOccurs() != null)
            out.writeAttribute("maxOccurs", _property.getMaxOccurs());

          out.writeAttribute("ref", "XXX");
        }
      
      case ELEMENT:
        {
          XmlElement element = getAnnotation(XmlElement.class);
          XmlList xmlList = getAnnotation(XmlList.class);

          if (xmlList != null) {
            out.writeStartElement(XML_SCHEMA_PREFIX, "element", 
                                  XMLConstants.W3C_XML_SCHEMA_NS_URI);
            out.writeAttribute("name", getName());

            out.writeStartElement(XML_SCHEMA_PREFIX, "simpleType", 
                                  XMLConstants.W3C_XML_SCHEMA_NS_URI);

            out.writeEmptyElement(XML_SCHEMA_PREFIX, "list", 
                                  XMLConstants.W3C_XML_SCHEMA_NS_URI);
            out.writeAttribute("itemType", _property.getSchemaType());

            out.writeEndElement(); // simpleType

            out.writeEndElement(); // element
          }
          else {
            out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                                  XMLConstants.W3C_XML_SCHEMA_NS_URI);

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

            if ((element != null && element.nillable()) ||
                Collection.class.isAssignableFrom(getType()) ||
                (getType().isArray() && 
                 ! byte.class.equals(getType().getComponentType())))
              out.writeAttribute("nillable", "true");


            String typeName = _property.getSchemaType();

            // jaxb/22d0
            if (getAnnotation(XmlID.class) != null)
              typeName = "xsd:ID";

            // look for the XmlSchemaType
            XmlSchemaType xmlSchemaType = getAnnotation(XmlSchemaType.class);

            if (xmlSchemaType == null) {
              xmlSchemaType = getPackageAnnotation(XmlSchemaType.class);

              if (xmlSchemaType != null) {
                if (XmlSchemaType.DEFAULT.class.equals(xmlSchemaType.type()))
                  throw new JAXBException(L.l("@XmlSchemaType with name {0} on package {1} does not specify type", xmlSchemaType.name(), getPackage().getName()));

                if (! getType().equals(xmlSchemaType.type()))
                  xmlSchemaType = null;
              }
            }

            if (xmlSchemaType == null) {
              XmlSchemaTypes xmlSchemaTypes = 
                getPackageAnnotation(XmlSchemaTypes.class);

              if (xmlSchemaTypes != null) {
                XmlSchemaType[] array = xmlSchemaTypes.value();

                for (int i = 0; i < array.length; i++) {
                  xmlSchemaType = array[i];

                  if (XmlSchemaType.DEFAULT.class.equals(xmlSchemaType.type()))
                    throw new JAXBException(L.l("@XmlSchemaType with name {0} on package {1} does not specify type", xmlSchemaType.name(), getPackage().getName()));

                  if (getType().equals(xmlSchemaType.type()))
                    break;

                  xmlSchemaType = null;
                }
              }
            }

            if (xmlSchemaType != null) {
              QName typeQName = new QName(xmlSchemaType.namespace(),
                                          xmlSchemaType.name());
              typeName = StaxUtil.qnameToString(out, typeQName);
            }

            out.writeAttribute("type", typeName);
            out.writeAttribute("name", getName());

            XmlMimeType xmlMimeType = getAnnotation(XmlMimeType.class);

            if (xmlMimeType != null) {
              out.writeAttribute(XML_MIME_NS, "expectedContentTypes", 
                                 xmlMimeType.value());
            }
          }
        }

        break;
    }
  }

  public boolean isNillable()
  {
    // XXX propertyMap
    return _property.isNillable();
  }

  public String getSchemaType()
  {
    // XXX propertyMap
    return _property.getSchemaType();
  }

  public AccessorType getAccessorType()
    throws JAXBException
  {
    if (_accessorType != AccessorType.UNSET)
      return _accessorType;

    XmlAnyAttribute xmlAnyAttribute = getAnnotation(XmlAnyAttribute.class);
    XmlAnyElement xmlAnyElement = getAnnotation(XmlAnyElement.class);
    XmlAttribute xmlAttribute = getAnnotation(XmlAttribute.class);
    XmlElement xmlElement = getAnnotation(XmlElement.class);
    XmlElementRef xmlElementRef = getAnnotation(XmlElementRef.class);
    XmlElements xmlElements = getAnnotation(XmlElements.class);
    XmlID xmlID = getAnnotation(XmlID.class);
    XmlIDREF xmlIDREF = getAnnotation(XmlIDREF.class);
    XmlValue xmlValue = getAnnotation(XmlValue.class);

    Annotation[] annotations = new Annotation[] {
      xmlAnyAttribute, xmlAnyElement, xmlAttribute, xmlElement, 
      xmlElementRef, xmlElements, xmlIDREF, xmlValue
    };

    int count = 0;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < annotations.length; i++) {
      if (annotations[i] != null) {
        count++;
        sb.append("@" + annotations[i].getClass().getName());
        sb.append(", ");
      }
    }

    if (count > 1) {
      throw new JAXBException(L.l("Annotations {0} cannot be used together on a single field or property", sb.toString()));
    }

    if (xmlValue != null)
      _accessorType = AccessorType.VALUE;

    else if (xmlAnyAttribute != null)
      _accessorType = AccessorType.ANY_ATTRIBUTE;

    else if (xmlAttribute != null)
      _accessorType = AccessorType.ATTRIBUTE;

    else if (xmlElementRef != null) {
      _accessorType = AccessorType.ELEMENT_REF;
    }

    else if (xmlElements != null)
      _accessorType = AccessorType.ELEMENTS;

    else {

      if (xmlAnyElement != null) {
        if (xmlAnyElement.lax())
          _accessorType = AccessorType.ANY_TYPE_ELEMENT_LAX;
        else
          _accessorType = AccessorType.ANY_TYPE_ELEMENT;
      }
      else
        _accessorType = AccessorType.ELEMENT;
    }

    return _accessorType;
  }

  public enum AccessorType {
    UNSET, 
    VALUE, 
    ANY_ATTRIBUTE, 
    ATTRIBUTE, 
    ELEMENT, 
    ELEMENT_REF, 
    ELEMENTS, 
    ANY_TYPE_ELEMENT, 
    ANY_TYPE_ELEMENT_LAX
  }

  public abstract Object get(Object o) throws JAXBException;
  public abstract void set(Object o, Object value) throws JAXBException;
  public abstract String getName();
  public abstract Class getType();
  public abstract Type getGenericType();
  public abstract Package getPackage();
  public abstract <A extends Annotation> A getAnnotation(Class<A> c);
  public abstract <A extends Annotation> A getPackageAnnotation(Class<A> c);
}
