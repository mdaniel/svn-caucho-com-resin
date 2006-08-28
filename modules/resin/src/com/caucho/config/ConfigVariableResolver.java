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

package com.caucho.config;

import java.util.HashMap;

import javax.el.*;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.el.EL;
import com.caucho.el.AbstractVariableResolver;

/**
 * Creates a variable resolver based on the classloader.
 */
public class ConfigVariableResolver extends AbstractVariableResolver {
  private HashMap<String,Object> _varMap = new HashMap<String,Object>();
  
  private ClassLoader _originalLoader;
  private ClassLoader _configureClassLoader;
  /**
   * Creates the resolver
   */
  public ConfigVariableResolver()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates the resolver
   */
  public ConfigVariableResolver(ClassLoader loader)
  {
    super(EL.getEnvironment(loader).getELResolver());

    _originalLoader = loader;
  }

  /**
   * Gets the configure environment class loader.
   */
  ClassLoader getConfigLoader()
  {
    return _configureClassLoader;
  }

  /**
   * Sets the configure environment class loader.
   */
  void setConfigLoader(ClassLoader loader)
  {
    // server/13co
    if (_configureClassLoader == null)
      _configureClassLoader = loader;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext context,
			 Object base,
			 Object property)
  {
    if (base != null)
      return null;
    else if (! (property instanceof String))
      return null;

    context.setPropertyResolved(true);

    String var = (String) property;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    for (;
	 loader != _originalLoader && loader != null;
	 loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
	Object value = EL.getLevelVar(var, loader);

	if (value == EL.NULL)
	  return null;
	else if (value != null)
	  return value;
      }
    }

    if (loader == _configureClassLoader) {
      Object value = EL.getLevelVar(var, loader);

      if (value == EL.NULL)
	return null;
      else if (value != null)
	return value;
    }
    
    Object value = _varMap.get(var);

    if (value == EL.NULL)
      return null;
    else if (value != null)
      return value;

    value = Environment.getAttribute(var, _originalLoader);

    if (value == EL.NULL)
      return null;
    else if (value != null)
      return value;
    else
      return super.getValue(context, base, property);
  }
  
  /**
   * Sets the value for the named variable.
   */
  @Override
  public void setValue(ELContext context,
		       Object base,
		       Object property,
		       Object value)
  {
    if (base != null || ! (property instanceof String))
      return;

    context.setPropertyResolved(true);

    String name = (String) property;
    
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (;
	 loader != _originalLoader && loader != null;
	 loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
	EL.putVar(name, value, loader);

	return;
      }
    }

    if (loader == _configureClassLoader) {
      EL.putVar(name, value, loader);

      return;
    }
    
    _varMap.put(name, value);
  }

  public String toString()
  {
    return "ConfigVariableResolver[]";
  }
}
