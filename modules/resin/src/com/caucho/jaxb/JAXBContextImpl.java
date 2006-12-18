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

package com.caucho.jaxb;

import com.caucho.jaxb.skeleton.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Entry point to API
 */
public class JAXBContextImpl extends JAXBContext {
  private static final L10N L = new L10N(JAXBContextImpl.class);

  private static final HashSet<Class> _specialClasses = new HashSet<Class>();

  static {
    _specialClasses.add(Object.class);
    _specialClasses.add(Class.class);
    _specialClasses.add(String.class);
    _specialClasses.add(Double.class);
    _specialClasses.add(Float.class);
    _specialClasses.add(Integer.class);
    _specialClasses.add(Long.class);
    _specialClasses.add(Boolean.class);
    _specialClasses.add(Character.class);
    _specialClasses.add(Short.class);
    _specialClasses.add(Byte.class);
    _specialClasses.add(BigDecimal.class);
    _specialClasses.add(BigInteger.class);
    _specialClasses.add(QName.class);
    _specialClasses.add(Date.class);
    _specialClasses.add(Calendar.class);
  }

  private String[] _packages;
  private ClassLoader _classLoader;
  private JAXBIntrospector _jaxbIntrospector;

  private XMLInputFactory _staxInputFactory;
  private XMLOutputFactory _staxOutputFactory;

  private ArrayList<ObjectFactorySkeleton> _objectFactories 
    = new ArrayList<ObjectFactorySkeleton>();

  private HashMap<String,Object> _properties 
    = new HashMap<String,Object>();

  private LinkedHashMap<Class,ClassSkeleton> _classSkeletons 
    = new LinkedHashMap<Class,ClassSkeleton>();

  private HashMap<QName,Skeleton> _roots 
    = new HashMap<QName,Skeleton>();

  public JAXBContextImpl(String contextPath,
                         ClassLoader classLoader,
                         Map<String,?> properties)
    throws JAXBException
  {
    _jaxbIntrospector = new JAXBIntrospectorImpl(this);
    _classLoader = classLoader;

    StringTokenizer st = new StringTokenizer(contextPath, ":");

    do {
      String packageName = st.nextToken(); 
      loadPackage(packageName);
    } 
    while (st.hasMoreTokens());

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
        setProperty(e.getKey(), e.getValue());

    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
  }

  public static JAXBContext createContext(Class []classes, 
                                          Map<String,?> properties)
    throws JAXBException
  {
    return new JAXBContextImpl(classes, properties);
  }

  public JAXBContextImpl(Class[] classes, Map<String,?> properties)
    throws JAXBException
  {
    _jaxbIntrospector = new JAXBIntrospectorImpl(this);
    _packages = new String[0];
    _classLoader = null;

    for(Class c : classes) {
      // XXX pull out to JAX-WS?
      if (! c.isPrimitive() &&
          ! c.isArray() && 
          ! _specialClasses.contains(c))
        createSkeleton(c);
    }

    if (properties != null) {
      for(Map.Entry<String,?> e : properties.entrySet())
        setProperty(e.getKey(), e.getValue());
    }

    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
  }

  public Marshaller createMarshaller()
    throws JAXBException
  {
    return new MarshallerImpl(this);
  }

  public Unmarshaller createUnmarshaller()
    throws JAXBException
  {
    return new UnmarshallerImpl(this);
  }

  public Validator createValidator()
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  XMLStreamReader getXMLStreamReader(InputStream is)
    throws XMLStreamException
  {
    if (_staxInputFactory == null)
      _staxInputFactory = XMLInputFactory.newInstance();

    return _staxInputFactory.createXMLStreamReader(is);
  }

  XMLInputFactory getXMLInputFactory()
  {
    if (_staxInputFactory == null)
      _staxInputFactory = XMLInputFactory.newInstance();

    return _staxInputFactory;
  }

  XMLOutputFactory getXMLOutputFactory()
  {
    if (_staxOutputFactory == null)
      _staxOutputFactory = XMLOutputFactory.newInstance();

    return _staxOutputFactory;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("JAXBContext[");

    for (Class c : _classSkeletons.keySet())
      sb.append(c.getName() + ":");

    for (int i = 0; i < _packages.length; i++) {
      String p = _packages[i];
      sb.append(p + (i < _packages.length - 1 ? ":" : ""));
    }

    sb.append("]");
    return sb.toString();
  }

  private void setProperty(String key, Object val)
  {
    _properties.put(key, val);
  }

  public Binder<Node> createBinder()
  {
    throw new UnsupportedOperationException("XML Infoset not supported");
  }

  public <T> Binder<T> createBinder(Class<T> domType)
  {
    throw new UnsupportedOperationException("XML Infoset not supported");
  }
  
  public JAXBIntrospector createJAXBIntrospector()
  {
    return _jaxbIntrospector;
  }

  public void generateSchema(SchemaOutputResolver outputResolver)
    throws IOException
  {
    Result result = outputResolver.createOutput("", "schema1.xsd");

    try {
      XMLOutputFactory factory = getXMLOutputFactory();
      XMLStreamWriter out = factory.createXMLStreamWriter(result);

      out.writeStartDocument("UTF-8", "1.0");

      generateSchemaWithoutHeader(out);
    }
    catch (Exception e) {
      IOException ioException = new IOException();

      ioException.initCause(e);

      throw ioException;
    }
  }

  public void generateSchemaWithoutHeader(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    out.writeStartElement("xsd", "schema", "http://www.w3.org/2001/XMLSchema");
    out.writeAttribute("version", "1.0");

    for (Skeleton skeleton : _classSkeletons.values())
      skeleton.generateSchema(out);

    out.writeEndElement(); // schema
  }

  public void createSkeleton(Class c)
    throws JAXBException
  {
    if (_classSkeletons.containsKey(c))
      return;

    // XXX
    if (c.isEnum() || c.isInterface())
      return;

    // Breadcrumb to prevent problems with recursion
    _classSkeletons.put(c, null); 

    ClassSkeleton skeleton = new ClassSkeleton(this, c);
    _classSkeletons.put(c, skeleton);
  }

  public Skeleton getSkeleton(Class c)
    throws JAXBException
  {
    createSkeleton(c);

    return _classSkeletons.get(c);
  }

  public ClassSkeleton findSkeletonForObject(Object obj)
    throws JAXBException
  {
    Class cl = obj.getClass();

    while (! cl.equals(Object.class)) {
      ClassSkeleton skeleton = _classSkeletons.get(cl);

      if (skeleton != null)
        return skeleton;

      cl = cl.getSuperclass();
    }

    throw new JAXBException(L.l("Class {0} unknown to this JAXBContext", cl));
  }

  public Property createProperty(Class type)
    throws JAXBException
  {
    if (String.class.equals(type))
      return StringProperty.PROPERTY;

    if (Map.class.equals(type))
      return new MapProperty();

    if (Double.class.equals(type) || Double.TYPE.equals(type))
      return DoubleProperty.PROPERTY;

    if (Float.class.equals(type) || Float.TYPE.equals(type))
      return FloatProperty.PROPERTY;

    if (Integer.class.equals(type) || Integer.TYPE.equals(type))
      return IntProperty.PROPERTY;

    if (Long.class.equals(type) || Long.TYPE.equals(type))
      return LongProperty.PROPERTY;

    if (Boolean.class.equals(type) || Boolean.TYPE.equals(type))
      return BooleanProperty.PROPERTY;

    if (Character.class.equals(type) || Character.TYPE.equals(type))
      return CharacterProperty.PROPERTY;

    if (Short.class.equals(type) || Short.TYPE.equals(type))
      return ShortProperty.PROPERTY;

    if (Byte.class.equals(type) || Byte.TYPE.equals(type))
      return ByteProperty.PROPERTY;

    if (BigDecimal.class.equals(type))
      return BigDecimalProperty.PROPERTY;

    if (BigInteger.class.equals(type))
      return BigIntegerProperty.PROPERTY;

    if (QName.class.equals(type))
      return QNameProperty.PROPERTY;

    if (List.class.equals(type))
      return new ListProperty();

    if (Date.class.equals(type))
      return DateTimeProperty.PROPERTY;

    if (Calendar.class.equals(type))
      return CalendarProperty.PROPERTY;

    if (byte[].class.equals(type))
      return ByteArrayProperty.PROPERTY;

    if (Collection.class.isAssignableFrom(type))
      return new CollectionProperty();

    if (type.isArray()) {
      Property componentProperty = createProperty(type.getComponentType());
      return ArrayProperty.createArrayProperty(componentProperty,
                                               type.getComponentType());
    }

    if (type.isEnum())
      return new EnumProperty();

    return new SkeletonProperty(getSkeleton(type));
  }

  public void addRootElement(Skeleton s) 
  {
    _roots.put(s.getTypeName(), s);
  }

  public Skeleton getRootElement(QName q)
  {
    return _roots.get(q);
  }

  private void loadPackage(String packageName) 
    throws JAXBException
  {
    boolean success = false;

    try {
      Class cl = CauchoSystem.loadClass(packageName + ".ObjectFactory");
      _objectFactories.add(new ObjectFactorySkeleton(cl));

      success = true;
    }
    catch (ClassNotFoundException e) {
      // we can still try for jaxb.index
    }

    try {
      String resourceName = packageName.replace('.', '/') + "/jaxb.index";

      // For some reason, this approach works when running resin...
      InputStream is = this.getClass().getResourceAsStream('/' + resourceName);

      // ...and this approach works in QA
      if (is == null) {
        ClassLoader classLoader = 
          Thread.currentThread().getContextClassLoader();

        is = classLoader.getResourceAsStream(resourceName);
      }

      InputStreamReader isr = new InputStreamReader(is, "utf-8");
      LineNumberReader in = new LineNumberReader(isr);

      for (String line = in.readLine();
           line != null;
           line = in.readLine()) {
        String[] parts = line.split("#", 2);
        String className = parts[0].trim();

        if (! "".equals(className)) {
          Class cl = CauchoSystem.loadClass(packageName + "." + className);

          createSkeleton(cl);
        }
      }

      success = true;
    }
    catch (Throwable t) {
      if (! success) {
        throw new JAXBException(L.l("Unable to open jaxb.index for package {0}",
                                    packageName), t);
      }
    }
  }
}

