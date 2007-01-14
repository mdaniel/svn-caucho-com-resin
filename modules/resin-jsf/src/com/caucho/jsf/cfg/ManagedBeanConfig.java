/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.cfg;

import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import javax.xml.bind.annotation.*;

import com.caucho.config.*;
import com.caucho.util.*;

@XmlRootElement(name="faces-config")
public class ManagedBeanConfig
{
  private static final L10N L = new L10N(ManagedBeanConfig.class);
  
  @XmlAttribute(name="id")
  private String _id;

  @XmlElement(name="managed-bean-name")
  private String _name;

  private Class _type;

  private Scope _scope = Scope.REQUEST;

  public String getName()
  {
    return _name;
  }
  
  @XmlElement(name="managed-bean-class")
  public void setManagedBeanClass(Class cl)
  {
    Config.checkCanInstantiate(cl);

    _type = cl;
  }

  public Class getManagedBeanClass()
  {
    return _type;
  }
  
  @XmlElement(name="managed-bean-scope")
  public void setManagedBeanScope(String scope)
  {
    if ("request".equals(scope))
      _scope = Scope.REQUEST;
    else if ("session".equals(scope))
      _scope = Scope.SESSION;
    else if ("application".equals(scope))
      _scope = Scope.APPLICATION;
    else
      throw new ConfigException(L.l("'{0}' is an unknown managed-bean-scope.  Expected values are request, session, or application.",
				    scope));
  }

  public String getManagedBeanScope()
  {
    return _scope.toString();
  }

  public Object create(FacesContext context)
    throws FacesException
  {
    try {
      Object value = _type.newInstance();

      ExternalContext extContext = context.getExternalContext();

      switch (_scope) {
      case REQUEST:
	extContext.getRequestMap().put(_name, value);
	break;
	
      case SESSION:
	extContext.getSessionMap().put(_name, value);
	break;
	
      case APPLICATION:
	extContext.getApplicationMap().put(_name, value);
	break;
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new FacesException(e);
    }
  }
  
  enum Scope {
    REQUEST,
    SESSION,
    APPLICATION
  };
}
