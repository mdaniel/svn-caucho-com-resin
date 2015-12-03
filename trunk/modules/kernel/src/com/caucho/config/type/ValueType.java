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

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

/**
 * Represents a &lt;value> type.
 */
public class ValueType extends ConfigType
{
  private static final L10N L = new L10N(ListType.class);
  private static final Logger log
    = Logger.getLogger(ListType.class.getName());

  public ValueType()
  {
  }

  /**
   * Returns the given type.
   */
  public Class getType()
  {
    return Object.class;
  }

  /**
   * Creates a new instance
   */
  public Object create(Object parent)
  {
    return null;
  }

  /**
   * Returns the attribute based on the given name.
   */
  public Attribute getAttribute(QName name)
  {
    return null;
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    return text;
  }
}
