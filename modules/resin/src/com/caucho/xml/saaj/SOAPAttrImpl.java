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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.xml.saaj;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;

import java.io.IOException;

import javax.xml.soap.*;

public class SOAPAttrImpl extends SOAPNodeImpl 
                          implements Attr 
{
  private boolean _isId = false;
  private boolean _specified;
  private String _value;

  SOAPAttrImpl(SOAPFactory factory, NameImpl name)
  { 
    super(factory, name);
  }

  SOAPAttrImpl(SOAPFactory factory, NameImpl name, String value)
  { 
    super(factory, name);

    _value = value;
  }

  public String getName()
  {
    return _name.getQualifiedName();
  }

  public Element getOwnerElement()
  {
    return (Element) getParentNode();
  }

  public short getNodeType()
  {
    return ATTRIBUTE_NODE;
  }

  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  public boolean getSpecified()
  {
    return _specified;
  }

  public void setSpecified(boolean specified)
  {
    _specified = specified;
  }

  public String getValue()
  {
    return _value;
  }

  public void setValue(String value)
  {
    _value = value;
  }

  public boolean isId()
  {
    return _isId;
  }

  public void setIsId(boolean isId)
  {
    _isId = isId;
  }

  public String getNodeValue()
  {
    return _value;
  }

  public void setNodeValue(String value)
  {
    _value = value;
  }

  public String getNodeName()
  {
    return getName();
  }

  public String toString()
  {
    if (_value != null)
      return "Attr[" + _name + " " + _value + "]";
    else
      return "Attr[" + _name + "]";
  }
}
