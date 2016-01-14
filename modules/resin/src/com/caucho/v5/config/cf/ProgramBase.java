/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.cf;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.util.L10N;

/**
 * Program to assign parameters.
 */
abstract public class ProgramBase extends ConfigProgram
{
  private static final L10N L = new L10N(ProgramBase.class);
  
  private String _location;
  
  protected ProgramBase(ConfigContext config)
  {
    super(config);
  }
  
  public String getLocation()
  {
    return _location;
  }
  
  public void setLocation(String location)
  {
    _location = location;
  }
  
  public void setLocation(String fileName, int line)
  {
    _location = fileName + ":" + line + ": ";
  }
  
  static AttributeConfig getAttribute(NameCfg id, ConfigType<?> type)
  {
    AttributeConfig attr = type.getAttribute(id);

    if (attr != null) {
      return attr;
    }

    if (id.getLocalName().startsWith("_p")) {
      // config/1203 - rest values
      attr = type.getAttribute("_rest");
      
      if (attr != null) {
        return attr;
      }
    }
    
    attr = type.getDefaultAttribute(id);
    
    /*
    attr = type.getProgramAttribute();
    
    if (attr != null) {
      return attr;
    }
    
    attr = type.getProgramContentAttribute();

    if (attr != null) {
      return attr;
    }
    
    attr = type.getProgramBeanAttribute();
    
    if (attr != null && id.getNamespaceURI().startsWith("urn:java:")) {
      return attr;
    }
    
    attr = TypeFactory.getFactory().getEnvironmentAttribute(id);
    
    if (attr != null) {
      return attr;
    }
    */
    
    return attr;
  }
  
  protected ConfigException error(String msg, Object ...args)
  {
    String location = getLocation();
    
    if (location != null) {
      return new ConfigExceptionLocation(location, L.l(msg, args));
    }
    else {
      return new ConfigException(L.l(msg, args));
    }
  }
  
  protected RuntimeException error(RuntimeException exn)
  {
    String location = getLocation();
    
    if (location != null) {
      return ConfigExceptionLocation.wrap(location, exn);
    }
    else {
      return exn;
    }
  }
  
  protected RuntimeException error(Exception exn)
  {
    String location = getLocation();
    
    if (location != null) {
      return ConfigExceptionLocation.wrap(location, exn);
    }
    else {
      return ConfigException.wrap(exn);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

