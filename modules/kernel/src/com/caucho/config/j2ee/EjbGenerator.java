/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ValueGenerator;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.config.ConfigException;
import com.caucho.naming.*;
import com.caucho.util.L10N;

import javax.naming.*;
import javax.persistence.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.rmi.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generator for the @EJB tag.
 */
public class EjbGenerator extends ValueGenerator {
  private static final Logger log
    = Logger.getLogger(EjbGenerator.class.getName());
  private static final L10N L = new L10N(EjbGenerator.class);

  private final Class _type;
  private final String _mappedName;
  private final String _beanName;
  
  private final String _jndiName;
  
  private final String _location;

  private AbstractBean _component;
  private boolean _isBound;

  EjbGenerator(Class type,
	       String mappedName,
	       String beanName,
	       String jndiName,
	       String location)
  {
    _type = type;
    _mappedName = mappedName;
    _beanName = beanName;
    
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
    /*
    if (_component == null && ! _isBound) {
      _isBound = false;
      
      WebBeansContainer webBeans = WebBeansContainer.create();

      if (_mappedName != null && ! "".equals(_mappedName)) {
	_component = webBeans.bind("error", _type, _mappedName);
	
	if (_component == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;
	  
	  throw new ConfigException(_location + L.l("'{0}' with mappedName='{1}' is an unknown @EJB", _type.getName(), _mappedName));
	}
      }
      else if (_beanName != null && ! "".equals(_beanName)) {
	_component = webBeans.bind("error", _type, _beanName);
	
	if (_component == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;

	  throw new ConfigException(_location + L.l("'{0}' with beanName='{1}' is an unknown @EJB", _type.getName(), _beanName));
	}
      }
      else {
	_component = webBeans.bind("error", _type);
	
	if (_component == null) {
	  Object value = getJndiValue(_type);

	  if (value != null)
	    return value;

	  throw new ConfigException(_location + L.l("'{0}'  is an unknown @EJB", _type.getName()));
	}
      }

      if (_component != null && _jndiName != null && ! "".equals(_jndiName)) {
	try {
	  Jndi.bindDeepShort(_jndiName, _component);
	} catch (NamingException e) {
	  throw ConfigException.create(e);
	}
      }
    }

    if (_component != null)
      return _component.get();
    else
      return getJndiValue(_type);
    */
    return null;
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
    
    if (_beanName != null) {
      sb.append(", beanName=");
      sb.append(_beanName);
    }
    
    if (_jndiName != null) {
      sb.append(", jndiName=");
      sb.append(_jndiName);
    }

    sb.append("]");

    return sb.toString();
  }
}
