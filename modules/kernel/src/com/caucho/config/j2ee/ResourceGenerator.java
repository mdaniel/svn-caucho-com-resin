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
 * @author Scott Ferguson
 */

package com.caucho.config.j2ee;

import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.ConfigContext;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import javax.naming.*;
import javax.persistence.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.inject.manager.Bean;
import javax.rmi.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generator for the @Resource tag.
 */
public class ResourceGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(ResourceGenerator.class.getName());
  private static final L10N L = new L10N(ResourceGenerator.class);

  private final Class _type;
  private final String _mappedName;
  
  private final String _jndiName;
  
  private final String _location;

  private InjectManager _webBeans;
  private Bean _bean;
  private boolean _isBound;

  ResourceGenerator(Class type,
		    String mappedName,
		    String jndiName,
		    String location)
  {
    _type = type;
    _mappedName = mappedName;
    
    _jndiName = jndiName;
    
    _location = location;
  }

  /**
   * Returns the expected type
   */
  @Override
  public Class getType()
  {
    return _type;
  }

  /**
   * Creates the value.
   */
  public Object create()
  {
    if (_bean == null && ! _isBound) {
      _isBound = false;
      
      InjectManager webBeans = InjectManager.create();

      if (_mappedName != null && ! "".equals(_mappedName)) {
	_bean = InjectIntrospector.bind(_location, _type, _mappedName);
	
	if (_bean == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;
	  
	  throw new ConfigException(_location + L.l("'{0}' with mappedName='{1}' is an unknown @EJB", _type.getName(), _mappedName));
	}
      }
      else if (_jndiName != null && ! "".equals(_jndiName)) {
	_bean = InjectIntrospector.bind(_location, _type, _jndiName);
	
	if (_bean == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;
	}
      }

      if (_bean == null) {
	_bean = InjectIntrospector.bind(_location, _type);
	
	if (_bean == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;

	  throw new ConfigException(_location + L.l("'{0}'  is an unknown @Resource", _type.getName()));
	}
      }

      if (_bean != null && _jndiName != null && ! "".equals(_jndiName)) {
	try {
	  Jndi.bindDeepShort(_jndiName, _bean);
	} catch (NamingException e) {
	  throw ConfigException.create(e);
	}
      }
    }

    if (_bean != null)
      return _webBeans.getReference(_bean);
    else
      return getJndiValue(_type);
  }

  private Object getJndiValue(Class type)
  {
    if (_jndiName == null || "".equals(_jndiName))
      return null;
    
    try {
      Object value = Jndi.lookup(_jndiName);

      if (value != null)
	return PortableRemoteObject.narrow(value, type);
      else
	return null;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getName());
    
    if (_mappedName != null) {
      sb.append(", mappedName=");
      sb.append(_mappedName);
    }
    
    if (_jndiName != null) {
      sb.append(", jndiName=");
      sb.append(_jndiName);
    }

    sb.append("]");

    return sb.toString();
  }
}
