/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson;
 */

package com.caucho.config;

import com.caucho.config.j2ee.Inject;
import com.caucho.webbeans.context.DependentScope;

/**
 * A saved program for configuring an object.
 */
public abstract class BuilderProgram extends Inject {
  //private NodeBuilder _nodeBuilder;
  private NodeBuilder _nodeBuilder;
  private ClassLoader _loader;

  protected BuilderProgram()
  {
    this(NodeBuilder.getCurrentBuilder());
  }

  protected BuilderProgram(NodeBuilder builder)
  {
    // server/13co
    /*
    _nodeBuilder = builder;

    if (builder == null)
      _nodeBuilder = new NodeBuilder(); // XXX:
    */

    _loader = Thread.currentThread().getContextClassLoader();
  }

  public void addProgram(BuilderProgram program)
  {
    throw new UnsupportedOperationException("Cannot add a program to a BuilderProgram. You probably need a BuilderProgramContainer.");
  }

  /**
   * Configures the object.
   */
  public void configure(Object bean)
    throws ConfigException
  {
    // server/23e7
    if (NodeBuilder.getCurrentBuilder() != null)
      configureImpl(NodeBuilder.getCurrentBuilder(), bean);
    else
      configureImpl(NodeBuilder.createForProgram(), bean);
  }

  /**
   * Configures the object.
   */
  public void configure(Object bean, DependentScope scope)
    throws ConfigException
  {
    NodeBuilder builder = NodeBuilder.createForProgram();

    builder.setDependentScope(scope);
    
    configureImpl(builder, bean);
  }

  /**
   * Configures the object.
   */
  public void configure(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    configureImpl(NodeBuilder.getCurrentBuilder(), bean);
  }

  public Object configure(Class type)
    throws ConfigException
  {
    if (NodeBuilder.getCurrentBuilder() != null)
      return configureImpl(NodeBuilder.getCurrentBuilder(), type);
    else
      return configureImpl(NodeBuilder.createForProgram(), type);
  }

  /**
   * Configures the object.
   */
  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    inject(bean, builder.getDependentScope());
  }

  /**
   * Configures a bean given a class to instantiate.
   */
  protected Object configureImpl(NodeBuilder builder, Class type)
    throws ConfigException
  {
    try {
      Object bean = type.newInstance();

      configureImpl(builder, bean);

      return bean;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void init(Object bean)
    throws ConfigException
  {
    Config.init(bean);
  }

  //
  // Inject API
  //

  /**
   * Configures the object.
   */
  @Override
  public void inject(Object bean, DependentScope scope)
    throws ConfigException
  {
    configure(bean);
  }
}
