/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

@SuppressWarnings("serial")
public class JmxListQuery implements java.io.Serializable
{
  private boolean _isPrintAttributes;
  private boolean _isPrintValues;
  private boolean _isPrintOperations;
  private boolean _isAllBeans;
  private boolean _isPlatform;
  private String _pattern;

  public JmxListQuery()
  {
  }

  public JmxListQuery(String pattern,
                      boolean isPrintAttrs,
                      boolean isPrintValues,
                      boolean isPrintOps,
                      boolean allBeans,
                      boolean platform)
  {
    _pattern = pattern;
    _isPrintAttributes = isPrintAttrs;
    _isPrintValues = isPrintValues;
    _isPrintOperations = isPrintOps;
    _isAllBeans = allBeans;
    _isPlatform = platform;
  }

  public boolean isPrintAttributes()
  {
    return _isPrintAttributes;
  }

  public void setPrintAttributes(boolean printAttributes)
  {
    _isPrintAttributes = printAttributes;
  }

  public boolean isPrintValues()
  {
    return _isPrintValues;
  }

  public void setPrintValues(boolean printValues)
  {
    _isPrintValues = printValues;
  }

  public boolean isPrintOperations()
  {
    return _isPrintOperations;
  }

  public void setPrintOperations(boolean printOperations)
  {
    _isPrintOperations = printOperations;
  }

  public boolean isAllBeans()
  {
    return _isAllBeans;
  }

  public void setAllBeans(boolean allBeans)
  {
    _isAllBeans = allBeans;
  }

  public boolean isPlatform()
  {
    return _isPlatform;
  }

  public void setPlatform(boolean platform)
  {
    _isPlatform = platform;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
