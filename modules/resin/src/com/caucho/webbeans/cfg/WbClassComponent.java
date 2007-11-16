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
import com.caucho.webbeans.*;
import com.caucho.webbeans.inject.*;
import com.caucho.webbeans.context.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class WbClassComponent extends WbComponent {
  private static final L10N L = new L10N(WbClassComponent.class);
  
  public WbClassComponent(WbWebBeans webbeans)
  {
    super(webbeans);
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect()
  {
    Class cl = getInstanceClass();
    
    for (Annotation ann : cl.getDeclaredAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	if (getType() != null)
	  throw new ConfigException(L.l("{0}: component type annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ComponentType.",
					cl.getName(),
					getType().getType().getName(),
					ann.annotationType().getName()));
	
	setComponentType(_webbeans.createComponentType(ann.annotationType()));
      }
      
      if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	if (getScopeAnnotation() != null)
	  throw new ConfigException(L.l("{0}: @ScopeType annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ScopeType.",
					cl.getName(),
					getScopeAnnotation().annotationType().getName(),
					ann.annotationType().getName()));
	
	setScopeAnnotation(ann);
      }
    }

    if (getType() == null) {
      throw new ConfigException(L.l("WebBeans component '{0}' does not have a ComponentType",
				    cl.getName()));
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectBindings()
  {
    for (Annotation ann : getInstanceClass().getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class))
	addBinding(new WbBinding(ann));
    }
  }
}
