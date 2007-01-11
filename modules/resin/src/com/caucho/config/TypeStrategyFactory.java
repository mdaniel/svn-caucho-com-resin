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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import com.caucho.config.jaxb.JaxbBeanType;
import com.caucho.config.types.ClassTypeStrategy;
import com.caucho.config.types.InitProgram;
import com.caucho.config.types.RawString;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Factory for returning type strategies.
 */
public class TypeStrategyFactory {
  private static final Logger log = Log.open(TypeStrategyFactory.class);
  private static L10N L = new L10N(TypeStrategyFactory.class);
  
  private static final HashMap<String,TypeStrategy> _primitiveTypes
    = new HashMap<String,TypeStrategy>();
  
  private static final EnvironmentLocal<TypeStrategyFactory> _localFactory
    = new EnvironmentLocal<TypeStrategyFactory>();
  
  private final HashMap<String,TypeStrategy> _typeMap
    = new HashMap<String,TypeStrategy>();

  private final HashMap<QName,AttributeStrategy> _flowMap
    = new HashMap<QName,AttributeStrategy>();

  private final HashMap<QName,AttributeStrategy> _envMap
    = new HashMap<QName,AttributeStrategy>();
  
  private TypeStrategyFactory()
  {

  }

  /**
   * Returns the appropriate strategy.
   */
  public static TypeStrategy getTypeStrategy(Class type)
  {
    TypeStrategyFactory factory = getFactory(type.getClassLoader());

    return factory.getTypeStrategyImpl(type);
  }
  
  public static AttributeStrategy getFlowAttribute(Class type,
                                                   QName name) throws Exception
  {
    return getFactory(type.getClassLoader()).getFlowAttribute(name);
  }

  private AttributeStrategy getFlowAttribute(QName name)
  {
    return _flowMap.get(name);
  }

  public static AttributeStrategy getEnvironmentAttribute(Class type,
                                                          QName name) throws Exception
  {
    return getFactory(type.getClassLoader()).getEnvironmentAttribute(name);
  }

  private AttributeStrategy getEnvironmentAttribute(QName name)
  {
    AttributeStrategy strategy = _envMap.get(name);

    if (strategy != null)
      return strategy;

    if (name.getLocalName() != null && ! name.getLocalName().equals("")) {
      strategy = _envMap.get(new QName(name.getLocalName(), null));
      return strategy;
    }
    else
      return null;
  }

  private static TypeStrategyFactory getFactory(ClassLoader loader)
  {
    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    TypeStrategyFactory factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = new TypeStrategyFactory();
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }

  /**
   * Initialize the type strategy factory with files in META-INF/caucho
   *
   * @param loader the owning class loader
   * @throws Exception
   */
  private void init(ClassLoader loader)
  {
    try {
      Enumeration<URL> urls = loader.getResources("META-INF/caucho/config-types.xml");

      while (urls.hasMoreElements()) {
	URL url = urls.nextElement();

	InputStream is = url.openStream();
	
	try {
	  new Config(loader).configure(this, is);
	} finally {
	  is.close();
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Adds a new TypeStrategy
   */
  public void addTypeStrategy(TypeStrategyConfig config)
  {
    _typeMap.put(config.getName(), config.getType());
  }

  /**
   * Adds an new environmnent attribute.
   */
  public FlowAttributeConfig createFlowAttribute()
  {
    return new FlowAttributeConfig();
  }

  /**
   * Adds an new environmnent attribute.
   */
  public EnvironmentAttributeConfig createEnvironmentAttribute()
  {
    return new EnvironmentAttributeConfig();
  }

  private TypeStrategy getTypeStrategyImpl(Class type)
  {
    TypeStrategy strategy = _typeMap.get(type.getName());

    if (strategy == null) {
      strategy = _primitiveTypes.get(type.getName());

      if (strategy != null) {
      }
      else if (EnvironmentBean.class.isAssignableFrom(type))
        strategy = new EnvironmentTypeStrategy(type);
      else if (type.isAnnotationPresent(XmlRootElement.class))
        strategy = new JaxbBeanType(type);
      else
        strategy = new BeanTypeStrategy(type);

      _typeMap.put(type.getName(), strategy);
    }

    return strategy;
  }

  // configuration types
  public static class TypeStrategyConfig {
    private String _name;
    private TypeStrategy _type;

    public void setName(Class type)
    {
      _name = type.getName();
    }

    public String getName()
    {
      return _name;
    }

    public void setType(Class type)
            throws ConfigException, IllegalAccessException, InstantiationException
    {
      Config.validate(type, TypeStrategy.class);

      _type = (TypeStrategy) type.newInstance();
    }

    public TypeStrategy getType()
    {
      return _type;
    }
  }

  public class EnvironmentAttributeConfig {
    public void put(QName name, Class type)
    {
      TypeStrategy typeStrategy = getTypeStrategyImpl(type);

      _envMap.put(name, new EnvironmentAttributeStrategy(typeStrategy));
    }
  }

  public class FlowAttributeConfig {
    public void put(QName name, Class type)
    {
      TypeStrategy typeStrategy = getTypeStrategyImpl(type);

      _flowMap.put(name, new EnvironmentAttributeStrategy(typeStrategy));
    }
  }
   // catalog of primitive types.

  private static class PrimitiveBooleanTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive boolean.
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureRawString(node);

      if (value == null || value.equals(""))
	return Boolean.TRUE; // empty flag tags are true
      else
	return builder.evalBoolean(value) ? Boolean.TRUE : Boolean.FALSE;
    }
  }

  private static class PrimitiveByteTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive byte
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Byte((byte) 0);
      else
	return new Byte(value);
    }
  }

  private static class PrimitiveShortTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive short
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Short((short) 0);
      else
	return new Short(value);
    }
  }

  private static class PrimitiveIntTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive int
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Integer(0);
      else
	return new Integer(value);
    }
  }

  private static class PrimitiveLongTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive int
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Long(0);
      else
	return new Long(value);
    }
  }

  private static class PrimitiveFloatTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive float
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Float(0);
      else
	return new Float(value);
    }
  }

  private static class PrimitiveDoubleTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive double
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return new Double(0);
      else
	return new Double(value);
    }
  }

  private static class PrimitiveCharTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a primitive char
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.length() == 0)
	return new Character((char) 0);
      else if (value.length() == 1)
        return new Character(value.charAt(0));
      else
        throw builder.error(L.l("Character must be a single char '{0}'", value),
                            node);
    }
  }

  private static class BooleanTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a boolean object.
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureRawString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return builder.evalBoolean(value) ? Boolean.TRUE : Boolean.FALSE;
    }
  }

  private static class ByteTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a byte object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Byte(value);
    }
  }

  private static class ShortTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a short object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Short(value);
    }
  }

  private static class IntegerTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as an integer object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Integer(value);
    }
  }

  private static class LongTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a long object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Long(value);
    }
  }

  private static class FloatTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a float object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Float(value);
    }
  }

  private static class DoubleTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a double object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.equals(""))
	return null;
      else
	return new Double(value);
    }
  }

  private static class CharacterTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a character object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      String value = builder.configureString(node);

      if (value == null || value.length() == 0)
	return null;
      else if (value.length() == 1)
        return new Character(value.charAt(0));
      else
        throw builder.error(L.l("Character must be a single char '{0}'", value),
                            node);
    }
  }

  private static class StringTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a string object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return builder.configureString(node);
    }
  }

  private static class RawStringTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a string object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return new RawString(builder.configureRawStringNoTrim(node));
    }
  }

  private static class NodeTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a node object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return node;
    }
  }

  private static class ObjectTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as an object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return builder.configureObject(node, parent);
    }
  }

  private static class InitProgramTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as an init program object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public void configureBean(NodeBuilder builder, Object bean, Node node)
      throws Exception
    {
      InitProgram program = (InitProgram) bean;

      program.addBuilderProgram(new NodeBuilderProgram(builder, node));
    }
    
    /**
     * Configures the node as an init program object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return new InitProgram(new NodeBuilderProgram(builder, node));
    }
  }

  private static class BuilderProgramTypeStrategy extends TypeStrategy {
    /**
     * Configures the node as a child program object
     *
     * @param builder the builder context.
     * @param node the configuration node
     * @param parent
     */
    public Object configure(NodeBuilder builder, Node node, Object parent)
      throws Exception
    {
      return new NodeBuilderChildProgram(builder, node);
    }
  }

  static {
    _primitiveTypes.put("boolean", new PrimitiveBooleanTypeStrategy());
    _primitiveTypes.put("byte", new PrimitiveByteTypeStrategy());
    _primitiveTypes.put("short", new PrimitiveShortTypeStrategy());
    _primitiveTypes.put("int", new PrimitiveIntTypeStrategy());
    _primitiveTypes.put("long", new PrimitiveLongTypeStrategy());
    _primitiveTypes.put("float", new PrimitiveFloatTypeStrategy());
    _primitiveTypes.put("double", new PrimitiveDoubleTypeStrategy());
    _primitiveTypes.put("char", new PrimitiveCharTypeStrategy());

    _primitiveTypes.put("java.lang.Boolean", new BooleanTypeStrategy());
    _primitiveTypes.put("java.lang.Byte", new ByteTypeStrategy());
    _primitiveTypes.put("java.lang.Short", new ShortTypeStrategy());
    _primitiveTypes.put("java.lang.Integer", new IntegerTypeStrategy());
    _primitiveTypes.put("java.lang.Long", new LongTypeStrategy());
    _primitiveTypes.put("java.lang.Float", new FloatTypeStrategy());
    _primitiveTypes.put("java.lang.Double", new DoubleTypeStrategy());
    _primitiveTypes.put("java.lang.Character", new CharacterTypeStrategy());

    _primitiveTypes.put("java.lang.Object", new ObjectTypeStrategy());
    _primitiveTypes.put("java.lang.String", new StringTypeStrategy());
    _primitiveTypes.put("com.caucho.config.types.RawString",
                        new RawStringTypeStrategy());
    _primitiveTypes.put("org.w3c.dom.Node", new NodeTypeStrategy());
    _primitiveTypes.put("java.lang.Class", new ClassTypeStrategy());

    _primitiveTypes.put("com.caucho.config.types.InitProgram",
                        new InitProgramTypeStrategy());
    _primitiveTypes.put("com.caucho.config.BuilderProgram",
                        new BuilderProgramTypeStrategy());
  }
}
