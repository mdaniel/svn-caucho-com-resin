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

public class ValueBootOption extends AbstractBootOption {
  private String _value;
  
  public ValueBootOption(String name,
                         String value,
                         String description)
  {
    this(name, value, description, false);
  }

  public ValueBootOption(String name,
                         String value,
                         String description,
                         boolean deprecated)
  {
    super(name, description, deprecated);
    
    _value = value;
  }
  
  @Override
  public String getValueName()
  {
    return _value;
  }
  
  @Override
  public boolean isValue()
  {
    return true;
  }
}
