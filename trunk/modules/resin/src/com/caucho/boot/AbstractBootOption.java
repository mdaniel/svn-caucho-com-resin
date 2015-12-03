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

package com.caucho.boot;

public abstract class AbstractBootOption implements BootOption
{
  private String _name;
  private String _description;
  private boolean _deprecated = false;
  
  public AbstractBootOption(String name, String description)
  {
    this(name, description, false);
  }

  public AbstractBootOption(String name, String description, boolean deprecated)
  {
    _name = name;
    _description = description;
    _deprecated = deprecated;
  }

  @Override
  public String getName()
  {
    return _name;
  }
  
  @Override
  public String getDescription()
  {
    return _description;
  }
  
  protected String getValueName()
  {
    return "value";
  }
  
  public boolean isDeprecated()
  {
    return _deprecated;
  }
  
  public void setDeprecated(boolean deprecated)
  {
    _deprecated = deprecated;
  }
  
  @Override
  public String getUsage()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("  --");
    sb.append(getName());
    
    if (isIntValue()) {
      sb.append(" <" + getValueName() + ">");
    } else if (isValue()) {
      sb.append(" <" + getValueName() + ">");
    }
    
    while (sb.length() < 22) {
      sb.append(" ");
    }
    
    sb.append(" : ").append(getDescription());
    
    if (isDeprecated())
      sb.append(" (deprecated)");
    
    return sb.toString();
  }
  
  @Override
  public boolean isFlag()
  {
    return false;
  }
  
  @Override
  public boolean isValue()
  {
    return false;
  }
  
  @Override
  public boolean isIntValue()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
