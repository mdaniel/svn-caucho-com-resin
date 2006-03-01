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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

public class DOMXPath{

  XPath _xpath;
  DOMDocumentValue _DOMDocument;

  public DOMXPath(DOMDocumentValue DOMDocument)
  {
    XPathFactory factory = XPathFactory.newInstance();

    _xpath = factory.newXPath();
    _DOMDocument = DOMDocument;
  }

  public Value evaluate(Value expression,
                        Value contextnode)
    throws XPathException
  {
    return new StringValue(_xpath.evaluate(expression.toString(), ((DOMNodeValue) contextnode).getNode()));
  }

  public Value query(Value expression,
                     Value contextnode)
    throws XPathException
  {
    return new DOMNodeListValue((NodeList) _xpath.evaluate(expression.toString(),((DOMNodeValue)contextnode).getNode(), XPathConstants.NODESET));
  }
  
  //@todo
  public Value registerNamespace(Value prefix,
                                 Value namespaceURI)
  {
    throw new UnsupportedOperationException();
  }
  
  public Value getField(String name)
  {
    if ("document".equals(name))
      return _DOMDocument;
    
    return NullValue.NULL;
  }
}
