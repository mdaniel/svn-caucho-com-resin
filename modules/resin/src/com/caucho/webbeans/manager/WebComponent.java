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

package com.caucho.webbeans.manager;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.*;
import com.caucho.webbeans.component.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.ArrayList;

/**
 * Configuration for the xml web bean component.
 */
public class WebComponent {
  private static L10N L = new L10N(WebComponent.class);
  
  private Type _type;

  private ArrayList<ComponentImpl> _componentList
    = new ArrayList<ComponentImpl>();

  public WebComponent(Type type)
  {
    _type = type;
  }

  public void addComponent(ComponentImpl comp)
  {
    for (int i = _componentList.size() - 1; i >= 0; i--) {
      ComponentImpl oldComponent = _componentList.get(i);

      if (! comp.getClassName().equals(oldComponent.getClassName())) {
      }
      else if (comp.isFromClass() && ! oldComponent.isFromClass())
	return;
      else if (! comp.isFromClass() && oldComponent.isFromClass())
	_componentList.remove(i);
      else if (comp.equals(oldComponent)) {
	return;
      }
    }

    _componentList.add(comp);
  }
  
  public void createProgram(ArrayList<ConfigProgram> initList,
			    Field field,
			    ArrayList<Annotation> bindList)
    throws ConfigException
  {
    ComponentImpl comp = bind(WebBeansContainer.location(field), bindList);

    comp.createProgram(initList, field);
  }
  
  public ComponentImpl bind(String location, ArrayList<Annotation> bindList)
    throws ConfigException
  {
    ComponentImpl matchComp = null;
    ComponentImpl secondComp = null;

    for (int i = 0; i < _componentList.size(); i++) {
      ComponentImpl comp = _componentList.get(i);

      if (! comp.isMatch(bindList))
	continue;

      if (matchComp == null)
	matchComp = comp;
      else if (comp.getBindingList().size() == bindList.size()
	       && matchComp.getBindingList().size() != bindList.size()) {
	matchComp = comp;
	secondComp = null;
      }
      else if (matchComp.getBindingList().size() == bindList.size()
	       && comp.getBindingList().size() != bindList.size()) {
      }
      else if (matchComp.getType().getPriority() < comp.getType().getPriority()) {
	matchComp = comp;
	secondComp = null;
      }
      else if (comp.getType().getPriority() < matchComp.getType().getPriority()) {
      }
      else {
	secondComp = comp;
      }
    }

    if (matchComp == null) {
      return null;
    }
    else if (matchComp != null && secondComp != null) {
	throw new ConfigException(location +
				  L.l("WebBeans conflict between '{0}' and '{1}'.  WebBean injection must match uniquely.",
					      matchComp, secondComp));
    }
    
    return matchComp;
  }
  
  public ComponentImpl bindByBindings(String location,
				      Type type,
				      ArrayList<Binding> bindList)
    throws ConfigException
  {
    ComponentImpl matchComp = null;
    ComponentImpl secondComp = null;

    for (int i = 0; i < _componentList.size(); i++) {
      ComponentImpl comp = _componentList.get(i);

      if (! comp.isMatchByBinding(bindList))
	continue;

      if (matchComp == null)
	matchComp = comp;
      else if (comp.getBindingList().size() == bindList.size()
	       && matchComp.getBindingList().size() != bindList.size()) {
	matchComp = comp;
	secondComp = null;
      }
      else if (matchComp.getBindingList().size() == bindList.size()
	       && comp.getBindingList().size() != bindList.size()) {
      }
      else if (matchComp.getType().getPriority() < comp.getType().getPriority()) {
	matchComp = comp;
	secondComp = null;
      }
      else if (comp.getType().getPriority() < matchComp.getType().getPriority()) {
      }
      else {
	secondComp = comp;
      }
    }

    if (matchComp == null) {
      return null;
    }
    else if (matchComp != null && secondComp != null) {
      throw new ConfigException(location
				+ L.l("Injection of '{0}' conflicts between '{1}' and '{2}'.  WebBean injection must match uniquely.",
				      getName(type), matchComp, secondComp));
    }
    
    return matchComp;
  }

  static String getName(Type type)
  {
    if (type instanceof Class)
      return ((Class) type).getName();
    else
      return String.valueOf(type);
  }
}
