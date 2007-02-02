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

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.annotation.*;

import javax.xml.namespace.QName;

import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
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
  private Package _package;
  private Method _createMethod;
  private Object _factory;

  private Method _beforeUnmarshal;
  private Method _afterUnmarshal;
  private Method _beforeMarshal;
  private Method _afterMarshal;

  private Constructor _constructor;

  /**
   * The value @XmlValue.
   * 
   **/
  protected Accessor _value;

  public Class<C> getType()
  {
    return _class;
  }

  public String toString()
  {
    return "ClassSkeleton[" + _class + "]";
  }

  protected ClassSkeleton(JAXBContextImpl context)
  {
    super(context);
  }

  public ClassSkeleton(JAXBContextImpl context, Class<C> c)
    throws JAXBException
  {
    super(context);

    try {
      _class = c;
      _package = c.getPackage();

      // check for special before and after methods
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
        _beforeMarshal = c.getMethod("beforeMarshal", Marshaller.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _afterMarshal = c.getMethod("afterMarshal", Marshaller.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      if (Set.class.isAssignableFrom(_class)) {
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

      _typeName = JAXBUtil.getXmlSchemaDatatype(_class);
      _context.addXmlType(_typeName, this);

      // Check for the complete name of the element...
      String namespace = null;

      // look at package defaults first...
      if (_package.isAnnotationPresent(XmlSchema.class)) {
        XmlSchema schema = (XmlSchema) _package.getAnnotation(XmlSchema.class);

        if (! "".equals(schema.namespace()))
          namespace = schema.namespace();
      }
      
      // then look at class specific overrides.
      if (c.isAnnotationPresent(XmlRootElement.class)) {
        XmlRootElement xre = c.getAnnotation(XmlRootElement.class);

        String localName = null;
        
        if ("##default".equals(xre.name()))
          localName = JAXBUtil.identifierToXmlName(_class);
        else
          localName = xre.name();

        if (! "##default".equals(xre.namespace()))
          namespace = xre.namespace();

        if (namespace == null)
          _elementName = new QName(localName);
        else
          _elementName = new QName(namespace, localName);

        _typeName = _elementName;

        _context.addRootElement(this);
      }

      // order the elements, if specified
      XmlType xmlType = (XmlType) _class.getAnnotation(XmlType.class);
      HashMap<String,Integer> orderMap = null;

      if (xmlType != null &&
          (xmlType.propOrder().length != 1 ||
           ! "".equals(xmlType.propOrder()[0]))) {
        // non-default propOrder
        orderMap = new HashMap<String,Integer>();

        for (int i = 0; i < xmlType.propOrder().length; i++)
          orderMap.put(xmlType.propOrder()[i], i);
      }

      // Collect the fields/properties of the class
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

          // XXX special cases for Throwable specified in JAX-WS
          // Should it be in the general JAXB?
          if (Throwable.class.isAssignableFrom(c) && 
              ("stackTrace".equals(property.getName()) ||
               "cause".equals(property.getName()) ||
               "localizedMessage".equals(property.getName())))
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

          Accessor a = new GetterSetterAccessor(property, _context); 

          if (orderMap != null) {
            Integer i = orderMap.remove(property.getName());

            if (i != null)
              a.setOrder(i.intValue());
            // XXX else throw something?
          }

          addAccessor(a);
        }
      } 

      if (accessType != XmlAccessType.PROPERTY) {
        // XXX Don't overwrite property accessors
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

          Accessor a = new FieldAccessor(f, _context);

          if (orderMap != null) {
            Integer i = orderMap.remove(f.getName());

            if (i != null)
              a.setOrder(i.intValue());
            // XXX else throw something?
          }

          addAccessor(a);
        }
      }

    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  protected void addAccessor(Accessor a)
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

    if (! a.isXmlPrimitiveType())
      _context.createSkeleton(a.getType());
  }

  public QName getTypeName()
  {
    return _typeName;
  }

  public void setCreateMethod(Method createMethod, Object factory)
  {
    _createMethod = createMethod;
    _factory = factory;
  }

  public C newInstance()
    throws JAXBException
  {
    try {
      if (_createMethod != null && _factory != null) {
        return (C) _createMethod.invoke(_factory);
      }
      else {
        // XXX move into constructor
        XmlType xmlType = getXmlType();

        if (xmlType != null) {
          Class factoryClass = xmlType.factoryClass();

          if (xmlType.factoryClass() == XmlType.DEFAULT.class)
            factoryClass = _class;

          if (! "".equals(xmlType.factoryMethod())) {
            Method m = 
              factoryClass.getMethod(xmlType.factoryMethod(), NO_PARAMS);

            if (! Modifier.isStatic(m.getModifiers()))
              throw new JAXBException(L.l("Factory method not static"));

            return (C) m.invoke(null);
          }
        }

        Constructor con = _class.getConstructor(NO_PARAMS);

        return (C)con.newInstance(NO_ARGS);
      }
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
    try {
      C ret = newInstance();

      if (_beforeUnmarshal != null)
        _beforeUnmarshal.invoke(ret, u, /*FIXME : parent*/ null);

      if (u.getListener() != null)
        u.getListener().beforeUnmarshal(ret, null);

      if (_value != null) { 
        Object val = _value.read(u, in);
        _value.set(ret, val);
      }
      else {
        int i = 0;
        in.nextTag();

        while (in.getEventType() == in.START_ELEMENT) {
          Accessor a = getAccessor(in.getName());

          if (a == null) {
            throw new UnmarshalException(L.l("Child <{0}> not found", 
                                             in.getName()));
          }

          if (! a.checkOrder(i++, u.getEventHandler())) {
            throw new UnmarshalException(L.l("Child <{0}> misordered", 
                                             in.getName()));
          }

          Object val = a.read(u, in);
          a.set(ret, val);
        }
      }

      if (_afterUnmarshal != null)
        _afterUnmarshal.invoke(ret, u, /*FIXME : parent*/ null);

      if (u.getListener() != null)
        u.getListener().afterUnmarshal(ret, null);

      return ret;
    }
    catch (InvocationTargetException e) {
      throw new UnmarshalException(e);
    }
    catch (IllegalAccessException e) {
      throw new UnmarshalException(e);
    }
  }

  public Object read(Unmarshaller u, XMLEventReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    try {
      C ret = newInstance();

      if (_beforeUnmarshal != null)
        _beforeUnmarshal.invoke(ret, u, /*FIXME : parent*/ null);

      if (u.getListener() != null)
        u.getListener().beforeUnmarshal(ret, null);


      if (_value != null) { 
        Object val = _value.read(u, in);
        _value.set(ret, val);
      }
      else {
        int i = 0;
        XMLEvent event = in.nextEvent();
        event = in.nextEvent();

        while (event.isStartElement()) {
          QName name = ((StartElement) event).getName();
          Accessor a = getAccessor(name);

          if (a == null)
            throw new UnmarshalException(L.l("Child <{0}> not found", name));

          if (! a.checkOrder(i++, u.getEventHandler()))
            throw new UnmarshalException(L.l("Child <{0}> misordered", name));

          Object val = a.read(u, in);
          a.set(ret, val);

          event = in.peek();
        }
      }

      if (_afterUnmarshal != null)
        _afterUnmarshal.invoke(ret, u, /*FIXME : parent*/ null);
      if (u.getListener() != null)
        u.getListener().afterUnmarshal(ret, null);
      
      return ret;
    }
    catch (InvocationTargetException e) {
      throw new JAXBException(e);
    }
    catch (IllegalAccessException e) {
      throw new JAXBException(e);
    }
  }

  public Object bindFrom(BinderImpl binder, Object existing, NodeIterator node)
    throws JAXBException
  {
    Node root = node.getNode();
    C ret = (C) existing;
    
    if (ret == null)
      ret = newInstance();

    if (_value != null) { 
      Object val = _value.bindFrom(binder, node);
      _value.set(ret, val);
    }
    else {
      int i = 0;
      Node child = node.firstChild();

      while (child != null) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
          QName name = JAXBUtil.qnameFromNode(child);

          Accessor a = getAccessor(name);

          if (a == null)
            throw new UnmarshalException(L.l("Child <{0}> not found", name));

          if (! a.checkOrder(i++, binder.getEventHandler()))
            throw new UnmarshalException(L.l("Child <{0}> misordered", name));

          Object val = a.bindFrom(binder, node);
          a.set(ret, val);
        }

        child = node.nextSibling();
      }
    }
    
    node.setNode(root);
    binder.bind(ret, root);

    return ret;
  }
  
  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj == null)
      return;

    try {
      if (_beforeMarshal != null)
        _beforeMarshal.invoke(obj, m);

      if (m.getListener() != null)
        m.getListener().beforeMarshal(obj);

      QName tagName = fieldName;

      if (tagName == null)
        tagName = _elementName;

      if (_value != null)
        _value.write(m, out, _value.get(obj));
      else {
        if (tagName.getNamespaceURI() == null ||
            "".equals(tagName.getNamespaceURI()))
          out.writeStartElement(tagName.getLocalPart());
        else if (tagName.getPrefix() == null ||
                 "".equals(tagName.getPrefix())) 
          out.writeStartElement(tagName.getNamespaceURI(),
                                tagName.getLocalPart());
        else
          out.writeStartElement(tagName.getPrefix(),
                                tagName.getLocalPart(),
                                tagName.getNamespaceURI());

        for (Accessor a : _elementAccessors.values())
          a.write(m, out, a.get(obj));
        
        out.writeEndElement();
      }
      
      if (_afterMarshal != null)
        _afterMarshal.invoke(obj, m);

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

  public void write(Marshaller m, XMLEventWriter out,
                    Object obj, QName fieldName)
    throws IOException, XMLStreamException, JAXBException
  {
    // XXX: beforeMarshal/afterMarshal???
    if (obj == null)
      return;

    try {
      if (_beforeMarshal != null)
        _beforeMarshal.invoke(obj, m);

      if (m.getListener() != null)
        m.getListener().beforeMarshal(obj);

      QName tagName = _elementName; //getElementName((C) obj);
      
      if (tagName == null)
        tagName = fieldName;

      if (_value != null)
        _value.write(m, out, _value.get(obj));
      else {
        out.add(JAXBUtil.EVENT_FACTORY.createStartElement(tagName, null, null));

        for (Accessor a : _elementAccessors.values())
          a.write(m, out, a.get(obj));

        out.add(JAXBUtil.EVENT_FACTORY.createEndElement(tagName, null));
      }
      
      if (_afterMarshal != null)
        _afterMarshal.invoke(obj, m);

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

  public Node bindTo(BinderImpl binder, Node node, Object obj, QName fieldName)
    throws JAXBException
  {
    if (obj == null)
      return null;

    QName tagName = fieldName;

    if (tagName == null)
      tagName = _elementName;

    if (_value != null) {
      Node newNode = _value.bindTo(binder, node, _value.get(obj));

      if (newNode != node) {
        binder.invalidate(node);
        node = newNode;
      }
    }
    else {
      QName nodeName = JAXBUtil.qnameFromNode(node);

      if (tagName.equals(nodeName)) {
        Node child = node.getFirstChild();

        child = JAXBUtil.skipIgnorableNodes(child);

        for (Accessor a : _elementAccessors.values()) {
          if (child != null) {
            // try to reuse as many of the child nodes as possible
            Node newNode = a.bindTo(binder, child, a.get(obj));

            if (newNode != child) {
              node.replaceChild(newNode, child);
              binder.invalidate(child);
              child = newNode;
            }

            child = child.getNextSibling();
            child = JAXBUtil.skipIgnorableNodes(child);
          }
          else {
            Node newNode = JAXBUtil.elementFromQName(a.getQName(), node);
            node.appendChild(a.bindTo(binder, newNode, a.get(obj)));
          }
        }
      }
      else {
        binder.invalidate(node);

        node = JAXBUtil.elementFromQName(tagName, node);

        for (Accessor a : _elementAccessors.values()) {
          Node child = JAXBUtil.elementFromQName(a.getQName(), node);
          node.appendChild(a.bindTo(binder, child, a.get(obj)));
        }
      }
    }

    binder.bind(obj, node);

    return node;
  }

  public QName getElementName()
  {
    if (_elementName != null)
      return _elementName;
    else
      return _typeName;
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
