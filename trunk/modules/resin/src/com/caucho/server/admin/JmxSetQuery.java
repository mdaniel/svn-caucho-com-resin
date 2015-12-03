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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

@SuppressWarnings("serial")
public class JmxSetQuery implements java.io.Serializable
{
  private String _pattern;
  private String _attribute;
  private String _value;

  public JmxSetQuery()
  {
  }

  public JmxSetQuery(String pattern, String attribute, String value)
  {
    _pattern = pattern;
    _attribute = attribute;
    _value = value;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  public void setAttribute(String attribute)
  {
    _attribute = attribute;
  }

  public String getPattern()
  {
    return _pattern;
  }

  public void setPattern(String pattern)
  {
    _pattern = pattern;
  }

  public String getValue()
  {
    return _value;
  }

  public void setValue(String value)
  {
    _value = value;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
