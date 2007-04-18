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
import com.caucho.jaxb.annotation.XmlLocation;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.annotation.*;

import javax.xml.namespace.QName;

import javax.xml.stream.events.*;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

import java.io.IOException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
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

  private Accessor _locationAccessor;

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
  {
    super(context);
    _class = c;
  }

  public void init() 
    throws JAXBException
  {
    try {
      _package = _class.getPackage();

      // check for special before and after methods
      try {
        _beforeUnmarshal =
          _class.getMethod("beforeUnmarshal", Unmarshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _afterUnmarshal =
          _class.getMethod("afterUnmarshal", Unmarshaller.class, Object.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _beforeMarshal = _class.getMethod("beforeMarshal", Marshaller.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      try {
        _afterMarshal = _class.getMethod("afterMarshal", Marshaller.class);
      } catch (NoSuchMethodException _) {
        // deliberate
      }

      if (Set.class.isAssignableFrom(_class)) {
        // XXX:
      }
      
      // XXX: @XmlJavaTypeAdapter
      
      // Find the zero-parameter constructor
      try {
        _constructor = _class.getConstructor(NO_PARAMS);
        _constructor.setAccessible(true);
      }
      catch (Exception e1) {
        try {
          _constructor = _class.getDeclaredConstructor(NO_PARAMS);
          _constructor.setAccessible(true);
        }
        catch (Exception e2) {
          throw new JAXBException(L.l("{0}: Zero-arg constructor not found", 
                                      _class.getName()), e2);
        }
      }

      _typeName = JAXBUtil.getXmlSchemaDatatype(_class);

      // Special case: when name="", this is an "anonymous" type, bound
      // exclusively to a particular element name
      if (! "".equals(_typeName.getLocalPart()))
        _context.addXmlType(_typeName, this);

      // Check for the complete name of the element...
      String namespace = null;

      // look at package defaults first...
      XmlSchema schema = (XmlSchema) _package.getAnnotation(XmlSchema.class);

      if (schema != null && ! "".equals(schema.namespace()))
        namespace = schema.namespace();
      
      // then look at class specific overrides.
      XmlRootElement xre = _class.getAnnotation(XmlRootElement.class);

      if (xre != null) { 
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

        _context.addRootElement(this);
      }

      // order the elements, if specified
      
      XmlAccessOrder accessOrder = XmlAccessOrder.UNDEFINED;

      XmlAccessorOrder packageOrder = 
        _package.getAnnotation(XmlAccessorOrder.class);

      XmlAccessorOrder classOrder = 
        _class.getAnnotation(XmlAccessorOrder.class);

      if (packageOrder != null)
        accessOrder = packageOrder.value();

      if (classOrder != null)
        accessOrder = classOrder.value();

      // try property orders too
     
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
      if (orderMap != null) {
        for (int i = 0; i < orderMap.size(); i++)
          _elementAccessors.add(null);
      }

      XmlAccessorType accessorType = 
        _class.getAnnotation(XmlAccessorType.class);

      XmlAccessType accessType = (accessorType == null ? 
                                  XmlAccessType.PUBLIC_MEMBER :
                                  accessorType.value());

      if (accessType != XmlAccessType.FIELD) {
        // getter/setter
        TreeSet<Method> methodSet = new TreeSet<Method>(methodComparator);

        Method[] declared = _class.getDeclaredMethods();

        for (Method m : declared)
          methodSet.add(m);

        Method[] methods = new Method[methodSet.size()];
        methodSet.toArray(methods);

        AccessibleObject.setAccessible(methods, true);

        while (methodSet.size() > 0) {
          Method m = methodSet.first();
          methodSet.remove(m);

          String name = null;
          Method get = null;
          Method set = null;

          if (m.getName().startsWith("get")) {
            get = m;

            if (Void.TYPE.equals(get.getReturnType()))
              continue;

            name = get.getName().substring(3); // 3 == "get".length());

            Class cl = get.getDeclaringClass();

            try {
              set = cl.getDeclaredMethod("set" + name, get.getReturnType());
            }
            catch (NoSuchMethodException e) {
              continue;
            }

            if (! methodSet.remove(set))
              continue;
          } 
          else if (m.getName().startsWith("set")) {
            set = m;

            Class[] parameterTypes = set.getParameterTypes();

            if (parameterTypes.length != 1)
              continue;

            name = set.getName().substring(3); // 3 == "set".length());

            Class cl = set.getDeclaringClass();

            try {
              get = cl.getDeclaredMethod("get" + name);
            }
            catch (NoSuchMethodException e) {
              continue;
            }

            if (! parameterTypes[0].equals(get.getReturnType()))
              continue;

            if (! methodSet.remove(get))
              continue;
          }
          else
            continue;

          name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

          // JAXB specifies that a "class" property must be specified as "clazz"
          // because of Object.getClass()
          if ("class".equals(name))
            continue;

          // XXX special cases for Throwable specified in JAX-WS
          // Should it be in the general JAXB?
          if (Throwable.class.isAssignableFrom(_class) && 
              ("stackTrace".equals(name) ||
               "cause".equals(name) ||
               "localizedMessage".equals(name)))
            continue;

          // XXX PUBLIC_MEMBER

          if (accessType == XmlAccessType.NONE &&
              ! JAXBUtil.isJAXBAnnotated(get) &&
              ! JAXBUtil.isJAXBAnnotated(set))
            continue;

          // jaxb/0456
          if (Modifier.isStatic(get.getModifiers()) &&
              JAXBUtil.isJAXBAnnotated(get))
            throw new JAXBException(L.l("JAXB annotations cannot be applied to static methods: {0}", get));

          // jaxb/0457
          if (Modifier.isStatic(set.getModifiers()) &&
              JAXBUtil.isJAXBAnnotated(set))
            throw new JAXBException(L.l("JAXB annotations cannot be applied to static methods: {0}", set));

          // jaxb/0374
          if (Modifier.isStatic(set.getModifiers()) ||
              Modifier.isStatic(get.getModifiers()))
            continue;

          if (get != null && get.isAnnotationPresent(XmlTransient.class))
            continue;
          if (set != null && set.isAnnotationPresent(XmlTransient.class))
            continue;

          get.setAccessible(true);
          set.setAccessible(true);

          Accessor a = new GetterSetterAccessor(_context, name, get, set); 

          if (orderMap != null) {
            Integer i = orderMap.remove(name);

            if (i != null)
              a.setOrder(i.intValue());
            // XXX else throw something?
          }

          processAccessor(a);
        }
      }

      if (accessType != XmlAccessType.PROPERTY) {
        // XXX Don't overwrite property accessors
        HashSet<Field> fieldSet = new HashSet<Field>();

        Field[] declared = _class.getDeclaredFields();

        for (Field f : declared)
          fieldSet.add(f);

        Field[] fields = new Field[fieldSet.size()];
        fieldSet.toArray(fields);

        AccessibleObject.setAccessible(fields, true);

        for (Field f : fields) {
          if (f.isAnnotationPresent(XmlLocation.class)) 
          {
            if (! f.getType().equals(Location.class))
              throw new JAXBException(L.l("Fields annotated by @Location must have type javax.xml.stream.Location"));

            _locationAccessor = new FieldAccessor(_context, f);
          }

          // special case: jaxb/0250
          // fields which are static are skipped _unless_ they are also
          // both final and attributes
          if (Modifier.isStatic(f.getModifiers()) &&
              ! (Modifier.isFinal(f.getModifiers()) &&
                 f.isAnnotationPresent(XmlAttribute.class)))
            continue;

          if (f.isAnnotationPresent(XmlTransient.class))
            continue;
          // jaxb/0176: transient modifier ignored

          if (accessType == XmlAccessType.PUBLIC_MEMBER && 
              ! Modifier.isPublic(f.getModifiers()) &&
              ! JAXBUtil.isJAXBAnnotated(f))
            continue;

          if (accessType == XmlAccessType.NONE && ! JAXBUtil.isJAXBAnnotated(f))
            continue;

          Accessor a = new FieldAccessor(_context, f);

          if (orderMap != null) {
            Integer i = orderMap.remove(f.getName());

            if (i != null)
              a.setOrder(i.intValue());
            // XXX else throw something?
          }

          processAccessor(a);
        }
      }

      if (log.isLoggable(Level.FINEST))
        log.finest("JAXB: " + _class.getName() + " has children: ");

      // do ordering if necessary
      if (orderMap != null || accessOrder != XmlAccessOrder.ALPHABETICAL) {
        for (int i = 0; i < _elementAccessors.size(); i++) {
          Accessor a = _elementAccessors.get(i);
          a.putQNames(_elementQNameToAccessorMap);

          if (log.isLoggable(Level.FINEST))
            log.finest("\t" + a);
        }
      }
      else {
        Collections.sort(_elementAccessors, Accessor.nameComparator);

        for (int i = 0; i < _elementAccessors.size(); i++) { 
          Accessor a = _elementAccessors.get(i);
          a.putQNames(_elementQNameToAccessorMap);

          if (log.isLoggable(Level.FINEST))
            log.finest("\t" + a);
        }
      }
    }
    catch (Exception e) {
      throw new JAXBException(L.l("{0}: Initialization error", 
                                  _class.getName()), e);
    }

    if (! Object.class.equals(_class.getSuperclass()))
      _parent = _context.getSkeleton(_class.getSuperclass());
  }

  /**
   * Handles any processing that needs to happen after all ClassSkeletons
   * have been created and all classes have been discovered.
   **/
  public void postProcess()
    throws JAXBException
  {
    for (int i = 0; i < _elementAccessors.size(); i++) { 
      Accessor a = _elementAccessors.get(i);

      if (a.getAccessorType() == Accessor.AccessorType.ELEMENT_REF)
        a.putQNames(_elementQNameToAccessorMap);
    }
  }

  private void processAccessor(Accessor a)
    throws JAXBException
  {
    switch (a.getAccessorType()) {
      case VALUE: 
        if (_value != null)
          throw new JAXBException(L.l("Cannot have two @XmlValue annotated fields or properties"));

        if (_elementAccessors.size() > 0) {
          // in case of propOrder & XmlValue
          if (_elementAccessors.size() != 1 || _elementAccessors.get(0) != null)
            throw new JAXBException(L.l("Cannot have both @XmlValue and elements in a JAXB element (e.g. {0})", _elementAccessors.get(0)));

          _elementAccessors.clear();
        }

        _value = a;
        break;

      case ATTRIBUTE:
        a.putQNames(_attributeQNameToAccessorMap);
        _attributeAccessors.add(a);
        break;

      case ANY_ATTRIBUTE:
        if (_anyTypeAttributeAccessor != null)
          throw new JAXBException(L.l("Cannot have two fields or properties with @XmlAnyAttribute annotation"));

        _anyTypeAttributeAccessor = a;
        _attributeAccessors.add(a);
        break;

      case ELEMENT:
      case ELEMENT_REF:
      case ELEMENTS:
        if (_value != null)
          throw new JAXBException(L.l("{0}: Cannot have both @XmlValue and elements in a JAXB element", _class.getName()));

        if (a.getOrder() >= 0)
          _elementAccessors.set(a.getOrder(), a);
        else
          _elementAccessors.add(a);
        break;
        
      case ANY_TYPE_ELEMENT:
      case ANY_TYPE_ELEMENT_LAX:
        if (_anyTypeElementAccessor != null)
          throw new JAXBException(L.l("{0}: Cannot have two @XmlAnyElement annotations in a single class", _class.getName()));

        _anyTypeElementAccessor = a;
        break;
    }
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
    catch (JAXBException e) {
      throw e;
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

      if (_locationAccessor != null)
        _locationAccessor.set(ret, in.getLocation());

      if (_beforeUnmarshal != null)
        _beforeUnmarshal.invoke(ret, u, null);

      if (u.getListener() != null)
        u.getListener().beforeUnmarshal(ret, null);

      if (_value != null) {
        Object val = _value.read(u, in, ret, this);
        _value.set(ret, val);
      }
      else {
        // process the attributes
        for (int i = 0; i < in.getAttributeCount(); i++) {
          QName attributeName = in.getAttributeName(i);
          Accessor a = getAttributeAccessor(attributeName);

          if (a == null)
            throw new UnmarshalException(L.l("Attribute {0} not found in {1}", 
                                             attributeName, getType()));

          a.set(ret, a.readAttribute(in, i, ret));
        }

        int i = 0;
        in.nextTag();

        while (in.getEventType() == in.START_ELEMENT) {
          Accessor a = getElementAccessor(in.getName());

          if (a == null) {
            throw new UnmarshalException(L.l("Child <{0}> not found in {1}", 
                                             in.getName(), getType()));
          }

          if (! a.checkOrder(i++, u.getEventHandler())) {
            throw new UnmarshalException(L.l("Child <{0}> misordered in {1}", 
                                             in.getName(), getType()));
          }

          Object val = a.read(u, in, ret);
          a.set(ret, val);
        }

        // essentially a nextTag() that handles end of document gracefully
        while (in.hasNext()) {
          in.next();

          if (in.getEventType() == in.START_ELEMENT ||
              in.getEventType() == in.END_ELEMENT)
            break;
        }
      }

      if (_afterUnmarshal != null)
        _afterUnmarshal.invoke(ret, u, null);

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
        Object val = _value.read(u, in, ret);
        _value.set(ret, val);
      }
      else {
        XMLEvent event = in.peek();
        StartElement start = event.asStartElement();
        Iterator iterator = start.getAttributes();

        // process the attributes
        while (iterator.hasNext()) {
          Attribute attribute = (Attribute) iterator.next();
          Accessor a = getAttributeAccessor(attribute.getName());

          if (a == null)
            throw new UnmarshalException(L.l("Attribute {0} not found in {1}", 
                                             attribute.getName(), getType()));

          a.set(ret, a.readAttribute(attribute, ret));
        }

        int i = 0;
        event = in.nextEvent();
        event = in.nextEvent();

        while (event.isStartElement()) {
          QName name = ((StartElement) event).getName();
          Accessor a = getElementAccessor(name);

          if (a == null)
            throw new UnmarshalException(L.l("Child <{0}> not found", name));

          if (! a.checkOrder(i++, u.getEventHandler()))
            throw new UnmarshalException(L.l("Child <{0}> misordered", name));

          Object val = a.read(u, in, ret);
          a.set(ret, val);

          event = in.peek();
        }

        while (in.hasNext()) {
          event = in.nextEvent();

          if (event.isStartElement() ||
              event.isEndElement())
            break;
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
    throws IOException, JAXBException
  {
    Node root = node.getNode();
    C ret = (C) existing;
    
    if (ret == null)
      ret = newInstance();

    if (_value != null) { 
      Object val = _value.bindFrom(binder, node, ret);
      _value.set(ret, val);
    }
    else {
      int i = 0;
      Node child = node.firstChild();

      while (child != null) {
        if (child.getNodeType() == Node.ELEMENT_NODE) {
          QName name = JAXBUtil.qnameFromNode(child);

          Accessor a = getElementAccessor(name);

          if (a == null)
            throw new UnmarshalException(L.l("Child <{0}> not found", name));

          if (! a.checkOrder(i++, binder.getEventHandler()))
            throw new UnmarshalException(L.l("Child <{0}> misordered", name));

          Object val = a.bindFrom(binder, node, ret);
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
                    Object obj, Namer namer, Iterator attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj == null)
      return;

    try {
      if (_beforeMarshal != null)
        _beforeMarshal.invoke(obj, m);

      if (m.getListener() != null)
        m.getListener().beforeMarshal(obj);

      QName tagName = null;
      
      if (namer != null)
        tagName = namer.getQName(obj);

      if (tagName == null)
        tagName = _elementName;

      if (_value != null) {
        _value.setQName(tagName);
        _value.write(m, out, obj, _attributeAccessors.iterator());
      }
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

        if (attributes != null) {
          while (attributes.hasNext()) {
            Accessor a = (Accessor) attributes.next();
            a.write(m, out, obj);
          }
        }

        for (Accessor a : _attributeAccessors)
          a.write(m, out, obj);

        for (Accessor a : _elementAccessors)
          a.write(m, out, obj);

        if (_anyTypeElementAccessor != null) // XXX ordering!
          _anyTypeElementAccessor.write(m, out, obj);
        
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
                    Object obj, Namer namer, Iterator attributes)
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

      QName tagName = null;
      
      if (namer != null)
        tagName = namer.getQName(obj);

      if (tagName == null)
        tagName = _elementName;

      if (_value != null) {
        _value.setQName(tagName);
        _value.write(m, out, _value.get(obj), _attributeAccessors.iterator());
      }
      else {
        out.add(JAXBUtil.EVENT_FACTORY.createStartElement(tagName, null, null));

        if (attributes != null) {
          // XXX
        }

        for (Accessor a : _elementAccessors)
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

  public Node bindTo(BinderImpl binder, Node node, 
                     Object obj, Namer namer, Iterator attributes)
    throws IOException, JAXBException
  {
    if (obj == null)
      return null;

    QName tagName = null;

    if (namer != null)
      tagName = namer.getQName(obj);

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

        if (attributes != null) {
          // XXX
        }

        for (Accessor a : _elementAccessors) {
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
            Node newNode = JAXBUtil.elementFromQName(a.getQName(obj), node);
            node.appendChild(a.bindTo(binder, newNode, a.get(obj)));
          }
        }
      }
      else {
        binder.invalidate(node);

        node = JAXBUtil.elementFromQName(tagName, node);

        for (Accessor a : _elementAccessors) {
          Node child = JAXBUtil.elementFromQName(a.getQName(obj), node);
          node.appendChild(a.bindTo(binder, child, a.get(obj)));
        }
      }
    }

    binder.bind(obj, node);

    return node;
  }

  public boolean isRootElement()
  {
    return _elementName != null;
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

      if (Collection.class.isAssignableFrom(_value.getType())) {
        out.writeEmptyElement(XML_SCHEMA_PREFIX, "list", XML_SCHEMA_NS);
        out.writeAttribute("itemType", _value.getSchemaType());
      }
      else {
        out.writeEmptyElement(XML_SCHEMA_PREFIX, "restriction", XML_SCHEMA_NS);
        out.writeAttribute("base", _value.getSchemaType());
      }

      for (Accessor accessor : _attributeAccessors)
        accessor.generateSchema(out);

      out.writeEndElement(); // simpleType
    }
    else {
      out.writeStartElement(XML_SCHEMA_PREFIX, "complexType", XML_SCHEMA_NS);

      if (Modifier.isAbstract(_class.getModifiers()))
        out.writeAttribute("abstract", "true");

      if (! "".equals(_typeName.getLocalPart()))
        out.writeAttribute("name", _typeName.getLocalPart());

      out.writeStartElement(XML_SCHEMA_PREFIX, "sequence", XML_SCHEMA_NS);

      for (Accessor accessor : _elementAccessors)
        accessor.generateSchema(out);

      if (_anyTypeElementAccessor != null)
        _anyTypeElementAccessor.generateSchema(out);

      out.writeEndElement(); // sequence

      for (Accessor accessor : _attributeAccessors)
        accessor.generateSchema(out);

      out.writeEndElement(); // complexType
    }
  }

  //XXX The TreeSet needs this for some reason
  private static final Comparator methodComparator
    = new java.util.Comparator<Method>() {
      public int compare(Method m1, Method m2)
      {
        return m1.toGenericString().compareTo(m2.toGenericString());
      }

      public boolean equals(Object obj)
      {
        return obj == this;
      }
    };
}
