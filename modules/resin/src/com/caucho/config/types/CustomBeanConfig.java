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
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.*;
import com.caucho.config.j2ee.*;
import com.caucho.jca.program.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.context.*;
import com.caucho.xml.QName;

import java.util.*;
import java.lang.reflect.*;
import java.lang.annotation.*;

import javax.annotation.*;

import javax.resource.spi.*;

import javax.webbeans.*;

/**
 * Custom bean configured by namespace
 */
public class CustomBeanConfig {
  private static final L10N L = new L10N(CustomBeanConfig.class);

  private static final String RESIN_NS
    = "http://caucho.com/ns/resin";

  private Class _class;
  private WbComponentConfig _component = new WbComponentConfig();
  private ConfigType _configType;

  private QName _name;

  private String _filename;
  private int _line;

  public CustomBeanConfig(QName name, Class cl)
  {
    _name = name;

    _class = cl;
    _component.setClass(cl);

    _configType = TypeFactory.getType(cl);
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

  public void setClass(Class cl)
  {
    _component.setClass(cl);
  }

  public void setScope(String scope)
  {
    _component.setScope(scope);
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    QName name = program.getQName();

    if (name == null) {
      _component.addInitProgram(program);
    }
    
    else if (name.getNamespaceURI().equals(_name.getNamespaceURI())) {
      if (_configType.getAttribute(name) == null)
	throw new ConfigException(L.l("'{0}' is an unknown field for '{1}'",
				      name.getLocalName(), _class.getName()));
      
      _component.addInitProgram(program);
    }

    else if (name.getNamespaceURI().equals(RESIN_NS)) {
      // XXX: temp

      // XXX: service scope?
      _component.setService(true);
    }
    else {
      throw new ConfigException(L.l("'{0}' is an unknown field name.  Fields must belong to the same namespace as the class",
				    name.getCanonicalName()));
    }
  }

  public WbComponentConfig getComponent()
  {
    return _component;
  }

  @PostConstruct
  public void init()
  {
    _component.init();
  }
}
