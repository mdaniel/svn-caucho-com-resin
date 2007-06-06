/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.simplexml.node;

/**
 * Represents a SimpleXML result set from SimpleXML operations.
 * There is no parent node.
 */
public class SimpleResultSet extends SimpleElement
{
  public SimpleResultSet(String name)
  {
    super(null);
    
    setQName(name);
  }
  
  @Override
  public SimpleNode get(int index)
  {
    if (index < 0)
      return null;
    else if (index <= getElementList().size())
      return getElementList().get(index);
    else if (index <= getAttributes().size())
      return getAttributes().get(index);
    else
      return null;
  }
  
  @Override
  public String toXML()
  {
    if (getElementList().size() == 0 && getAttributes().size() == 0)
      return null;
    
    StringBuilder sb = new StringBuilder();
    
    toXMLImpl(sb);
    
    return sb.toString();
  }
  
  @Override
  protected void toXMLImpl(StringBuilder sb)
  {
    if (getElementList().size() > 0)
      getElementList().get(0).toXMLImpl(sb);
    else if (getAttributes().size() > 0) {
      getAttributes().get(0).toXMLImpl(sb);
    }
  }
}
