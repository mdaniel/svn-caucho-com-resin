/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.cfg;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.custom.CustomBean;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.util.L10N;

/**
 * Backwards compatibility class for 3.1-style &lt;jms-queue>, etc.
 */
abstract public class BeanConfigBase {
  private static final L10N L = new L10N(BeanConfigBase.class);

  private String _filename;
  private int _line;

  private String _name;
  private String _jndiName;

  private Class<?> _cl;

  private ArrayList<Annotation> _annotations
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _qualifiers
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _stereotypes
    = new ArrayList<Annotation>();

  private Class<? extends Annotation> _scope;

  private ContainerProgram _init;

  private CustomBean _beanConfig;

  protected BeanConfigBase()
  {
  }

  /**
   * Sets the configuration location
   */
  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLine()
  {
    return _line;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;

    // _bindingList.add(Names.create(name));
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Assigns the class
   */
  public void setClass(Class<?> cl)
  {
    _cl = cl;
  }

  /**
   * Returns the instance class
   */
  public Class<?> getInstanceClass()
  {
    return _cl;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(Annotation binding)
  {
    _annotations.add(binding);
  }

  /**
   * Adds a component binding.
   */
  public void add(Annotation binding)
  {
    _annotations.add(binding);

    if (binding.annotationType().isAnnotationPresent(Qualifier.class))
      _qualifiers.add(binding);
  }
  
  public void add(CustomBean beanConfig)
  {
    _beanConfig = beanConfig;
  }
  
  protected CustomBean getBeanConfig()
  {
    return _beanConfig;
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
    if ("singleton".equals(scope))
      add(SingletonLiteral.ANN);
    else if ("dependent".equals(scope))
      add(DependentLiteral.ANN);
    else if ("request".equals(scope))
      add(RequestScopedLiteral.ANN);
    else if ("session".equals(scope))
      add(SessionScopedLiteral.ANN);
    else if ("application".equals(scope))
      add(ApplicationScopedLiteral.ANN);
    else if ("conversation".equals(scope))
      add(ConversationScopedLiteral.ANN);
    else {
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @Scope annotation."));
    }
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Sets the init program.
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  protected void initImpl()
  {
  }

  @PostConstruct
  public final void init()
  {
    initImpl();

    if (_cl == null && _beanConfig == null) {
      throw new ConfigException(L.l("{0} requires a 'class' attribute",
                                    getClass().getSimpleName()));
    }
  }
  
  protected <X> Bean<X> deploy()
  {
    InjectManager beanManager = InjectManager.create();

    //AnnotatedTypeImpl<X> beanType = buildAnnotatedType();

    /*
    BeanBuilder<X> builder = beanManager.createBeanFactory(beanType);

    if (_scope != null)
      builder.scope(_scope);

    if (_init != null)
      builder.init(_init);
    
    for (Annotation qualifier : _qualifiers)
      builder.qualifier(qualifier);

    Object value = replaceObject();
    Bean<X> bean = null;

    if (value != null) {
      bean = builder.singleton(value);
      beanManager.addBeanDiscover(bean);
    }
    else {
      bean = builder.bean();
      beanManager.addBeanDiscover(bean);
    }

    return bean;
    */
    
    return null;
  }
  
  /*
  protected <X> AnnotatedTypeImpl<X> buildAnnotatedType()
  {
    CandiManager cdiManager = CandiManager.create();
    
    AnnotatedType<X> annType = (AnnotatedType<X>) ReflectionAnnotatedFactory.introspectType(_cl);
    AnnotatedTypeImpl<X> beanType;
    
    BeanManagerBase beanManager = cdiManager.getBeanManager();
    
    beanType = new AnnotatedTypeImpl<X>(annType, beanManager);

    if (_name != null) {
      beanType.addAnnotation(Names.create(_name));
    }

    for (Annotation binding : _qualifiers) {
      beanType.addAnnotation(binding);
    }

    for (Annotation stereotype : _stereotypes) {
      beanType.addAnnotation(stereotype);
    }

    for (Annotation ann : _annotations) {
      beanType.addAnnotation(ann);
    }
    
    return beanType;
  }
  */

  protected Object replaceObject()
  {
    return null;
  }
}
