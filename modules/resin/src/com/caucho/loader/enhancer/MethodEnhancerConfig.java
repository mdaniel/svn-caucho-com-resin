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

package com.caucho.loader.enhancer;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.aopalliance.intercept.MethodInterceptor;

import com.caucho.aop.AopMethodEnhancer;

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JMethod;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.BaseMethod;

import com.caucho.util.L10N;

/**
 * Configuration for a method-enhancer builder.
 */
public class MethodEnhancerConfig implements MethodEnhancer {
  private static final L10N L = new L10N(MethodEnhancerConfig.class);

  private static final Logger log =
    Logger.getLogger(MethodEnhancerConfig.class.getName());
  
  private EnhancerManager _manager;
  
  private Class _annotation;
  private Class _type;
  private boolean _isStatic = true;

  private MethodEnhancer _enhancer;

  /**
   * Sets the manager.
   */
  public void setEnhancerManager(EnhancerManager manager)
  {
    _manager = manager;
  }

  /**
   * Sets the annotation.
   */
  public void setAnnotation(Class ann)
  {
    _annotation = ann;
  }

  /**
   * Gets the annotation.
   */
  public Class getAnnotation()
  {
    return _annotation;
  }
  
  /**
   * Sets the type of the method enhancer.
   */
  public void setType(Class type)
    throws Exception
  {
    _type = type;

    if (MethodEnhancer.class.isAssignableFrom(type)) {
      _enhancer = (MethodEnhancer) type.newInstance();
    }
    else if (MethodInterceptor.class.isAssignableFrom(type)) {
      AopMethodEnhancer aopEnhancer = new AopMethodEnhancer();
      aopEnhancer.setType(type);
      
      _enhancer = aopEnhancer;
    }
    else
      throw new ConfigException(L.l("'{0}' is an unsupported interceptor type.  MethodInterceptor is required.",
				    type.getName()));
  }

  /**
   * Set true for a static instance.
   */
  public void setStatic(boolean isStatic)
  {
    _isStatic = isStatic;
  }

  /**
   * Initializes the config.
   */
  public void init()
    throws ConfigException
  {
    _enhancer.setAnnotation(_annotation);

    if (_enhancer instanceof AopMethodEnhancer)
      ((AopMethodEnhancer) _enhancer).setStatic(_isStatic);
  }

  /**
   * Enhances the method.
   *
   * @param genClass the generated class
   * @param jMethod the method to be enhanced
   * @param jAnn the annotation to be enhanced
   */
  public void enhance(GenClass genClass,
		      JMethod jMethod,
		      JAnnotation jAnn)
  {
    _enhancer.enhance(genClass, jMethod, jAnn);
  }
}
