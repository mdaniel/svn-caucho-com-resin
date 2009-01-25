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
 * @author Scott Ferguson
 */

package com.caucho.config.cfg;

/**
 * Configuration for the xml component type.
 */
public class WbComponentType {
  private Class _type;
  private int _priority = -1;

  public WbComponentType(Class type)
  {
    _type = type;

    if (type == null)
      throw new NullPointerException();
  }

  public void setType(Class type)
  {
    _type = type;
  }

  public Class getType()
  {
    return _type;
  }

  public void setPriority(int priority)
  {
    _priority = priority;
  }

  public int getPriority()
  {
    return _priority;
  }

  public boolean isEnabled()
  {
    return _priority >= 0;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[@" + _type.getSimpleName() + "]";
  }
}
