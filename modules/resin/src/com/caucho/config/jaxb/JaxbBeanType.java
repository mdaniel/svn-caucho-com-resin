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
 * @author Scott Ferguson
 */

package com.caucho.config.jaxb;

import java.beans.Introspector;

import java.lang.reflect.*;

import java.lang.ref.SoftReference;

import java.util.ArrayList;
import java.util.HashMap;

import javax.el.*;

import org.w3c.dom.Node;

import com.caucho.util.*;

import com.caucho.config.*;

import com.caucho.xml.*;

public class JaxbBeanType extends TypeStrategy {
  protected static final L10N L = new L10N(BeanTypeStrategy.class);

  private static final HashMap<Class,PrimType> _primTypes
    = new HashMap<Class,PrimType>();

  private HashMap<String,AttributeStrategy> _attributeMap
    = new HashMap<String,AttributeStrategy>();
  
  private final Class _type;

  public JaxbBeanType(Class type)
  {
    _type = type;

    introspect();
  }

  /**
   * Returns the type class.
   */
  public Class getType()
  {
    return _type;
  }

  /**
   * Returns the type name.
   */
  public String getTypeName()
  {
    return getType().getName();
  }

  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  public Object configure(NodeBuilder builder, Node node, Object parent)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the appropriate strategy for the bean.
   *
   * @param attrName
   * @return the strategy
   * @throws ConfigException
   */
  public AttributeStrategy getAttributeStrategy(QName attrName)
    throws Exception
  {
    AttributeStrategy strategy = _attributeMap.get(attrName.getLocalName());

    if (strategy != null)
      return strategy;

    return strategy;
  }

  private void introspect()
  {
    introspectMethods();
    introspectFields();
  }

  private void introspectMethods()
  {
    Method []methods = _type.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      String name = method.getName();

      if (! Modifier.isPublic(method.getModifiers()))
	continue;

      if (! name.startsWith("set"))
	continue;

      Class retType = method.getReturnType();
      if (! void.class.equals(retType))
	continue;

      Class []paramTypes = method.getParameterTypes();

      if (paramTypes.length != 1)
	continue;

      AttributeStrategy attr = createPropertyAttribute(method, paramTypes[0]);

      name = Introspector.decapitalize(name.substring(3));

      if (attr != null)
	_attributeMap.put(name, attr);
    }
  }

  private void introspectFields()
  {
    Field []fields = _type.getFields();

    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      String name = field.getName();

      if (! Modifier.isPublic(field.getModifiers()))
	continue;

      Class fieldType = field.getType();

      AttributeStrategy attr = createFieldAttribute(field, fieldType);

      if (attr != null)
	_attributeMap.put(name, attr);
    }
  }

  private AttributeStrategy createPropertyAttribute(Method method, Class type)
  {
    PrimType primType = _primTypes.get(type);

    if (primType != null) {
      switch (primType) {
      case BOOLEAN:
      case BOOLEAN_OBJECT:
	return new BooleanProperty(method);
	
      case BYTE:
      case BYTE_OBJECT:
	return new ByteProperty(method);
	
      case SHORT:
      case SHORT_OBJECT:
	return new ShortProperty(method);
	
      case INTEGER:
      case INTEGER_OBJECT:
	return new IntProperty(method);
	
      case LONG:
      case LONG_OBJECT:
	return new LongProperty(method);
	
      case FLOAT:
      case FLOAT_OBJECT:
	return new FloatProperty(method);
	
      case DOUBLE:
      case DOUBLE_OBJECT:
	return new DoubleProperty(method);
	
      case STRING:
	return new StringProperty(method);

      default:
	throw new UnsupportedOperationException(primType.toString());
      }
    }

    return new BeanProperty(method, type);
  }

  private AttributeStrategy createFieldAttribute(Field field, Class type)
  {
    PrimType primType = _primTypes.get(type);

    if (primType != null) {
      switch (primType) {
      case BOOLEAN:
      case BOOLEAN_OBJECT:
	return new BooleanField(field);
	
      case BYTE:
      case BYTE_OBJECT:
	return new ByteField(field);
	
      case SHORT:
      case SHORT_OBJECT:
	return new ShortField(field);
	
      case INTEGER:
      case INTEGER_OBJECT:
	return new IntField(field);
	
      case LONG:
      case LONG_OBJECT:
	return new LongField(field);
	
      case FLOAT:
      case FLOAT_OBJECT:
	return new FloatField(field);
	
      case DOUBLE:
      case DOUBLE_OBJECT:
	return new DoubleField(field);
	
      case STRING:
	return new StringField(field);

      default:
	throw new UnsupportedOperationException(primType.toString());
      }
    }

    return new BeanField(field, type);
  }

  enum PrimType {
    BOOLEAN,
    BOOLEAN_OBJECT,
    
    BYTE,
    BYTE_OBJECT,
    
    SHORT,
    SHORT_OBJECT,
    
    INTEGER,
    INTEGER_OBJECT,
    
    LONG,
    LONG_OBJECT,
    
    FLOAT,
    FLOAT_OBJECT,
    
    DOUBLE,
    DOUBLE_OBJECT,
    
    STRING,
  };

  static {
    _primTypes.put(boolean.class, PrimType.BOOLEAN);
    _primTypes.put(Boolean.class, PrimType.BOOLEAN_OBJECT);
    
    _primTypes.put(byte.class, PrimType.BYTE);
    _primTypes.put(Byte.class, PrimType.BYTE_OBJECT);
    
    _primTypes.put(short.class, PrimType.SHORT);
    _primTypes.put(Short.class, PrimType.SHORT_OBJECT);
    
    _primTypes.put(int.class, PrimType.INTEGER);
    _primTypes.put(Integer.class, PrimType.INTEGER_OBJECT);
    
    _primTypes.put(long.class, PrimType.LONG);
    _primTypes.put(Long.class, PrimType.LONG_OBJECT);
    
    _primTypes.put(float.class, PrimType.FLOAT);
    _primTypes.put(Float.class, PrimType.FLOAT_OBJECT);
    
    _primTypes.put(double.class, PrimType.DOUBLE);
    _primTypes.put(Double.class, PrimType.DOUBLE_OBJECT);
    
    _primTypes.put(String.class, PrimType.STRING);
  }
}
