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

package com.caucho.webbeans.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.naming.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.manager.WebBeansContainer;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class BeanConfig extends WbComponentConfig {
  private static final L10N L = new L10N(BeanConfig.class);

  private String _jndiName;
  
  private String _mbeanName;
  private Class _mbeanClass;
  
  public BeanConfig()
  {
    setScope("singleton");
  }

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  public void setMBeanName(String mbeanName)
  {
    _mbeanName = mbeanName;
  }

  public void setMBeanClass(Class cl)
  {
    _mbeanClass = cl;
  }

  public void init()
  {
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
}
