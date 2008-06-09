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
 * @author Alex Rojkov
 */

package com.caucho.jsf.el;

import com.caucho.util.L10N;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.faces.application.ResourceHandler;
import javax.faces.application.Resource;
import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.logging.Logger;

public class ResourceELResolver
  extends ELResolver
{
  private static final L10N L = new L10N(ResourceELResolver.class);
  private static final Logger log
    = Logger.getLogger(ResourceELResolver.class.getName());

  public ResourceELResolver()
  {
  }

  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base != null)
      return null;
    else
      return Object.class;
  }

  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    return null;
  }

  public Class<?> getType(ELContext context, Object base, Object property)
  {
    return null;
  }

  public Object getValue(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    if (property != null && base instanceof ResourceHandler) {

      final String name = (String) property;

      String[] nameParts = name.split(":");

      if (nameParts.length > 2) {
        throw new ELException(L.l("Resource property '{0}' should have at most one colon, e.g. '{1}'", name, "library:resource"));
      }

      ResourceHandler resourceHandler = (ResourceHandler) base;

      final Resource resource;

      if (nameParts.length == 2) {
        resource
          = resourceHandler.createResource(nameParts[1], nameParts[0]);
      } else if (nameParts.length == 1) {
        resource
          = resourceHandler.createResource(nameParts[0]);
      } else {
        resource = null;
      }

      if (resource != null)
        context.setPropertyResolved(true);
      

      return resource;
    }

    return null;
  }

  public boolean isReadOnly(ELContext context, Object base, Object property)
    throws PropertyNotFoundException, ELException
  {
    return false;
  }

  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
    throws PropertyNotFoundException, PropertyNotWritableException, ELException
  {
    return;
  }
}