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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import com.caucho.jaxb.JAXBContextImpl;

public class Skeleton<C> {
  private static final Logger log = Logger.getLogger(Skeleton.class.getName());

  private JAXBContextImpl _context;
  private Class<C> _class;

  private Method _beforeUnmarshal;
  private Method _afterUnmarshal;
  private Method _beforeMarshal;
  private Method _afterMarshal;

  private HashMap<String,Property> _properties = 
    new HashMap<String,Property>();

  public Class<C> getType()
  {
    return _class;
  }
  
  public Skeleton(JAXBContextImpl context, Class<C> c)
    throws JAXBException
  {
    try {
      _context = context;
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
      
      // XXX: look for @XmlValue annotation
      
      if (c.isAnnotationPresent(XmlRootElement.class)) {
        XmlRootElement xre = c.getAnnotation(XmlRootElement.class);
        if (!"##default".equals(xre.name())) {
          QName qname = null;
          if ("##default".equals(xre.namespace()))
            qname = new QName(xre.name());
          else
            qname = new QName(xre.namespace(), xre.name());
          _context.addRootElement(qname, this);
        }
      }
      
      for(Field f : c.getFields()) {
        if ((f.getModifiers() & Modifier.PUBLIC) == 0) continue;
        if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
        if (f.isAnnotationPresent(XmlTransient.class))
          continue;

        Accessor a = new Accessor.FieldAccessor(f, _context);
        Property p = _context.createProperty(a);
        _properties.put(p.getName(), p);
      }
      
      // XXX: getter/setter methods
    }
    catch (Exception e) {
      throw new JAXBException(e);
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
        
        if (!"".equals(xmlType.factoryMethod())) {
          Method m = factoryClass.getMethod(xmlType.factoryMethod(),
                                            new Class[] { });
          // XXX: make sure m is static
          return (C)m.invoke(null);
        }
      }
      
      Constructor con = _class.getConstructor(new Class[] { });
      return (C)con.newInstance(new Object[] { });
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  /*
    private Iterable<String> propNames()
    {
    XmlType xmlType = getXmlType();
    if (xmlType != null) {
    if (xmlType.propOrder() == 
    }
    return null;
    }
  */

  public XmlType getXmlType()
  {
    return (XmlType)_class.getAnnotation(XmlType.class);
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    try {
      Constructor constructor = _class.getConstructor(new Class[] { });
      C ret = (C) constructor.newInstance();
      in.next();

      if (_beforeUnmarshal != null)
        _beforeUnmarshal.invoke(ret, /*FIXME*/ null, null);
      if (u.getListener() != null)
        u.getListener().beforeUnmarshal(ret, null);
      
      while (in.getEventType() != -1) {
        if (in.getEventType() == in.START_ELEMENT) {
          Property prop = getProperty(in.getName());
          Object val = prop.read(u, in);
          prop.set(ret, val);
        } 
        else if (in.getEventType() == in.END_ELEMENT) {
          in.next();
          break;
        }
        in.next();
      }

      if (_afterUnmarshal != null)
        _afterUnmarshal.invoke(ret, /*FIXME*/ null, null);
      if (u.getListener() != null)
        u.getListener().afterUnmarshal(ret, null);
      
      return ret;
    }
    catch (NoSuchMethodException e) {
      throw new JAXBException(e);
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
  
  private Property getProperty(QName q)
  {
    // XXX
    return _properties.get(q.getLocalPart());
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
      
      QName tagName = getElementName((C) obj);
      
      if (tagName == null)
        tagName = fieldName;
      
      if (fieldName.getNamespaceURI() == null ||
          fieldName.getNamespaceURI().equals(""))
        out.writeStartElement(tagName.getLocalPart());
      else
        out.writeStartElement(tagName.getNamespaceURI(),
                              tagName.getLocalPart());
      
      for(Property p : _properties.values())
        p.write(m, out, p.get(obj));
      
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

  public QName getElementName(C object)
  {
    QName tagName = null;

    /*
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
    */

    if (tagName==null && _class.isAnnotationPresent(XmlElement.class)) {
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
  }
}
