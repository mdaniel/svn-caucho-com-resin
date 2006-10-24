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
* @author Emil Ong
*/

package com.caucho.xml.stream.events;

import java.io.*;
import java.util.*;

import javax.xml.namespace.*;

import javax.xml.stream.*;
import javax.xml.stream.events.*;

public class StartElementImpl extends XMLEventImpl implements StartElement {
  private final QName _name;
  private final HashMap<QName, Attribute> _attributes;
  private final HashMap<String, Namespace> _namespaces;
  private final NamespaceContext _namespaceContext;

  public StartElementImpl(QName name, HashMap<QName, Attribute> attributes,
                          HashMap<String, Namespace> namespaces,
                          NamespaceContext namespaceContext)
  {
    _name = name;
    _attributes = attributes;
    _namespaces = namespaces;
    _namespaceContext = namespaceContext;
  }

  public Attribute getAttributeByName(QName name)
  {
    return _attributes.get(name);
  }

  public Iterator getAttributes()
  {
    return _attributes.values().iterator();
  }

  public QName getName()
  {
    return _name;
  }

  public NamespaceContext getNamespaceContext()
  {
    return _namespaceContext;
  }

  public Iterator getNamespaces()
  {
    return _namespaces.values().iterator();
  }

  public String getNamespaceURI(String prefix)
  {
    return _namespaces.get(prefix).getNamespaceURI();
  }

  public int getEventType()
  {
    return START_ELEMENT;
  }

  public void writeAsEncodedUnicode(Writer writer) 
    throws XMLStreamException
  {
    try {
      writer.write("<" + _name + ">");

      for (Attribute attribute : _attributes.values())
        attribute.writeAsEncodedUnicode(writer);
    }
    catch (IOException e) {
      throw new XMLStreamException(e);
    }
  }

  public String toString()
  {
    return "StartElement[" + _name + "]";
  }
}

