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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.Optional;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

abstract public class DOMNode {

  abstract Node getNode();

  // Needed for env.wrapJava();
  abstract Env getEnv();

  public String getNodeName()
  {
    return getNode().getNodeName();
  }

  // Double check this behavior
  public String getNodeValue()
  {
    return getNode().getNodeValue();
  }

  // XXX: do setNodeValue(String value)
  public void setNodeValue(String value)
  {
    
  }
  
  public int getNodeType()
  {
    int type = getNode().getNodeType();

    switch (type) {
      case 1:
        return QuercusDOMModule.XML_ELEMENT_NODE;
      case 2:
        return QuercusDOMModule.XML_ATTRIBUTE_NODE;
      case 3:
        return QuercusDOMModule.XML_TEXT_NODE;
      case 4:
        return QuercusDOMModule.XML_CDATA_SECTION_NODE;
      case 5:
        return QuercusDOMModule.XML_ENTITY_REF_NODE;
      case 6:
        return QuercusDOMModule.XML_ENTITY_NODE;
      case 7:
        return QuercusDOMModule.XML_PI_NODE;
      case 8:
        return QuercusDOMModule.XML_COMMENT_NODE;
      case 9:
        return QuercusDOMModule.XML_DOCUMENT_NODE;
      case 10:
        return QuercusDOMModule.XML_DOCUMENT_TYPE_NODE;
      case 11:
        return QuercusDOMModule.XML_DOCUMENT_FRAG_NODE;
      case 12:
        return QuercusDOMModule.XML_NOTATION_NODE;
      default:
        return type;
     }
  }

  public DOMNode getParentNode()
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().getParentNode());
  }

  public ArrayValue getChildNodes()
  {
    NodeList childList = getNode().getChildNodes();
    int length = childList.getLength();

    ArrayValueImpl result = new ArrayValueImpl();
    Env env = getEnv();

    for (int i = 0; i < length; i++) {
      Node child = childList.item(i);

      result.put(getEnv().wrapJava(DOMNodeUtil.createDOMNode(env, child)));
    }

    return result;
  }

  public DOMNode getFirstChild()
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().getFirstChild());
  }

  public DOMNode getLastChild()
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().getLastChild());
  }

  public DOMNode getPreviousSibling()
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().getPreviousSibling());
  }

  public DOMNode getNextSibling()
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().getNextSibling());
  }

  public DOMNamedNodeMap getAttributes()
  {
    NamedNodeMap attributes = getNode().getAttributes();
    int length = attributes.getLength();
    
    DOMNamedNodeMap result = new DOMNamedNodeMap(getEnv(), attributes);
    
    for (int i = 0; i < length; i++) {
      result.put(getEnv().wrapJava(DOMNodeUtil.createDOMNode(getEnv(), attributes.item(i))));
    }
    
    return result;
  }

  public DOMDocument getOwnerDocument()
  {
    return new DOMDocument(getEnv(), getNode().getOwnerDocument());
  }

  public String getNamespaceURI()
  {
    return getNode().getNamespaceURI();
  }

  public String getPrefix()
  {
    return getNode().getPrefix();
  }

  public void setPrefix(String prefix)
  {
    getNode().setPrefix(prefix);
  }

  public String getLocalName()
  {
    return getNode().getLocalName();
  }

  public String getBaseURI()
  {
    return getNode().getBaseURI();
  }

  public String getTextContent()
  {
    return getNode().getTextContent();
  }

  public void setTextContent(String textContent)
  {
    getNode().setTextContent(textContent);
  }

  public void appendChild(DOMNode newChild)
  {
    getNode().appendChild(newChild.getNode());
  }

  public DOMNode cloneNode(@Optional("false") boolean deep)
  {
    return DOMNodeUtil.createDOMNode(getEnv(), getNode().cloneNode(deep));
  }

  public boolean hasAttributes()
  {
    return getNode().hasAttributes();
  }

  public boolean hasChildNodes()
  {
    return getNode().hasChildNodes();
  }

  public void insertBefore(DOMNode newNode,
                           @Optional DOMNode refNode)
  {
    Node newChild = newNode.getNode();
    Node refChild;

    if (refNode != null)
      refChild = refNode.getNode();
    else
      refChild = null;

    getNode().insertBefore(newChild, refChild);
  }

  public boolean isSameNode(DOMNode other)
  {
    return getNode().isSameNode(other.getNode());
  }

  public boolean isSupported(String feature,
                             String version)
  {
    return getNode().isSupported(feature, version);
  }

  public String lookupNamespaceURI(String prefix)
  {
    return getNode().lookupNamespaceURI(prefix);
  }

  public String lookupPrefix(String namespaceURI)
  {
    return getNode().lookupPrefix(namespaceURI);
  }

  public void normalize()
  {
    getNode().normalize();
  }

  public DOMNode removeChild(DOMNode oldChild)
  {
    getNode().removeChild(oldChild.getNode());

    return oldChild;
  }

  public DOMNode replaceChild(DOMNode newChild,
                              DOMNode oldChild)
  {
    getNode().replaceChild(newChild.getNode(), oldChild.getNode());

    return oldChild;
  }
}
