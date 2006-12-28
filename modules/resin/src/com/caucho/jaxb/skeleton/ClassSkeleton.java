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
 * @author Adam Megacz
 */

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

import java.io.IOException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class ClassSkeleton<C> extends Skeleton {
  public static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";
  public static final String XML_SCHEMA_PREFIX = "xsd";

  private static final L10N L = new L10N(ClassSkeleton.class);
  private static final Logger log = Logger.getLogger(Skeleton.class.getName());

  private static final Class[] NO_PARAMS = new Class[0];
  private static final Object[] NO_ARGS = new Object[0];

  private Class<C> _class;

  private Method _beforeUnmarshal;
  private Method _afterUnmarshal;
  private Method _beforeMarshal;
  private Method _afterMarshal;

  private QName _elementName;

  private Constructor _constructor;

  /**
   * The value @XmlValue.
   * 
   **/
  private Accessor _value;

  public Class<C> getType()
  {
    return _class;
  }

  public String toString()
  {
    return "ClassSkeleton[" + _class + "]";
  }
  
  public ClassSkeleton(JAXBContextImpl context, Class<C> c)
    throws JAXBException
  {
    super(context);

    try {
      _class = c;

      try {
        _beforeUnmarshal =
          c.getMethod("beforeUnmarshal", Unmarshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _afterUnmarshal =
          c.getMethod("afterUnmarshal", Unmarshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _beforeMarshal =
          c.getMethod("beforeMarshal", Marshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _afterMarshal =
          c.getMethod("afterMarshal", Marshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      
      if (List.class.isAssignableFrom(_class)) {
        // XXX:
      }
      if (Set.class.isAssignableFrom(_class)) {
        // XXX:
      }
      if (HashMap.class.isAssignableFrom(_class)) {
        // XXX:
      }
      
      // XXX: @XmlJavaTypeAdapter
      
      // Find the zero-parameter constructor
      try {
        _constructor = c.getConstructor(NO_PARAMS);
        _constructor.setAccessible(true);
      }
      catch (Exception e1) {
        try {
          _constructor = c.getDeclaredConstructor(NO_PARAMS);
          _constructor.setAccessible(true);
        }
        catch (Exception e2) {
          throw new JAXBException(L.l("Zero-arg constructor not found for class {0}", c.getName()), e2);
        }
      }

      _typeName = new QName(JAXBUtil.getXmlSchemaDatatype(_class));
      
      if (c.isAnnotationPresent(XmlRootElement.class)) {
        XmlRootElement xre = c.getAnnotation(XmlRootElement.class);

        String localName = null;
        
        if ("##default".equals(xre.name()))
          localName = JAXBUtil.identifierToXmlName(_class);
        else
          localName = xre.name();

        if ("##default".equals(xre.namespace()))
          _elementName = new QName(localName);
        else
          _elementName = new QName(xre.namespace(), localName);

        _typeName = _elementName;

        _context.addRootElement(this);
      }

      XmlAccessorType accessorType = c.getAnnotation(XmlAccessorType.class);
      XmlAccessType accessType = (accessorType == null ? 
                                  XmlAccessType.PUBLIC_MEMBER :
                                  accessorType.value());

      if (accessType != XmlAccessType.FIELD) {
        // getter/setter
        BeanInfo beanInfo = Introspector.getBeanInfo(c);

        for (PropertyDescriptor property : beanInfo.getPropertyDescriptors()) {
          // JAXB specifies that a "class" property must be specified as "clazz"
          // because of Object.getClass()
          if ("class".equals(property.getName()))
            continue;

          Method get = property.getReadMethod();
          Method set = property.getWriteMethod();

          if (property.getPropertyType() == null) {
            continue;
          }

          if (get != null && get.isAnnotationPresent(XmlTransient.class))
            continue;
          if (set != null && set.isAnnotationPresent(XmlTransient.class))
            continue;

          addAccessor(new GetterSetterAccessor(property, _context));
        }
      } 

      if (accessType != XmlAccessType.PROPERTY) {
        Field[] fields = c.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);

        for (Field f : fields) {
          if (Modifier.isStatic(f.getModifiers()))
            continue;
          if (f.isAnnotationPresent(XmlTransient.class))
            continue;
          // jaxb/0176: transient modifier ignored

          if (accessType == XmlAccessType.PUBLIC_MEMBER
              && ! Modifier.isPublic(f.getModifiers()))
            continue;

          // XXX : Other annotations?
          if (accessType == XmlAccessType.NONE &&
              ! f.isAnnotationPresent(XmlElement.class) &&
              ! f.isAnnotationPresent(XmlAttribute.class))
            continue;

          addAccessor(new FieldAccessor(f, _context));
        }
      }
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  private void addAccessor(Accessor a)
    throws JAXBException
  {
    if (a.getAnnotation(XmlValue.class) != null) {
      if (_value != null)
        throw new JAXBException(L.l("Cannot have two @XmlValue annotated fields or properties"));

      if (! a.isXmlPrimitiveType() && 
          ! Collection.class.isAssignableFrom(a.getType()))
        throw new JAXBException(L.l("XmlValue must be either a collection or a simple type"));

      _value = a;
    }
    else if (a.getAnnotation(XmlAttribute.class) != null)
      _attributeAccessors.put(a.getName(), a);
    else {
      if (_value != null)
        throw new JAXBException(L.l("Cannot have both @XmlValue and elements in a JAXB element"));

      _elementAccessors.put(a.getName(), a);
    }

    // Make sure the field's type is in the context so that the
    // schema generates correctly
    /*
    if ((a instanceof IterableProperty) && 
        ! ((IterableProperty) p).getComponentProperty().isXmlPrimitiveType()) {
      Property compProp = ((IterableProperty) p).getComponentProperty();
      _context.createSkeleton(compProp.getAccessor().getType());
    }
    else */


    if (! a.isXmlPrimitiveType()) {
      _context.createSkeleton(a.getType());
    }
  }

  public C newInstance()
    throws JAXBException
  {
    try {
      XmlType xmlType = getXmlType();
      
      if (xmlType != null) {
        Class factoryClass = xmlType.factoryClass();
        
        if (xmlType.factoryClass() == XmlType.DEFAULT.class)
          factoryClass = _class;
        
        if (! "".equals(xmlType.factoryMethod())) {
          Method m = factoryClass.getMethod(xmlType.factoryMethod(), NO_PARAMS);

          if (! Modifier.isStatic(m.getModifiers()))
            throw new JAXBException(L.l("Factory method not static"));

          return (C) m.invoke(null);
        }
      }
      
      Constructor con = _class.getConstructor(NO_PARAMS);

      return (C)con.newInstance(NO_ARGS);
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public XmlType getXmlType()
  {
    return (XmlType)_class.getAnnotation(XmlType.class);
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX: sequences/choice/etc
    try {
      C ret = (C) _constructor.newInstance();
      in.next();

      if (_beforeUnmarshal != null)
        _beforeUnmarshal.invoke(ret, /*FIXME*/ null, null);
      if (u.getListener() != null)
        u.getListener().beforeUnmarshal(ret, null);

      while (in.getEventType() == in.START_ELEMENT) {
        Accessor a = getAccessor(in.getName());
        Object val = a.read(u, in);
        a.set(ret, val);
      }

      if (_afterUnmarshal != null)
        _afterUnmarshal.invoke(ret, /*FIXME*/ null, null);
      if (u.getListener() != null)
        u.getListener().afterUnmarshal(ret, null);
      
      return ret;
    }
    catch (InstantiationException e) {
      throw new JAXBException(e);
    }
    catch (InvocationTargetException e) {
      throw new JAXBException(e);
    }
    catch (IllegalAccessException e) {
      throw new JAXBException(e);
    }
  }
  
  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
  {
    try {
      if (_beforeMarshal != null)
        _beforeMarshal.invoke(obj, /*FIXME*/ null, /*FIXME*/ null);

      if (m.getListener() != null)
        m.getListener().beforeMarshal(obj);
      
      QName tagName = _elementName; //getElementName((C) obj);
      
      if (tagName == null)
        tagName = fieldName;
      
      if (tagName.getNamespaceURI() == null ||
          tagName.getNamespaceURI().equals(""))
        out.writeStartElement(tagName.getLocalPart());
      else
        out.writeStartElement(tagName.getNamespaceURI(),
                              tagName.getLocalPart());

      for (Accessor a : _elementAccessors.values())
        a.write(m, out, a.get(obj));
      
      out.writeEndElement();
      
      if (_afterMarshal != null)
        _afterMarshal.invoke(obj, /*FIXME*/ null, /*FIXME*/ null);
      if (m.getListener() != null)
        m.getListener().afterMarshal(obj);
    }
    catch (InvocationTargetException e) {
      throw new JAXBException(e);
    }
    catch (IllegalAccessException e) {
      throw new JAXBException(e);
    }
  }

  public QName getElementName()
  {
    return _elementName;
    /*
    QName tagName = null;

    if (tagName==null && _class.isAnnotationPresent(XmlRootElement.class)) {
      XmlRootElement annotation = _class.getAnnotation(XmlRootElement.class);
      String localname = annotation.name();
      String namespace = annotation.namespace();

      if (localname.equals("##default"))
        tagName = null;
      else if (namespace.equals("##default"))
        tagName = new QName(localname);
      else
        tagName = new QName(namespace, localname);
    }

    if (tagName == null && _class.isAnnotationPresent(XmlElement.class)) {
      XmlElement annotation = _class.getAnnotation(XmlElement.class);
      String localname = annotation.name();
      String namespace = annotation.namespace();

      if (localname.equals("##default"))
        tagName = null;
      else if (namespace.equals("##default"))
        tagName = new QName(localname);
      else
        tagName = new QName(namespace, localname);
    }

    return tagName;
    */
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    if (_elementName != null) {

      if ("".equals(_typeName.getLocalPart()))
        out.writeStartElement(XML_SCHEMA_PREFIX, "element", XML_SCHEMA_NS);
      else {
        out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", XML_SCHEMA_NS);
        out.writeAttribute("type", _typeName.getLocalPart());
      }

      out.writeAttribute("name", _elementName.getLocalPart());
    }

    generateSchemaType(out);

    if (_elementName != null && "".equals(_typeName.getLocalPart()))
      out.writeEndElement(); // element
  }

  public void generateSchemaType(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    if (_value != null) {
      out.writeStartElement(XML_SCHEMA_PREFIX, "simpleType", XML_SCHEMA_NS);

      if (! "".equals(_typeName.getLocalPart()))
        out.writeAttribute("name", _typeName.getLocalPart());

      out.writeEmptyElement(XML_SCHEMA_PREFIX, "restriction", XML_SCHEMA_NS);
      out.writeAttribute("base", _value.getSchemaType());

      for (Accessor accessor : _attributeAccessors.values())
        accessor.generateSchema(out);

      out.writeEndElement(); // simpleType
    }
    else {
      out.writeStartElement(XML_SCHEMA_PREFIX, "complexType", XML_SCHEMA_NS);

      if (! "".equals(_typeName.getLocalPart()))
        out.writeAttribute("name", _typeName.getLocalPart());

      out.writeStartElement(XML_SCHEMA_PREFIX, "sequence", XML_SCHEMA_NS);

      for (Accessor accessor : _elementAccessors.values())
        accessor.generateSchema(out);

      out.writeEndElement(); // sequence

      for (Accessor accessor : _attributeAccessors.values())
        accessor.generateSchema(out);

      out.writeEndElement(); // complexType
    }
  }
}
