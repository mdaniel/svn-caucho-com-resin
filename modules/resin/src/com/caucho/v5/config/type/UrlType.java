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

package com.caucho.v5.config.type;

import java.net.URL;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

public class UrlType extends ConfigType<URL>
{
  private static final L10N L = new L10N(UrlType.class);

  public static final UrlType TYPE = new UrlType();

  private UrlType()
  {
  }
  

  /**
   * Returns the path class.
   */
  
  public Class<URL> getType()
  {
    return URL.class;
  }

  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  @Override
  public Object valueOf(String text)
  {
    try {
      PathImpl path = PathType.lookupPath(text);
    
      return new URL(path.getURL());
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  /**
   * Converts the value to a value of the type.
   */
  @Override
  public Object valueOf(Object value)
  {
    try {
      if (value instanceof URL)
        return value;
      else if (value instanceof PathImpl)
        return new URL(((PathImpl) value).getURL());
      else if (value instanceof String)
        return valueOf((String) value);
      else if (value == null)
        return null;
      else
        return valueOf(String.valueOf(value));
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}
