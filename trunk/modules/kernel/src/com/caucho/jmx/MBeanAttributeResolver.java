/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 */

package com.caucho.jmx;

import java.beans.FeatureDescriptor;
import java.util.*;
import java.util.logging.*;

import javax.el.*;
import javax.management.*;

public class MBeanAttributeResolver extends ELResolver
{
  private final static Logger log
    = Logger.getLogger(MBeanAttributeResolver.class.getName());
  
  private MBeanServer _mBeanServer;

  public MBeanAttributeResolver()
  {
    _mBeanServer = Jmx.getGlobalMBeanServer();
  }
  
  public MBeanAttributeResolver(MBeanServer mBeanServer)
  {
    _mBeanServer = mBeanServer;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base instanceof ObjectName)
      return String.class;
    else
      return null;
  }

  @Override
  public Iterator<FeatureDescriptor> 
    getFeatureDescriptors(ELContext context, Object base)
  {
    List<FeatureDescriptor> featureDescriptors = 
      new ArrayList<FeatureDescriptor>();
    
    if (! (base instanceof ObjectName))
      return featureDescriptors.iterator();
    
    ObjectName objectName = (ObjectName) base;
    
    MBeanInfo mBeanInfo = null;
    
    try {
      mBeanInfo = _mBeanServer.getMBeanInfo(objectName);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      return featureDescriptors.iterator();
    }
    
    MBeanAttributeInfo []attribs = mBeanInfo.getAttributes();

    for (MBeanAttributeInfo attrib : attribs) {
      
      String name = attrib.getName();
      
      FeatureDescriptor desc = new FeatureDescriptor();
      desc.setName(name);
      desc.setDisplayName(name);
      desc.setShortDescription("");
      desc.setExpert(false);
      desc.setHidden(false);
      desc.setPreferred(true);
  
      desc.setValue(ELResolver.TYPE, String.class);
      desc.setValue(ELResolver.RESOLVABLE_AT_DESIGN_TIME, Boolean.TRUE);
  
      featureDescriptors.add(desc);
    }
  
    return featureDescriptors.iterator();
  }

  @Override
  public Class<?> getType(ELContext context, Object base, Object property)
  {
    if (! (base instanceof ObjectName))
      return null;
    
    context.setPropertyResolved(true);
    
    return Object.class;
  }

  @Override
  public Object getValue(ELContext context, Object base, Object property)
  {
    if (! (base instanceof ObjectName))
      return null;
    
    context.setPropertyResolved(true);
    
    ObjectName objectName = (ObjectName) base;
    
    Object value = null;
    
    try {
      value = _mBeanServer.getAttribute(objectName, String.valueOf(property));
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    return value;
  }

  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
  {
    if (base instanceof ObjectName)
      context.setPropertyResolved(true);

    return true;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base instanceof ObjectName)
      context.setPropertyResolved(true);

    throw new PropertyNotWritableException(String.valueOf(base));
  }
}
