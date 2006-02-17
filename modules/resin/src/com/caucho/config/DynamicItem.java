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

package com.caucho.config;

import java.util.*;
import java.util.logging.*;

import com.caucho.util.L10N;

import com.caucho.log.Log;

/**
 * A dynamic item represents a tag value which is determined at runtime.
 */
public class DynamicItem {
  static final Logger log = Log.open(DynamicItem.class);
  static final L10N L = new L10N(DynamicItem.class);

  private String _tag;
  private Class _beanClass;
  private String _methodName;

  /**
   * Zero-arg constructor.
   */
  public DynamicItem()
  {
  }

  /**
   * Creates a new dynamic item.
   *
   * @param tag the configuration tag name.
   * @param cl the bean class to be instantiated
   * @param methodName the container bean's method name.
   */
  public DynamicItem(String tag, Class beanClass, String methodName)
  {
    _tag = tag;
    _beanClass = beanClass;
    _methodName = methodName;
  }

  /**
   * Returns the tag name for the dynamic item.
   *
   * (XXX: namespace?  s/b QName?)
   */
  public String getTag()
  {
    return _tag;
  }

  /**
   * Sets the tag name for the dynamic item.
   */
  public void setTag(String tag)
  {
    _tag = tag;
  }

  /**
   * Returns the tag's bean class.
   */
  public Class getBeanClass()
  {
    return _beanClass;
  }

  /**
   * Sets the tag's bean class.
   */
  public void setBeanClass(Class beanClass)
  {
    _beanClass = beanClass;
  }

  /**
   * Returns the container's set method name.
   */
  public String getMethodName()
  {
    return _methodName;
  }

  /**
   * Sets the container's set method name.
   */
  public void setMethodName(String methodName)
  {
    _methodName = methodName;
  }

  public String toString()
  {
    return ("DynamicItem[tag=" + _tag + ",class=" + _beanClass.getName() +
            ",method=" + _methodName + "]");
  }
}

