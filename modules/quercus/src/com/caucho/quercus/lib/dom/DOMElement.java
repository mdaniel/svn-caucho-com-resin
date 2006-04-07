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
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Construct;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.lib.dom.DOMAttr;
import com.caucho.quercus.lib.dom.DOMDocument;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMElement extends DOMNode {

  Element _element;
  Env _env;

  public DOMElement(Env env,
                    Element element)
  {
    _env = env;
    _element = element;
  }

  @Construct
  public DOMElement(Env env,
                    String name,
                    @Optional String value,
                    @Optional String namespaceURI)
  {
    _env = env;
    Document document = DOMDocument.createDocument();
    _element = document.createElement(name);
    // XXX: deal with value and namespaceURI
  }

  public Node getNode()
  {
    return _element;
  }

  public Env getEnv()
  {
    return _env;
  }

  public DOMTypeInfo getSchemaTypeInfo()
  {
    return new DOMTypeInfo(_element.getSchemaTypeInfo());
  }

  public String getTagName()
  {
    return _element.getTagName();
  }

  public String getAttribute(String name)
  {
    return _element.getAttribute(name);
  }

  public DOMNode getAttributeNode(String name)
  {
    return DOMNodeUtil.createDOMNode(_env, _element.getAttributeNode(name));
  }

  public DOMNode getAttributeNodeNS(String namespaceURI,
                                    String localName)
  {
    return DOMNodeUtil.createDOMNode(_env, _element.getAttributeNodeNS(namespaceURI, localName));
  }

  public String getAttributeNS(String namespaceURI,
                               String localName)
  {
    return _element.getAttributeNS(namespaceURI, localName);
  }

  public Value getElementsByTagName(String name)
  {
    NodeList elements = _element.getElementsByTagName(name);
    DOMNodeListValue result = new DOMNodeListValue(_env, elements);
    int length = elements.getLength();

    for (int i=0; i < length; i++) {
      result.put(_env.wrapJava(DOMNodeUtil.createDOMNode(_env, elements.item(i))));
    }

    return result;
  }

  public DOMNodeListValue getElementsByTagNameNS(String namespaceURI,
                                                 String localName)
  {
    NodeList elements = _element.getElementsByTagNameNS(namespaceURI, localName);
    DOMNodeListValue result = new DOMNodeListValue(_env, elements);
    int length = elements.getLength();

    for (int i=0; i < length; i++) {
      result.put(_env.wrapJava(DOMNodeUtil.createDOMNode(_env, elements.item(i))));
    }

    return result;
  }

  public boolean hasAttribute(String name)
  {
    return _element.hasAttribute(name);
  }

  public boolean hasAttributeNS(String namespaceURI,
                                String localName)
  {
    return _element.hasAttributeNS(namespaceURI, localName);
  }

  public boolean removeAttribute(String name)
  {
    try {
      _element.removeAttribute(name);
      return true;
    } catch (DOMException e) {
      return false;
    }
  }

  public boolean removeAttributeNode(DOMAttr oldAttr)
  {
    try {
      _element.removeAttributeNode(oldAttr.getNode());
      return true;
    } catch (DOMException e) {
      return false;
    }
  }

  public boolean removeAttributeNS(String namespaceURI,
                                   String localName)
  {
    try {
      _element.removeAttributeNS(namespaceURI, localName);
      return true;
    } catch (DOMException e) {
      return false;
    }
  }

  public boolean setAttribute(String name,
                              String value)
  {
    try {
      _element.setAttribute(name, value);
      return true;
    } catch (DOMException e) {
      return false;
    }
  }

  public DOMNode setAttributeNode(DOMAttr newAttr)
  {
    return DOMNodeUtil.createDOMNode(_env, _element.setAttributeNode(newAttr.getNode()));
  }

  public DOMNode setAttributeNodeNS(DOMAttr newAttr)
  {
    return DOMNodeUtil.createDOMNode(_env, _element.setAttributeNodeNS(newAttr.getNode()));
  }

  public void setAttributeNS(String namespaceURI,
                             String qualifiedName,
                             String value)
  {
    _element.setAttributeNS(namespaceURI, qualifiedName, value);
  }
}
