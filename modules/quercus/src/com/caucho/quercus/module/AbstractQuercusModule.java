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

package com.caucho.quercus.module;

import java.util.Map;
import java.util.HashMap;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;

/**
 * Represents a quercus module.
 */
public class AbstractQuercusModule implements QuercusModule {
  protected static final int PHP_INI_USER = 1;
  protected static final int PHP_INI_PERDIR = 2;
  protected static final int PHP_INI_SYSTEM = 4;
  protected static final int PHP_INI_ALL = 7;

  private static final HashMap<String,Value> NULL_MAP
    = new HashMap<String,Value>();

  private static final HashMap<String,StringValue> NULL_INI_MAP
    = new HashMap<String,StringValue>();

  public Map<String,Value> getConstMap()
  {
    return NULL_MAP;
  }

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return NULL_INI_MAP;
  }

  /**
   * Returns true if the named extension is implemented by the module.
   *
   * @param name the extension name
   */
  public boolean isExtensionLoaded(String name)
  {
    return false;
  }

  /**
   * Adds a default ini.
   */
  protected static void addIni(Map<String,StringValue> map,
                               String name,
                               String value,
                               int code)
  {
    if (value != null)
      map.put(name, new StringValueImpl(value));
    else
      map.put(name, null);
  }
}

