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

package com.caucho.config.types;

import com.caucho.config.*;
import com.caucho.config.type.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.cfg.*;

/**
 * Configuration for the xml web bean component.
 */
public class BeanConfig extends WbComponentConfig {
  private static final L10N L = new L10N(BeanConfig.class);

  private String _filename;
  private int _line;

  private String _uri;
  
  private String _jndiName;
  
  private String _mbeanName;
  private Class _beanConfigClass;

  private CustomBeanConfig _customBean;
  
  public BeanConfig()
  {
    if (getDefaultScope() != null)
      setScope(getDefaultScope());
  }

  protected String getDefaultScope()
  {
    return "singleton";
  }

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

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;

    if (getName() == null)
      setName(jndiName);
  }

  public void setMbeanName(String mbeanName)
  {
    _mbeanName = mbeanName;
  }

  @Override
  public String getMBeanName()
  {
    return _mbeanName;
  }

  public void setMbeanClass(Class cl)
  {
    setMbeanInterface(cl);
  }

  public void setMbeanInterface(Class cl)
  {
  }

  public Class getBeanConfigClass()
  {
    return _beanConfigClass;
  }

  public void setBeanConfigClass(Class cl)
  {
    _beanConfigClass = cl;
  }

  /**
   * backwards compat
   */
  public void setType(Class cl)
  {
    setClass(cl);
  }

  @Override
  public Class getClassType()
  {
    if (_customBean != null)
      return _customBean.getClassType();
    else
      return super.getClassType();
  }

  /**
   * Check the class
   */
  @Override
  public void setClass(Class cl)
  {
    super.setClass(cl);

    Class type = getBeanConfigClass();

    if (type != null && ! type.isAssignableFrom(cl))
      throw new ConfigException(L.l("'{0}' is not a valid instance of '{1}'",
				    cl.getName(), type.getName()));
  }

  /**
   * uri-style configuration like the jms-queue url="memory:"
   */
  public void setUri(String uri)
  {
    Class beanConfigClass = getBeanConfigClass();

    if (beanConfigClass == null) {
      throw new ConfigException(L.l("'{0}' does not support the 'uri' attribute because its bean-config-class is undefined",
				    getClass().getName()));
    }

    _uri = uri;
    
    String scheme;
    String properties = "";

    int p = uri.indexOf(':');
    if (p >= 0) {
      scheme = uri.substring(0, p);
      properties = uri.substring(p + 1);
    }
    else
      scheme = uri;

    TypeFactory factory = TypeFactory.create();
    
    setClass(factory.getDriverClassByUrl(beanConfigClass, uri));

    String []props = properties.split("[;]");

    for (String prop : props) {
      if (prop.length() == 0)
	continue;
      
      String []values = prop.split("[=]");

      if (values.length != 2)
	throw new ConfigException(L.l("'{0}' is an invalid URI.  Bean URI syntax is 'scheme:prop1=value1;prop2=value2'", uri));

      addStringProperty(values[0], values[1]);
    }
  }

  /**
   * Returns the uri
   */
  public String getUri()
  {
    return _uri;
  }

  public void addCustomBean(CustomBeanConfig customBean)
  {
    _customBean = customBean;
  }
  
  public void init()
  {
    if (_customBean != null) {
      _customBean.initComponent();
      _comp = _customBean.getComponent();
      
      return;
    }

    setService(true);
    
    super.init();

    try {
      if (_comp == null) {
      }
      else if (_jndiName != null) {
	Jndi.bindDeepShort(_jndiName, _comp);
      }

      /*
      if (_mbeanName != null) {
	Object mbean = _object;

	if (_mbeanInterface != null)
	  mbean = new IntrospectionMBean(mbean, _mbeanInterface);
      
	Jmx.register(mbean, mbeanName);
	_mbeanInfo = mbeanServer.getMBeanInfo(mbeanName);
      }
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Introspection after the init has been set and before the @PostConstruct
   * for additional interception
   */
  @Override
  protected void introspectPostInit()
  {
  }
}
