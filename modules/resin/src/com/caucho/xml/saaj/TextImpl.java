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

package com.caucho.xml.saaj;

import javax.xml.namespace.*;
import javax.xml.soap.*;
import java.util.*;

public class TextImpl extends SOAPNodeImpl
                      implements Text
{
  private static final NameImpl TEXT_NAME = new NameImpl("#text");
  private StringBuilder _data;

  TextImpl(SOAPFactory factory, String data)
  {
    super(factory, TEXT_NAME);

    _data = new StringBuilder(data);
  }

  public boolean isComment()
  {
    return _data.toString().startsWith("<!--");
  }

  // org.w3c.dom.CharacterData

  public void appendData(String arg)
  {
    _data.append(arg);
  }

  public void deleteData(int offset, int count)
  {
    _data.delete(offset, offset + count);
  }

  public String getData()
  {
    return _data.toString();
  }

  public int getLength()
  {
    return _data.length();
  }

  public void insertData(int offset, String arg)
  {
    _data.insert(offset, arg);
  }

  public void replaceData(int offset, int count, String arg)
  {
    _data.replace(offset, offset + count, arg);
  }

  public void setData(String data)
  {
    _data = new StringBuilder(data);
  }

  public String substringData(int offset, int count)
  {
    return _data.substring(offset, offset + count);
  }

  // org.w3c.dom.Text
  
  public String getWholeText()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isElementContentWhitespace()
  {
    throw new UnsupportedOperationException();
  }

  public Text replaceWholeText(String content)
  {
    throw new UnsupportedOperationException();
  }

  public Text splitText(int offset)
  {
    throw new UnsupportedOperationException();
  }

  // javax.xml.soap.Node
  
  public String getValue()
  {
    return getData();
  }

  public void setValue(String value)
  {
    setData(value);
  }

  public void recycleNode()
  {
    // XXX
  }

  // org.w3c.dom.Node

  public short getNodeType()
  {
    return TEXT_NODE;
  }

  public String getNodeValue()
  {
    return getValue();
  }

  public String getNodeName()
  {
    return "#text";
  }
}
