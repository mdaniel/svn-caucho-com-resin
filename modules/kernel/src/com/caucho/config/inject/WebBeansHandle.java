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

package com.caucho.config.inject;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
// import com.caucho.hessian.io.HessianHandle;
import com.caucho.config.cfg.*;

/**
 * Handle for webbeans serialization
 */
public class WebBeansHandle implements Serializable //, HessianHandle
{
  private static final L10N L = new L10N(WebBeansHandle.class);
  private static final Logger log
    = Logger.getLogger(WebBeansHandle.class.getName());
  
  private Class _type;
  private HashMap<Class,HashMap<String,Object>> _binding;

  public WebBeansHandle()
  {
  }

  /**
   * Constructor for known/system singletons
   */
  public WebBeansHandle(Class type)
  {
    _type = type;
    _binding = new HashMap<Class,HashMap<String,Object>>();
  }
  
  public WebBeansHandle(Type type, ArrayList<WbBinding> bindingList)
  {
    if (type instanceof Class)
      _type = (Class) type;

    _binding = new HashMap<Class,HashMap<String,Object>>();

    for (WbBinding binding : bindingList) {
      Class annType = binding.getBindingClass();
      HashMap<String,Object> valueMap = new HashMap<String,Object>();

      for (WbBinding.WbBindingValue value : binding.getValueList()) {
	valueMap.put(value.getName(), value.getValue());
      }
      
      _binding.put(annType, valueMap);
    }
  }

  /**
   * Deserialization resolution
   */
  public Object readResolve()
  {
    try {
      /*
      ArrayList<Binding> bindingList = new ArrayList<Binding>();
      
      for (Map.Entry<Class,HashMap<String,Object>> entry : _binding.entrySet()) {
	Class type = entry.getKey();
	HashMap<String,Object> valueMap = entry.getValue();

	Binding binding = new Binding(type);
	for (Map.Entry<String,Object> bindingEntry : valueMap.entrySet()) {
	  binding.put(bindingEntry.getKey(), bindingEntry.getValue());
	}

	bindingList.add(binding);
      }
      
      WebBeansContainer webBeans = WebBeansContainer.create();
      ComponentImpl comp = webBeans.bindByBindings("", _type, bindingList);

      if (comp != null)
	return comp.get();
      else {
	log.warning(L.l("'{0}' is an unknown WebBean at deserialization",
			this));
	return null;
      }
      */
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toString()
  {
    return getClass().getName() + "[" + _type + "," + _binding + "]";
  }
}
