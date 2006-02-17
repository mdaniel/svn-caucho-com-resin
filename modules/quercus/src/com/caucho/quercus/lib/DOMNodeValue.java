/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Optional;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class DOMNodeValue extends Value{

  public String nodeName;
  public String nodeValue;
  public int nodeType;
  public DOMNodeValue parentNode;
  public DOMNodeListValue childNodes;
  public DOMNodeValue firstChild;
  public DOMNodeValue lastChild;
  public DOMNodeValue previousSibling;
  public DOMNodeValue nextSibling;
  public DOMNamedNodeMapValue attributes;
  public DOMDocumentValue ownerDocument;
  public String namespaceURI;
  public String prefix;
  public String localName;
  public String baseURI;
  public String textContent;

  private Node _node;

  public DOMNodeValue() {}

  public DOMNodeValue(Node node)
  {
    _node = node;
  }
  
  public Value putField(String key, Value value)
  {
    if ("nodeName".equals(key))
      return errorReadOnly(key);
    else if ("nodeValue".equals(key))
      return setNodeValue(value);
    else if ("nodeType".equals(key))
      return errorReadOnly(key);
    else if ("parentNode".equals(key))
      return errorReadOnly(key);
    else if ("childNodes".equals(key))
      return errorReadOnly(key);
    else if ("firstChild".equals(key))
      return errorReadOnly(key);
    else if ("lastChild".equals(key))
      return errorReadOnly(key);
    else if ("previousSibling".equals(key))
      return errorReadOnly(key);
    else if ("nextSibling".equals(key))
      return errorReadOnly(key);
    else if ("attributes".equals(key))
      return errorReadOnly(key);
    else if ("ownerDocument".equals(key))
      return errorReadOnly(key);
    else if ("namespaceURI".equals(key))
      return errorReadOnly(key);
    else if ("prefix".equals(key))
      return setPrefix(value);
    else if ("localName".equals(key))
      return errorReadOnly(key);
    else if ("baseURI".equals(key))
      return errorReadOnly(key);
    else if ("textContent".equals(key))
      return setTextContent(value);
    else
      return NullValue.NULL;
  }
  
  private Value setNodeValue(Value value)
  {
    _node.setNodeValue(value.toString());
    return value;
  }
  
  private Value setPrefix(Value value)
  {
    _node.setPrefix(value.toString());
    return value;
  }
  
  private Value setTextContent(Value value)
  {
    _node.setTextContent(value.toString());
    return value;
  }
  
  //Used if user trys to set a read-only property
  //in putField
  private Value errorReadOnly(String key)
  {
    return NullValue.NULL;
  }
  
  public Value getField(String name)
  {
    if ("nodeName".equals(name))
      return new StringValue(_node.getNodeName());
    else if ("nodeValue".equals(name))
      return new StringValue(_node.getNodeValue());
    else if ("nodeType".equals(name))
      return new LongValue(getNodeType());
    else if ("parentNode".equals(name))
      return new DOMNodeValue(_node.getParentNode());
    else if ("childNodes".equals(name))
      return new DOMNodeListValue(_node.getChildNodes());
    else if ("firstChild".equals(name))
      return new DOMNodeValue(_node.getFirstChild());
    else if ("lastChild".equals(name))
      return new DOMNodeValue(_node.getLastChild());
    else if ("previousSibling".equals(name))
      return new DOMNodeValue(_node.getPreviousSibling());
    else if ("nextSibling".equals(name))
      return new DOMNodeValue(_node.getNextSibling());
    else if ("attributes".equals(name))
      return new DOMNamedNodeMapValue(_node.getAttributes());
    else if ("ownerDocument".equals(name))
      return new DOMDocumentValue(_node.getOwnerDocument());
    else if ("namespaceURI".equals(name))
      return new StringValue(_node.getNamespaceURI());
    else if ("prefix".equals(name))
      return new StringValue(_node.getPrefix());
    else if ("localName".equals(name))
      return new StringValue(_node.getLocalName());
    else if ("baseURI".equals(name))
      return new StringValue(_node.getBaseURI());
    else if ("textContent".equals(name))
      return new StringValue(_node.getTextContent());
    else
      return NullValue.NULL;
  }
  
  public DOMNodeValue appendChild(DOMNodeValue newNode)
  {
    
    try {
      _node.appendChild(newNode.getNode());
    } catch (DOMException e) {
      //XXX: finish exception handling
      switch (e.code) {
        case DOMException.NO_MODIFICATION_ALLOWED_ERR:
          break;
        case DOMException.HIERARCHY_REQUEST_ERR:
          break;
        case DOMException.WRONG_DOCUMENT_ERR:
          break;
        default:
          break;
      }
    }
    
    return this;
  }

  public Node getNode()
  {
    return _node;
  }

  public int getNodeType()
  {
    int type = _node.getNodeType();
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
  
  public DOMNodeValue cloneNode(@Optional("false") boolean deep)
  {
    return new DOMNodeValue(_node.cloneNode(deep));
  }
  
  //@todo hasAttributes()
  //@todo hasChildNodes()
  //@todo insertBefore()
  //@todo isSameNode()
  //@todo isSupported()
  //@todo lookupNamespaceURI()
  //@todo lookupPrefix()
  //@todo normalize()
  //@todo removeChild()
  //@todo replaceChild()
}
