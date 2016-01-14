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

package com.caucho.v5.config.types;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.util.L10N;

import java.util.HashMap;

/**
 * Configuration for the init-param pattern.
 */
public class InitParam {
  private static L10N L = new L10N(InitParam.class);

  private HashMap<String,String> _parameters = new HashMap<String,String>();

  private boolean _allowEL = true;

  private String _paramName;
  private String _paramValue;

  private String _description;

  public InitParam()
  {
  }

  public InitParam(String name, String value)
  {
    _parameters.put(name, value);
  }

  /**
   * Sets true if EL is allowed.
   */
  public void setAllowEL(boolean allowEL)
  {
    _allowEL = allowEL;
  }

  /**
   * Sets the param-name.
   */
  public void setParamName(String paramName)
  {
    _paramName = paramName;
  }

  /**
   * Sets the param-value.
   */
  public void setParamValue(RawString paramValue)
  {
    String value = paramValue.getValue().trim();
    
    if (_allowEL) {
      value = ConfigContext.evalString(value);
    }

    _paramValue = value;
  }

  /**
   * Sets the description
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Sets a misc parameter.
   */
  public void setProperty(String name, RawString rawValue)
  {
    String value = rawValue.getValue().trim();
    
    if (_allowEL) {
      value = ConfigContext.evalString(value);
    }
    
    _parameters.put(name, value);
  }

  /**
   * Returns the parameters.
   */
  public HashMap<String,String> getParameters()
  {
    if (_paramName != null)
      _parameters.put(_paramName, _paramValue);

    return _parameters;
  }

  public String toString()
  {
    return "InitParam" + getParameters();
  }
}

