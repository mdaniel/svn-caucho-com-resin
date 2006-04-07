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

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringInputStream;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.lib.dom.DOMDocument;
import com.caucho.quercus.lib.dom.DOMNode;
import com.caucho.quercus.lib.simplexml.SimpleXMLElement;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

public class DOMXPath{

  private Env _env;
  private XPath _xpath;
  private DOMDocument _domDocument;

  public DOMXPath(Env env,
                  DOMDocument DOMDocument)
  {
    _env = env;

    XPathFactory factory = XPathFactory.newInstance();

    _xpath = factory.newXPath();
    _domDocument = DOMDocument;
  }

  public Value evaluate(String expression,
                        @Optional DOMNode contextnode)
    throws XPathException
  {
    SimpleXMLElement simpleXML;

    //If contextnode == null, then use root element.
    if (contextnode == null) {
      simpleXML = new SimpleXMLElement(_env, _domDocument.getNode(), _domDocument.getNode().getDocumentElement());
    } else {
      simpleXML = new SimpleXMLElement(_env, _domDocument.getNode(), (Element) contextnode.getNode());
    }

    return new StringValueImpl(_xpath.evaluate(expression, new InputSource(new StringInputStream(simpleXML.asXML().toString()))));
  }

  public Value query(String expression,
                     @Optional DOMNode contextnode)
    throws XPathException
  {
    throw new UnsupportedOperationException();
    //return new DOMNodeListValue((NodeList) _xpath.evaluate(expression.toString(),((DOMNode)contextnode).getNode(), XPathConstants.NODESET));
  }

  public Value registerNamespace(Value prefix,
                                 Value namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public DOMDocument getDocument()
  {
    return _domDocument;
  }
}
