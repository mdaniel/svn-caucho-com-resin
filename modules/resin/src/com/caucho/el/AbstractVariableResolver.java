/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.jsp.el.VariableResolver;

import com.caucho.util.NullIterator;

import com.caucho.log.Log;

/**
 * Abstract variable resolver.  Supports chaining and the "Var"
 * special variable.
 */
public class AbstractVariableResolver extends AbstractMap<String,Object>
  implements VariableResolver {
  private static final Logger log = Log.open(AbstractVariableResolver.class);
  
  private VariableResolver _next;
  
  /**
   * Creates the resolver
   */
  public AbstractVariableResolver()
  {
  }
  
  /**
   * Creates the resolver
   */
  public AbstractVariableResolver(VariableResolver next)
  {
    _next = next;
  }

  /**
   * Returns the next resolver.
   */
  public VariableResolver getNext()
  {
    return _next;
  }
  
  /**
   * Returns the named variable value.
   */
  public Object resolveVariable(String var)
  {
    Object value = null;

    if (_next != null) {
      try {
	value = _next.resolveVariable(var);
      } catch (javax.servlet.jsp.el.ELException e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    
      if (value == _next)
        return this;
      else if (value != null)
        return value;
    }

    if (var.equals("Var"))
      return this;
    else
      return null;
  }

  // Map API

  /**
   * Returns the named specified value
   */
  public Object get(String var)
  {
    return resolveVariable(var);
  }

  /**
   * Returns the named specified value
   */
  public Object get(Object var)
  {
    return resolveVariable((String) var);
  }

  /**
   * Return dummy 0 for the size.
   */
  public int size()
  {
    return 0;
  }

  /**
   * Return dummy null iterator for the entries
   */
  public Set<Entry<String,Object>> entrySet()
  {
    java.util.HashSet<Entry<String,Object>> set;
    set = new java.util.HashSet<Entry<String,Object>>();
    
    return set;
  }

  public String toString()
  {
    return "AbstractVariableResolver[]";
  }
}
