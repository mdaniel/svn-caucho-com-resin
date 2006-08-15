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
import java.math.*;
import javax.xml.bind.*;
import javax.xml.namespace.*;
import org.w3c.dom.*;
import java.util.*;
import java.io.*;

/**
 * Entry point to API
 */
public class JAXBContextImpl extends JAXBContext {

  private Class[]     _classes;
  private String[]    _packages;
  private ClassLoader _classLoader;
  private JAXBIntrospector _jaxbIntrospector;

  private HashMap<String,Object> _properties =
    new HashMap<String,Object>();

  private HashMap<Class,Skeleton> _skeletons =
    new HashMap<Class,Skeleton>();

  private HashMap<QName,Skeleton> _roots =
    new HashMap<QName,Skeleton>();

  // 
  // XXX: JAXB Providers are required to call the setDatatypeConverter
  // api at some point before the first marshal or unmarshal operation
  // (perhaps during the call to JAXBContext.newInstance)
  //

  public JAXBContextImpl(String contextPath,
			 ClassLoader classLoader,
			 Map<String,?> properties)
  {
    this._jaxbIntrospector = new JAXBIntrospectorImpl(this);
    this._classes = new Class[0];
    this._classLoader = classLoader;

    StringTokenizer st = new StringTokenizer(contextPath, ":");
    this._packages = new String[st.countTokens()];

    for(int i=0; i<_packages.length; i++)
      _packages[i] = st.nextToken();

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
	setProperty(e.getKey(), e.getValue());
  }

  public JAXBContextImpl(Class[] classes,
			 Map<String,?> properties)
    throws JAXBException
  {
    this._jaxbIntrospector = new JAXBIntrospectorImpl(this);
    this._classes = classes;
    this._packages = new String[0];
    this._classLoader = null;

    for(Class c : _classes)
      getSkeleton(c);

    if (properties != null)
      for(Map.Entry<String,?> e : properties.entrySet())
	setProperty(e.getKey(), e.getValue());
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
    throw new UnsupportedOperationException("Schema validation not supported");
  }

  Skeleton getSkeleton(Class c)
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

  public Skeleton getRootElement(QName q) {
    return _roots.get(q);
  }

}

