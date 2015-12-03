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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Hide;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

import java.util.HashMap;
import java.util.Map;

/**
 * To represent "core" constants and functions.
 */
public class CoreModule extends AbstractQuercusModule
{
  public static final long PHP_INT_MAX = Long.MAX_VALUE;

  private static final HashMap<StringValue,Value> CONST_MAP
    = new HashMap<StringValue,Value>();

  /**
   * Returns the extensions loaded by the module.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "Core" };
  }

  @Hide
  @Override
  public Map<StringValue,Value> getConstMap()
  {
    return CONST_MAP;
  }

  static {
    CONST_MAP.put(new StringBuilderValue("TRUE"), BooleanValue.TRUE);
    CONST_MAP.put(new StringBuilderValue("FALSE"), BooleanValue.FALSE);
    CONST_MAP.put(new StringBuilderValue("NULL"), NullValue.NULL);
  }
}
