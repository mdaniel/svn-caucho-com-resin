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

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.lang.ref.SoftReference;

import java.util.*;

import javax.el.*;
import javax.xml.bind.annotation.*;

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
    Object bean = _type.newInstance();
    
    configureBean(builder, bean, node);

    return bean;
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
    XmlAccessorType accessorType
      = (XmlAccessorType) _type.getAnnotation(XmlAccessorType.class);

    XmlAccessType accessType;

    if (accessorType != null)
      accessType = accessorType.value();
    else
      accessType = XmlAccessType.PUBLIC_MEMBER;
    
    introspectMethods(accessType);
    introspectFields(accessType);
  }

  private void introspectMethods(XmlAccessType accessType)
  {
    Method []methods = _type.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method setter = methods[i];
      String name = setter.getName();

      if (! name.startsWith("set"))
	continue;

      Class retType = setter.getReturnType();
      if (! void.class.equals(retType))
	continue;

      Class []paramTypes = setter.getParameterTypes();

      if (paramTypes.length != 1)
	continue;

      String getName = "get" + name.substring(3);
      String isName = "";

      if (boolean.class.equals(paramTypes[0])
	  || Boolean.class.equals(paramTypes[0])) {
	isName = "is" + name.substring(3);
      }

      Method getter = findGetter(_type, getName, isName, paramTypes[0]);

      if (getter == null)
	continue;

      if (hasAnnotation(XmlTransient.class, getter, setter))
	continue;

      XmlElement xmlElement
	= getAnnotation(XmlElement.class, getter, setter);
      
      XmlAttribute xmlAttribute
	= getAnnotation(XmlAttribute.class, getter, setter);

      name = Introspector.decapitalize(name.substring(3));

      if (xmlElement != null) {
	if (! "##default".equals(xmlElement.name()))
	  name = xmlElement.name();
      }
      else if (xmlAttribute != null) {
	if (! "##default".equals(xmlAttribute.name()))
	  name = xmlAttribute.name();
      }
      else if (accessType == XmlAccessType.PROPERTY) {
      }
      else if (accessType == XmlAccessType.PUBLIC_MEMBER
	       && Modifier.isPublic(setter.getModifiers())) {
      }
      else
	continue;

      AttributeStrategy attr = createPropertyAttribute(getter,
						       setter,
						       paramTypes[0]);

      if (attr != null)
	_attributeMap.put(name, attr);
    }
  }

  private Method findGetter(Class type,
			    String getName,
			    String isName,
			    Class retType)
  {
    if (type == null || Object.class.equals(type))
      return null;
    
    Method []methods = type.getDeclaredMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      
      if (method.getParameterTypes().length != 0)
	continue;
      else if (! retType.equals(method.getReturnType()))
	continue;

      else if (getName.equals(method.getName()))
	return method;
      else if (isName.equals(method.getName()))
	return method;
    }

    return null;
  }

  private void introspectFields(XmlAccessType accessType)
  {
    Field []fields = _type.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      String name = field.getName();

      if (field.isAnnotationPresent(XmlTransient.class))
	continue;
      if (Modifier.isTransient(field.getModifiers()))
	continue;

      XmlElement xmlElement = field.getAnnotation(XmlElement.class);
      XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

      if (xmlElement != null) {
	if (! "##default".equals(xmlElement.name()))
	  name = xmlElement.name();
      }
      else if (xmlAttribute != null) {
	if (! "##default".equals(xmlAttribute.name()))
	  name = xmlAttribute.name();
      }
      else if (accessType == XmlAccessType.FIELD) {
      }
      else if (accessType == XmlAccessType.PUBLIC_MEMBER
	       && Modifier.isPublic(field.getModifiers())) {
      }
      else
	continue;

      Class fieldType = field.getType();

      AttributeStrategy attr = createFieldAttribute(field, fieldType);

      if (attr != null)
	_attributeMap.put(name, attr);
    }
  }

  private AttributeStrategy createPropertyAttribute(Method getter,
						    Method setter,
						    Class type)
  {
    setter.setAccessible(true);
    
    PrimType primType = _primTypes.get(type);

    if (primType != null) {
      switch (primType) {
      case BOOLEAN:
      case BOOLEAN_OBJECT:
	return new BooleanProperty(setter);
	
      case BYTE:
      case BYTE_OBJECT:
	return new ByteProperty(setter);
	
      case SHORT:
      case SHORT_OBJECT:
	return new ShortProperty(setter);
	
      case INTEGER:
      case INTEGER_OBJECT:
	return new IntProperty(setter);
	
      case LONG:
      case LONG_OBJECT:
	return new LongProperty(setter);
	
      case FLOAT:
      case FLOAT_OBJECT:
	return new FloatProperty(setter);
	
      case DOUBLE:
      case DOUBLE_OBJECT:
	return new DoubleProperty(setter);
	
      case STRING:
	return new StringProperty(setter);

      default:
	throw new UnsupportedOperationException(primType.toString());
      }
    }

    if (Collection.class.isAssignableFrom(type)) {
      Type retType = getter.getGenericReturnType();
      Class componentType = Object.class;

      if (retType instanceof ParameterizedType) {
	ParameterizedType pType = (ParameterizedType) retType;
	Type []typeArgs = pType.getActualTypeArguments();

	if (typeArgs.length > 0) {
	  if (typeArgs[0] instanceof Class)
	    componentType = (Class) typeArgs[0];
	  else if (typeArgs[0] instanceof ParameterizedType)
	    componentType = (Class) ((ParameterizedType) typeArgs[0]).getRawType();
	}
      }
      
      TypeStrategy typeMarshal = createTypeMarshal(componentType);

      return new CollectionProperty(typeMarshal, getter);
    }

    return new BeanProperty(setter, type);
  }

  private TypeStrategy createTypeMarshal(Class type)
  {
    PrimType primType = _primTypes.get(type);

    if (primType != null) {
      switch (primType) {
      case BOOLEAN:
      case BOOLEAN_OBJECT:
	return BooleanType.TYPE;
	
      case BYTE:
      case BYTE_OBJECT:
	return ByteType.TYPE;
	
      case SHORT:
      case SHORT_OBJECT:
	return ShortType.TYPE;
	
      case INTEGER:
      case INTEGER_OBJECT:
	return IntegerType.TYPE;
	
      case LONG:
      case LONG_OBJECT:
	return LongType.TYPE;
	
      case FLOAT:
      case FLOAT_OBJECT:
	return FloatType.TYPE;
	
      case DOUBLE:
      case DOUBLE_OBJECT:
	return DoubleType.TYPE;
	
      case STRING:
	return StringType.TYPE;
      }
    }

    try {
      return TypeStrategyFactory.getTypeStrategy(type);
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  private static <X extends Annotation> X getAnnotation(Class<X> annType,
							Method getter,
							Method setter)
  {
    X ann = setter.getAnnotation(annType);

    if (ann != null)
      return ann;

    return getter.getAnnotation(annType);
  }

  private static boolean hasAnnotation(Class<? extends Annotation> annType,
				       Method getter,
				       Method setter)
  {
    return (getter.isAnnotationPresent(annType)
	    || setter.isAnnotationPresent(annType));
  }

  private AttributeStrategy createFieldAttribute(Field field, Class type)
  {
    field.setAccessible(true);
    
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
