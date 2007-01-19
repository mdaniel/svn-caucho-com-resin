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
* @author Emil Ong
*/

package com.caucho.xml.saaj;

import javax.xml.namespace.QName;
import javax.xml.soap.*;

import org.w3c.dom.*;

public class NameImpl extends QName implements Name {
  private String _qualifiedName;

  NameImpl(String localPart)
  {
    super(localPart);

    _qualifiedName = localPart;
  }

  NameImpl(String namespaceURI, String localPart) 
  {
    super(namespaceURI, localPart);

    _qualifiedName = localPart;
  }

  NameImpl(String namespaceURI, String localPart, String prefix) 
  {
    super(namespaceURI, localPart, prefix);

    _qualifiedName = prefix + ':' + localPart;
  }

  static NameImpl fromQualifiedName(String uri, String qualifiedName)
  {
    int colon = qualifiedName.indexOf(':');

    if (colon >= 0) {
      String prefix = qualifiedName.substring(0, colon);
      String localName = qualifiedName.substring(colon + 1);

      return new NameImpl(uri, localName, prefix);
    }

    return new NameImpl(uri, qualifiedName);
  }

  static NameImpl fromName(Name name)
  {
    if (name instanceof NameImpl)
      return (NameImpl) name;

    else if (name.getPrefix() != null)
      return new NameImpl(name.getURI(),
                          name.getLocalName(),
                          name.getPrefix());
    else if (name.getURI() != null)
      return new NameImpl(name.getURI(),
                          name.getLocalName());
    else
      return new NameImpl(name.getLocalName());
  }

  static NameImpl fromQName(QName qname)
  {
    if (qname instanceof NameImpl)
      return (NameImpl) qname;

    else if (qname.getPrefix() != null)
      return new NameImpl(qname.getNamespaceURI(),
                          qname.getLocalPart(),
                          qname.getPrefix());
    else if (qname.getNamespaceURI() != null)
      return new NameImpl(qname.getNamespaceURI(),
                          qname.getLocalPart());
    else
      return new NameImpl(qname.getLocalPart());
  }

  static NameImpl fromElement(org.w3c.dom.Element element)
  {
    return fromNode(element);
  }
    
  static NameImpl fromNode(org.w3c.dom.Node node)
  {
    if (node.getPrefix() != null) 
      return new NameImpl(node.getNamespaceURI(),
                          node.getLocalName(), 
                          node.getPrefix());
    else if (node.getNamespaceURI() != null)
      return new NameImpl(node.getNamespaceURI(),
                          node.getLocalName());
    else if (node.getLocalName() != null)
      return new NameImpl(node.getLocalName());
    else 
      return new NameImpl(node.getNodeName());
  }

  public String getLocalName()
  {
    return getLocalPart();
  }

  public String getQualifiedName()
  {
    return _qualifiedName;
  }

  public String getURI()
  {
    return getNamespaceURI();
  }

  public String toString()
  {
    return getQualifiedName();
  }

  static QName toQName(Name name)
  {
    if (name instanceof QName)
      return (QName) name;

    else if (name.getPrefix() != null)
      return new QName(name.getURI(), name.getLocalName(), name.getPrefix());
    else if (name.getURI() != null)
      return new QName(name.getURI(), name.getLocalName());
    else 
      return new QName(name.getLocalName());
  }
}

