/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.simplexml;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class SimpleNamespaceContext implements NamespaceContext
{
  private XPath _xpath;

  private HashMap<String,String> _prefixMap
    = new HashMap<String,String>();

  public SimpleNamespaceContext()
  {
    this(XPathFactory.newInstance().newXPath());
  }

  public SimpleNamespaceContext(XPath xpath)
  {
    _xpath = xpath;

    xpath.setNamespaceContext(this);
  }

  public XPath getXPath()
  {
    return _xpath;
  }

  public void addPrefix(String prefix, String namespaceURI)
  {
    _prefixMap.put(prefix, namespaceURI);
  }

  @Override
  public String getNamespaceURI(String prefix)
  {
    String uri = _prefixMap.get(prefix);

    return uri;
  }

  @Override
  public String getPrefix(String namespaceURI)
  {
    return null;
  }

  @Override
  public Iterator getPrefixes(String namespaceURI)
  {
    return null;
  }
}
