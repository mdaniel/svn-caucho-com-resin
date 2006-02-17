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

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.Optional;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class DOMNodeValue extends Value {

  private Node _node;

  public DOMNodeValue() {}

  public DOMNodeValue(Node node)
  {
    _node = node;
  }
  
  @Override
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
    if (_node == null)
      return NullValue.NULL;
    
    _node.setNodeValue(value.toString());
    return value;
  }
  
  private Value setPrefix(Value value)
  {
    if (_node == null)
      return NullValue.NULL;
    
    _node.setPrefix(value.toString());
    return value;
  }
  
  private Value setTextContent(Value value)
  {
    if (_node == null)
      return NullValue.NULL;
    
    _node.setTextContent(value.toString());
    return value;
  }
  
  //Used if user trys to set a read-only property
  //in putField
  private Value errorReadOnly(String key)
  {
    return NullValue.NULL;
  }
  
  @Override
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
  
  @Override
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("hasAttributes".equals(methodName))
      return hasAttributes();
    else if ("hasChildNodes".equals(methodName))
      return hasChildNodes();
    else if ("normalize".equals(methodName))
      return normalize();
    
    return super.evalMethod(env, methodName);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("appendChild".equals(methodName))
      return appendChild(a0);
    else if ("cloneNode".equals(methodName))
      return cloneNode(a0);
    else if ("isSameNode".equals(methodName))
      return isSameNode(a0);
    else if ("lookupNamespaceURI".equals(methodName))
      return lookupNamespaceURI(a0);
    else if ("lookupPrefix".equals(methodName))
      return lookupPrefix(a0);
    else if ("removeChild".equals(methodName))
      return removeChild(a0);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    if ("insertBefore".equals(methodName))
      return insertBefore(a0, a1);
    else if ("isSupported".equals(methodName))
      return isSupported(a0, a1);
    else if ("replaceChild".equals(methodName))
      return replaceChild(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
  
  public Value appendChild(Value newNode)
  {
    
    if (!(newNode instanceof DOMNodeValue)) {
      return NullValue.NULL;
    }
    
    try {
      _node.appendChild(((DOMNodeValue)newNode).getNode());
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

  public Value insertBefore(Value newNode,
                            @Optional Value refNode)
  {
    Value result = NullValue.NULL;
    
    if ((_node == null) || (!(newNode instanceof DOMNodeValue)))
      return result;
    
    if ((refNode != null) && (!(refNode instanceof DOMNodeValue)))
      return result;
    
    try {
      if (refNode != null)
        result = new DOMNodeValue(_node.insertBefore(((DOMNodeValue) newNode).getNode(), ((DOMNodeValue)refNode).getNode()));
      else
        result = new DOMNodeValue(_node.insertBefore(((DOMNodeValue) newNode).getNode(),null));
    } catch (DOMException e) {
      //XXX: finish exception handling
      switch (e.code) {
        case DOMException.HIERARCHY_REQUEST_ERR:
          break;
        case DOMException.WRONG_DOCUMENT_ERR:
          break;
        case DOMException.NO_MODIFICATION_ALLOWED_ERR:
          break;
        case DOMException.NOT_FOUND_ERR:
          break;
        default:
          break;
      }
    }
    
    return result;
  }
  
  public Value removeChild(Value oldNode)
  {
    Value result = BooleanValue.FALSE;
    
    if ((_node == null) || (!(oldNode instanceof DOMNodeValue)))
      return result;
    
    try {
      result = new DOMNodeValue(_node.removeChild(((DOMNodeValue) oldNode).getNode()));
    } catch (DOMException e) {
      //XXX: finish exception handling
      switch (e.code) {
        case DOMException.NO_MODIFICATION_ALLOWED_ERR:
          break;
        case DOMException.NOT_FOUND_ERR:
          break;
        default:
          break;
      }
    }
    return result;
  }
  
  public Value replaceChild(Value newNode,
                            Value oldNode)
  {
    Value result = BooleanValue.FALSE;
    
    if ((_node == null) || (!(newNode instanceof DOMNodeValue)) || (!(oldNode instanceof DOMNodeValue)))
      return result;
    
    try {
      result = new DOMNodeValue(_node.replaceChild(((DOMNodeValue)newNode).getNode(), ((DOMNodeValue)oldNode).getNode()));
    } catch (DOMException e) {
      //XXX: finish exception handling
      switch (e.code) {
        case DOMException.NO_MODIFICATION_ALLOWED_ERR:
          break;
        case DOMException.HIERARCHY_REQUEST_ERR:
          break;
        case DOMException.WRONG_DOCUMENT_ERR:
          break;
        case DOMException.NOT_FOUND_ERR:
          break;
        default:
          break;
      }
      
      result = BooleanValue.FALSE;
    }
    
    return result;
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
  
  public Value cloneNode(@Optional("false") Value deep)
  {
    if ((_node == null) || (!(deep instanceof BooleanValue)))
      return NullValue.NULL;
    
    return new DOMNodeValue(_node.cloneNode(deep.toBoolean()));
  }
  
  public Value hasAttributes()
  {
    if (_node == null)
       return BooleanValue.FALSE;
    
    if (_node.hasAttributes())
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value hasChildNodes()
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    if (_node.hasChildNodes())
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value isSameNode(Value node)
  {
    if ((_node == null) || (!(node instanceof DOMNodeValue)))
      return BooleanValue.FALSE;
    
    if (_node.isSameNode(((DOMNodeValue)node).getNode()))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value isSupported(Value feature,
                           Value version)
  {
    if (_node == null)
      return BooleanValue.FALSE;
    
    if (_node.isSupported(feature.toString(), version.toString()))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }
  
  public Value lookupNamespaceURI(Value prefix)
  {
    if ((_node == null) || (!(prefix instanceof StringValue)))
      return NullValue.NULL;
    
    return new StringValue (_node.lookupNamespaceURI(prefix.toString()));
  }
  
  public Value lookupPrefix(Value namespaceURI)
  {
    if ((_node == null) ||(!(namespaceURI instanceof StringValue)))
      return NullValue.NULL;
    
    return new StringValue(_node.lookupPrefix(namespaceURI.toString()));
  }
  
  public Value normalize()
  {
    if (_node != null)
      _node.normalize();
    
    return NullValue.NULL;
  }
}
