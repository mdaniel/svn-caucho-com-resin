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
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.context.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

/**
 * Configuration for the xml web bean component.
 */
public class WbComponent {
  private Class _cl;
  private WbComponentType _type;

  private String _name;
  
  private ArrayList<WbBinding> _bindingList
    = new ArrayList<WbBinding>();

  private Annotation _scopeAnn;
  private RequestScope _scopeContext;

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the component type.
   */
  public void setType(WbComponentType type)
  {
    _type = type;
  }

  /**
   * Gets the component type.
   */
  public WbComponentType getType()
  {
    return _type;
  }

  /**
   * Sets the component implementation class.
   */
  public void setClass(Class cl)
  {
    _cl = cl;

    if (_name == null) {
      String className = cl.getName();
      int p = className.lastIndexOf('.');
      
      char ch = Character.toLowerCase(className.charAt(p + 1));
      
      _name = ch + className.substring(p + 2);
    }
  }
  
  public String getClassName()
  {
    if (_cl != null)
      return _cl.getName();
    else
      return null;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(WbBinding binding)
  {
    _bindingList.add(binding);
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
  }

  /**
   * Sets the scope annotation.
   */
  public void setScopeAnnotation(Annotation scopeAnn)
  {
    _scopeAnn = scopeAnn;

    if (scopeAnn != null)
      _scopeContext = new RequestScope();
  }

  /**
   * Gets the scope annotation.
   */
  public Annotation getScopeAnnotation()
  {
    return _scopeAnn;
  }

  public boolean isMatch(ArrayList<Annotation> bindList)
  {
    for (int i = 0; i < bindList.size(); i++) {
      if (! isMatch(bindList.get(i)))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatch(Annotation bindAnn)
  {
    for (int i = 0; i < _bindingList.size(); i++) {
      if (_bindingList.get(i).isMatch(bindAnn))
	return true;
    }
    
    return false;
  }

  public Object get()
  {
    if (_scopeContext != null) {
      Object value = _scopeContext.get(_name);

      try {
	value = _cl.newInstance();

	_scopeContext.set(_name, value);

	return value;
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }
    else
      return null;
  }
  
  public void createProgram(ArrayList<BuilderProgram> initList,
			    AccessibleObject field,
			    String name,
			    AccessibleInject inject)
    throws ConfigException
  {
    BuilderProgram program = new NewComponentProgram(_cl, inject);

    initList.add(program);
  }

  public String toString()
  {
    return "WbComponent[" + _cl + "]";
  }
}
