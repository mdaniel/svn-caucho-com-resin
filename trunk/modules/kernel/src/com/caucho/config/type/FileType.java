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
 * @author Scott Ferguson
 */

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.*;
import java.net.URL;

import javax.el.ELContext;
import javax.el.ELException;

public class FileType extends ConfigType
{
  private static final L10N L = new L10N(FileType.class);

  public static final FileType TYPE = new FileType();

  private FileType()
  {
  }
  

  /**
   * Returns the path class.
   */
  
  public Class getType()
  {
    return File.class;
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
      Path path = PathType.lookupPath(text);
    
      return new File(path.getNativePath());
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Converts the value to a value of the type.
   */
  @Override
  public Object valueOf(Object value)
  {
    try {
      if (value instanceof File)
        return value;
      else if (value instanceof Path)
        return new File(((Path) value).getNativePath());
      else if (value instanceof String)
        return valueOf((String) value);
      else if (value == null)
        return null;
      else
        return valueOf(String.valueOf(value));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
