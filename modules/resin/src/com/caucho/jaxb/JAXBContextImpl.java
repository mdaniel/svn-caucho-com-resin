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
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
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

  private String[] _packages;
  private ClassLoader _classLoader;
  private JAXBIntrospector _jaxbIntrospector;

  private ArrayList<ObjectFactorySkeleton> _objectFactories 
    = new ArrayList<ObjectFactorySkeleton>();

  private HashMap<String,Object> _properties 
    = new HashMap<String,Object>();

  private LinkedHashMap<Class,ClassSkeleton> _classSkeletons 
    = new LinkedHashMap<Class,ClassSkeleton>();

  private LinkedHashSet<WrapperSkeleton> _wrappers 
    = new LinkedHashSet<WrapperSkeleton>();

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

  public JAXBContextImpl(Class[] classes, Map<String,?> properties)
    throws JAXBException
  {
    _jaxbIntrospector = new JAXBIntrospectorImpl(this);
    _packages = new String[0];
    _classLoader = null;

    for(Class c : classes) {
      if (! c.isPrimitive() && 
          ! c.equals(String.class) &&
          ! c.equals(Class.class) &&
          ! c.equals(Object.class))
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
      XMLOutputFactory factory = XMLOutputFactory.newInstance();
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

    for (Skeleton skeleton : _wrappers)
      skeleton.generateSchema(out);

    out.writeEndElement(); // schema
  }

  public void createSkeleton(Class c)
    throws JAXBException
  {
    if (_classSkeletons.containsKey(c))
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

  public Property createProperty(Accessor a)
    throws JAXBException
  {
    Class type = a.getType();

    if (String.class.equals(type))
      return new StringProperty(a);

    if (Map.class.equals(type))
      return new MapProperty(a);

    if (double.class.equals(type) || Double.class.equals(type))
      return new DoubleProperty(a, type.isPrimitive());

    if (float.class.equals(type) || Float.class.equals(type))
      return new FloatProperty(a, type.isPrimitive());

    if (int.class.equals(type) || Integer.class.equals(type))
      return new IntProperty(a, type.isPrimitive());

    if (Long.class.equals(type) || Long.TYPE.equals(type))
      return new LongProperty(a, type.isPrimitive());

    if (boolean.class.equals(type) || Boolean.class.equals(type))
      return new BooleanProperty(a, type.isPrimitive());

    if (Character.class.equals(type) || Character.TYPE.equals(type))
      return new CharacterProperty(a, type.isPrimitive());

    if (Short.class.equals(type) || Short.TYPE.equals(type))
      return new ShortProperty(a, type.isPrimitive());

    if (Byte.class.equals(type) || Byte.TYPE.equals(type))
      return new ByteProperty(a, type.isPrimitive());

    if (BigDecimal.class.equals(type))
      return new BigDecimalProperty(a);

    if (BigInteger.class.equals(type))
      return new BigIntegerProperty(a);

    if (List.class.equals(type))
      return new ListProperty(a);

    if (Date.class.equals(type))
      return new DateProperty(a);

    if (byte[].class.equals(type))
      return new ByteArrayProperty(a);

    if (Collection.class.isAssignableFrom(type))
      return new CollectionProperty(a);

    if (type.isArray())
      return new ArrayProperty(a);

    return new SkeletonProperty(getSkeleton(type), a);
  }

  public void addRootElement(Skeleton s) 
  {
    _roots.put(s.getTypeName(), s);
  }

  public Skeleton getRootElement(QName q)
  {
    return _roots.get(q);
  }

  /**
   *
   * A Resin-specific method that allows adding a skeleton to the context
   * which doesn't actually represent a java class.  This is specifically
   * for JAX-WS functionality which wraps arguments and return values for
   * communication.  Adding a wrapper as a skeleton also allows writing
   * its schema without generating a new class.
   *
   **/
  public void addWrapperType(QName elementName, 
                             QName typeName,
                             List<QName> names, 
                             List<Class> wrapped)
    throws JAXBException
  {
    WrapperSkeleton wrapper = 
      new WrapperSkeleton(this, elementName, typeName, names, wrapped);

    addRootElement(wrapper);
    _wrappers.add(wrapper);
  }
}

