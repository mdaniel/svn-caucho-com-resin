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

package com.caucho.jsp.el;

import java.beans.FeatureDescriptor;
import java.util.*;

import javax.servlet.jsp.el.*;
import javax.el.*;

import com.caucho.el.*;
import com.caucho.jsp.*;

/**
 * Variable resolution for JSP variables
 */
public class PageContextELResolver extends AbstractVariableResolver {
  private final PageContextImpl _pageContext;

  private final ELResolver []_customResolvers;
  
  private final MapELResolver _mapResolver = new MapELResolver();
  private final ListELResolver _listResolver = new ListELResolver();
  private final ArrayELResolver _arrayResolver = new ArrayELResolver();
  private final BeanELResolver _beanResolver = new BeanELResolver();

  public PageContextELResolver(PageContextImpl pageContext,
			       ELResolver []customResolvers)
  {
    _customResolvers = customResolvers;
    
    _pageContext = pageContext;
  }

  public ELResolver []getCustomResolvers()
  {
    return _customResolvers;
  }
  
  public Object getValue(ELContext env, Object base, Object property)
  {
    env.setPropertyResolved(false);

    for (int i = 0; i < _customResolvers.length; i++) {
      Object value = _customResolvers[i].getValue(env, base, property);

      if (env.isPropertyResolved())
	return value;
    }
    
    if (base != null) {
      if (base instanceof Map)
	return _mapResolver.getValue(env, base, property);
      else if (base instanceof List)
	return _listResolver.getValue(env, base, property);
      else if (base.getClass().isArray())
	return _arrayResolver.getValue(env, base, property);
      else
	return _beanResolver.getValue(env, base, property);
    }
    else if (property instanceof String) {
      env.setPropertyResolved(true);
	
      return _pageContext.getAttribute(property.toString());
    }
    else
      return null;
  }
    
  public void setValue(ELContext env,
		       Object base,
		       Object property,
		       Object value)
  {
    env.setPropertyResolved(false);
    
    if (base != null) {
      if (base instanceof Map)
	_mapResolver.setValue(env, base, property, value);
      else if (base instanceof List)
	_listResolver.setValue(env, base, property, value);
      else if (base.getClass().isArray())
	_arrayResolver.setValue(env, base, property, value);
      else
	_beanResolver.setValue(env, base, property, value);
    }
    else if (property instanceof String) {
      env.setPropertyResolved(true);
	
      _pageContext.setAttribute(property.toString(), value);
    }
  }
}
