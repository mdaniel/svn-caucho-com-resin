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

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;

import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.transform.*;

import org.w3c.dom.*;

import com.caucho.jaxb.skeleton.*;
import com.caucho.util.*;
import com.caucho.server.util.*;

/**
 * Entry point to API
 */
public class JAXBContextImpl extends JAXBContext {
  private static final L10N L = new L10N(JAXBContextImpl.class);

  private Class[]     _classes;
  private String[]    _packages;
  private ClassLoader _classLoader;
  private JAXBIntrospector _jaxbIntrospector;

  private ArrayList<ObjectFactorySkeleton> _objectFactories 
    = new ArrayList<ObjectFactorySkeleton>();

  private HashMap<String,Object> _properties 
    = new HashMap<String,Object>();

  private HashMap<Class,Skeleton> _skeletons 
    = new HashMap<Class,Skeleton>();

  private HashMap<QName,Skeleton> _roots 
    = new HashMap<QName,Skeleton>();

  public JAXBContextImpl(String contextPath,
                         ClassLoader classLoader,
                         Map<String,?> properties)
    throws JAXBException
  {
    this._jaxbIntrospector = new JAXBIntrospectorImpl(this);
    this._classes = new Class[0];
    this._classLoader = classLoader;

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

    String slashedPackageName = "/" + packageName.replace('.', '/');

    try {
      Class cl = CauchoSystem.loadClass(packageName + ".ObjectFactory");
      _objectFactories.add(new ObjectFactorySkeleton(cl));

      success = true;
    }
    catch (ClassNotFoundException e) {
      // we can still try for jaxb.index
    }

    try {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      InputStream is =
        classLoader.getResourceAsStream(slashedPackageName + "/jaxb.index");
      InputStreamReader isr = new InputStreamReader(is, "utf-8");
      LineNumberReader in = new LineNumberReader(isr);

      for (String line = in.readLine();
           line != null;
           line = in.readLine()) {
        String[] parts = line.split("#", 2);
        String className = parts[0].trim();

        if (! "".equals(className)) {
          Class cl = CauchoSystem.loadClass(packageName + "." + className);
          getSkeleton(cl);
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
    this._jaxbIntrospector = new JAXBIntrospectorImpl(this);
    this._packages = new String[0];
    this._classLoader = null;
    this._classes = new Class[classes.length];

    System.arraycopy(classes, 0, _classes, 0, classes.length);
    Arrays.sort(_classes, new ClassDepthComparator());

    for(Class c : _classes)
      getSkeleton(c);

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
        setProperty(e.getKey(), e.getValue());

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

    for(int i=0; i<_classes.length; i++) {
      Class c = _classes[i];
      sb.append(c.getName() + (i<_classes.length-1 ? ":" : ""));
    }

    for(int i=0; i<_packages.length; i++) {
      String p = _packages[i];
      sb.append(p + (i<_packages.length-1 ? ":" : ""));
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

      out.writeStartDocument();

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

    for (Skeleton skeleton : _skeletons.values())
      skeleton.generateSchema(out);

    out.writeEndElement(); // schema
  }

  public Skeleton getSkeleton(Class c)
    throws JAXBException
  {
    Skeleton skeleton = _skeletons.get(c);

    if (skeleton == null) {
      skeleton = new Skeleton(this, c);
      _skeletons.put(c, skeleton);
    }

    return skeleton;
  }

  public Property createProperty(Accessor a)
    throws JAXBException
  {
    Class type = a.getType();

    if (String.class.equals(type))
      return new StringProperty(a);

    if (Map.class.equals(type))
      return new MapProperty(a);

    if (Double.class.equals(type) || Double.TYPE.equals(type))
      return new DoubleProperty(a);

    if (Float.class.equals(type) || Float.TYPE.equals(type))
      return new FloatProperty(a);

    if (Integer.class.equals(type) || Integer.TYPE.equals(type))
      return new IntProperty(a);

    if (Long.class.equals(type) || Long.TYPE.equals(type))
      return new LongProperty(a);

    if (Boolean.class.equals(type) || Boolean.TYPE.equals(type))
      return new BooleanProperty(a);

    if (Character.class.equals(type) || Character.TYPE.equals(type))
      return new CharacterProperty(a);

    if (Short.class.equals(type) || Short.TYPE.equals(type))
      return new ShortProperty(a);

    if (Byte.class.equals(type) || Byte.TYPE.equals(type))
      return new ByteProperty(a);

    if (BigDecimal.class.equals(type))
      return new BigDecimalProperty(a);

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

  public void addRootElement(QName q, Skeleton s) {
    _roots.put(q, s);
  }

  public Skeleton getRootElement(QName q)
  {
    return _roots.get(q);
  }

  /**
   * Sorts classes in reverse depth order.  When searching for the correct
   * class for an instance, we need the most specific known type.
   */
  private static class ClassDepthComparator implements Comparator<Class> {
    public int compare(Class o1, Class o2) 
    {
      int d1 = classDepth(o1);
      int d2 = classDepth(o2);

      if (d1 < d2)
        return 1;
      else if (d1 > d2)
        return -1;
      else
        return 0;
    }

    public boolean equals(Object obj)
    {
      return obj instanceof ClassDepthComparator;
    }

    // XXX Memoize?
    private static int classDepth(Class cl)
    {
      Class superClass = cl.getSuperclass();

      if (superClass == null)
        return 0;

      return 1 + classDepth(superClass);
    }
  }
}

