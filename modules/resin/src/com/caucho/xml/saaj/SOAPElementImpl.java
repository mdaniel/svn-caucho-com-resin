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

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.*;
import javax.xml.soap.*;

import org.w3c.dom.*;

public class SOAPElementImpl extends SOAPNodeImpl implements SOAPElement 
{
  private static final Name ENCODING_STYLE_NAME
    = new NameImpl(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,
                   "encodingStyle",
                   SOAPConstants.SOAP_ENV_PREFIX);

  protected SOAPAttrImpl _firstAttribute;
  protected SOAPAttrImpl _lastAttribute;

  private String _encoding;
  private HashMap<String,String> _namespaces = new HashMap<String,String>();

  SOAPElementImpl(SOAPFactory factory, NameImpl name, SOAPPart owner)
    throws SOAPException
  {
    super(factory, name, owner);

    if (name.getPrefix() != null)
      _namespaces.put(name.getPrefix(), name.getURI());
  }

  SOAPElementImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    this(factory, name, null);
  }

  SOAPElementImpl(SOAPFactory factory, Element element)
    throws SOAPException
  {
    this(factory, NameImpl.fromElement(element));

    copyElement(element, true);
  }

  protected void setOwner(SOAPPart owner)
  {
    _owner = owner;
  }

  protected void copySOAPElement(SOAPElement source)
  {
    try {
      setEncodingStyle(source.getEncodingStyle());
    }
    catch (SOAPException e) {
      // ignore exception... if encoding style applies, it will be set
      // and not if not
    }

    if (source instanceof SOAPElementImpl) {
      SOAPElementImpl sourceImpl = (SOAPElementImpl) source;

      _namespaces = (HashMap<String,String>) sourceImpl._namespaces.clone();
      _factory = sourceImpl._factory;

      copyElement(sourceImpl, false);
    }
    else
      copyElement(source, true);
  }

  // do a shallow copy
  protected void copyElement(Element source, boolean addNamespaces)
  {
    _name = NameImpl.fromElement(source);

    for (org.w3c.dom.Node node = source.getFirstChild(); 
         node != null; 
         node = node.getNextSibling())
      appendChild(node);

    if (source instanceof SOAPElementImpl) {
      // more efficient when we have an element
      for (Attr attr = ((SOAPElementImpl) source)._firstAttribute;
           attr != null; 
           attr = (Attr) attr.getNextSibling()) {

        // Check if this is a namespace declaration.
        if (addNamespaces &&
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()))
          _namespaces.put(attr.getLocalName(), attr.getValue());

        setAttributeNode(attr);
      }
    }
    else {
      NamedNodeMap attributes = source.getAttributes();

      for (int i = 0; i < attributes.getLength(); i++) {
        Attr attr = (Attr) attributes.item(i);
        // Check if this is a namespace declaration.
        if (addNamespaces &&
            XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()))
          _namespaces.put(attr.getLocalName(), attr.getValue());

        setAttributeNode(attr);
      }
    }
    
    // don't need to set parent because this element should be added 
    // as a child to another element.  Ditto for siblings.
  }
  
  // javax.xml.soap.Element
  
  public SOAPElement addAttribute(Name name, String value) 
    throws SOAPException
  {
    setAttribute(NameImpl.fromName(name), value);

    return this;
  }

  public SOAPElement addAttribute(QName qname, String value) 
    throws SOAPException
  {
    setAttribute(NameImpl.fromQName(qname), value);

    return this;
  }

  public SOAPElement addChildElement(Name name) 
    throws SOAPException
  {
    SOAPElement child = _factory.createElement(name);

    appendChild(child);

    return child;
  }

  public SOAPElement addChildElement(QName qname) 
    throws SOAPException
  {
    return addChildElement((Name) NameImpl.fromQName(qname));
  }

  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException
  {
    // to be overriden in certain elements (Header, Fault, Body)
    appendChild(element);

    return element;
  }

  public SOAPElement addChildElement(String localName) 
    throws SOAPException
  {
    return addChildElement((Name) new NameImpl(localName));
  }

  public SOAPElement addChildElement(String localName, String prefix) 
    throws SOAPException
  {
    String uri = getNamespaceURI(prefix);

    if (uri == null)
      throw new SOAPException("Undefined prefix: " + prefix);

    return addChildElement(localName, prefix, uri);
  }

  public SOAPElement addChildElement(String localName, 
                                     String prefix, 
                                     String uri) 
    throws SOAPException
  {
    return addChildElement((Name) new NameImpl(uri, localName, prefix));
  }

  public SOAPElement addNamespaceDeclaration(String prefix, String uri) 
    throws SOAPException
  {
    _namespaces.put(prefix, uri);

    setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                   XMLConstants.XMLNS_ATTRIBUTE + ':' + prefix,
                   uri);

    return this;
  }

  public boolean removeNamespaceDeclaration(String prefix)
  {
    if (_namespaces.remove(prefix) != null) {
      removeAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                        XMLConstants.XMLNS_ATTRIBUTE + ':' + prefix);
      return true;
    }

    return false;
  }
  
  public SOAPElement addTextNode(String text) 
    throws SOAPException
  {
    appendChild(new TextImpl(_factory, text));

    return this;
  }

  public QName createQName(String localName, String prefix) 
    throws SOAPException
  {
    String uri = getNamespaceURI(prefix);

    if (uri == null)
      throw new SOAPException("Undefined prefix: " + prefix);

    return new NameImpl(uri, localName, prefix);
  }

  public Iterator getAllAttributes()
  {
    return new AttributeIterator(_firstAttribute);
  }

  public Iterator getAllAttributesAsQNames()
  {
    return new AttributeIterator(_firstAttribute);
  }
  
  public String getAttributeValue(Name name)
  {
    return getAttributeValue(NameImpl.toQName(name));
  }

  public String getAttributeValue(QName qname)
  {
    // DOM specifies that the default is "", while SAAJ says null
   
    Attr attr = null;

    if (qname.getNamespaceURI() != null)
      attr = getAttributeNodeNS(qname.getNamespaceURI(), qname.getLocalPart());
    else
      attr = getAttributeNode(qname.getLocalPart());

    if (attr == null)
      return null;

    return attr.getValue();
  }

  public Iterator getChildElements()
  {
    return new NodeIterator();
  }

  public Iterator getChildElements(Name name)
  {
    return getChildElements(NameImpl.toQName(name));
  }

  public Iterator getChildElements(QName qname)
  {
    return new NodeIterator(qname);
  }

  public Name getElementName()
  {
    return _name;
  }

  public QName getElementQName()
  {
    return _name;
  }
  
  public String getEncodingStyle()
    throws SOAPException
  {
    return getAttributeValue(ENCODING_STYLE_NAME);
  }

  public void setEncodingStyle(String encodingStyle) 
    throws SOAPException
  {
    if (! SOAPConstants.URI_NS_SOAP_ENCODING.equals(encodingStyle) &&
        ! SOAPConstants.URI_NS_SOAP_1_2_ENCODING.equals(encodingStyle))
      throw new IllegalArgumentException("Unknown SOAP Encoding: " + encodingStyle);

    addAttribute(ENCODING_STYLE_NAME, encodingStyle);
  }

  public Iterator getNamespacePrefixes()
  {
    return _namespaces.keySet().iterator();
  }

  public Iterator getVisibleNamespacePrefixes()
  {
    // XXX parent
    
    return _namespaces.keySet().iterator();
  }

  public String getNamespaceURI(String prefix)
  {
    String uri = _namespaces.get(prefix);

    if (uri == null && getParentNode() != null)
      return ((SOAPElement) getParentNode()).getNamespaceURI(prefix);
    else
      return uri;
  }

  public boolean removeAttribute(Name name)
  {
    SOAPAttrImpl attr = null;

    if (name.getURI() != null)
      attr = (SOAPAttrImpl) getAttributeNodeNS(name.getURI(), 
                                               name.getLocalName());
    else
      attr = (SOAPAttrImpl) getAttributeNode(name.getQualifiedName());

    if (attr != null) {
      attr.detachNode();
      return true;
    }

    return false;
  }

  public boolean removeAttribute(QName qname)
  {
    return removeAttribute((Name) NameImpl.fromQName(qname));
  }

  public void removeContents()
  {
    for (SOAPNodeImpl node = _firstChild; node != null; ) {
      SOAPNodeImpl current = node;

      node = current._next;

      current._parent = null;
      current._next = null;
      current._previous = null;
    }

    _firstChild = null;
    _lastChild = null;
  }

  public SOAPElement setElementQName(QName newName) 
    throws SOAPException
  {
    _name = NameImpl.fromQName(newName);

    return this;
  }

  // org.w3c.dom.Node

  public boolean hasAttributes()
  {
    return _firstAttribute != null;
  }

  public String getNodeName()
  {
    return getTagName();
  }

  public short getNodeType()
  {
    return ELEMENT_NODE;
  }

  public String getNodeValue()
  {
    return null;
  }

  public String lookupNamespaceURI(String prefix)
  {
    return getNamespaceURI(prefix);
  }

  public NamedNodeMap getAttributes()
  {
    return new NamedNodeMapImpl(_firstAttribute);
  }

  // org.w3c.dom.Element
  
  public String getAttribute(String name)
  {
    for (Attr attr = _firstAttribute; 
         attr != null; 
         attr = (Attr) attr.getNextSibling()) {
      if (name.equals(attr.getName()))
        return attr.getValue();
    }

    return "";
  }

  public String getAttributeNS(String namespaceURI, String localName)
  {
    for (Attr attr = _firstAttribute; 
         attr != null; 
         attr = (Attr) attr.getNextSibling()) {
      if (localName.equals(attr.getLocalName()) && 
          namespaceURI.equals(attr.getNamespaceURI()))
        return attr.getValue();
    }

    return "";
  }

  public Attr getAttributeNode(String name)
  {
    for (Attr attr = _firstAttribute; 
         attr != null; 
         attr = (Attr) attr.getNextSibling()) {
      if (name.equals(attr.getName()))
        return attr;
    }

    return null;
  }

  public Attr getAttributeNodeNS(String namespaceURI, String localName)
  {
    for (Attr attr = _firstAttribute; 
         attr != null; 
         attr = (Attr) attr.getNextSibling()) {
      if (localName.equals(attr.getLocalName()) && 
          namespaceURI.equals(attr.getNamespaceURI()))
        return attr;
    }

    return null;
  }

  public NodeList getElementsByTagName(String name)
  {
    return new SelectiveNodeListImpl(name);
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
  {
    return new SelectiveNodeListImpl(namespaceURI, localName);
  }

  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  public String getTagName()
  {
    return _name.getQualifiedName();
  }

  public boolean hasAttribute(String name)
  {
    return getAttributeNode(name) != null;
  }

  public boolean hasAttributeNS(String namespaceURI, String localName)
  {
    return getAttributeNodeNS(namespaceURI, localName) != null;
  }

  public void removeAttribute(String name)
  {
    SOAPAttrImpl attr = (SOAPAttrImpl) getAttributeNode(name);

    if (attr == null)
      return;

    removeAttributeNode(attr);
  }

  public void removeAttributeNS(String namespaceURI, String localName)
  {
    SOAPAttrImpl attr = 
      (SOAPAttrImpl) getAttributeNodeNS(namespaceURI, localName);

    if (attr == null)
      return;

    removeAttributeNode(attr);
  }

  public Attr removeAttributeNode(Attr oldAttr)
  {
    SOAPAttrImpl attr = (SOAPAttrImpl) oldAttr;

    if (attr._parent == this) {
      attr._parent = null;

      if (attr._previous != null)
        attr._previous._next = attr._next;

      if (attr._next != null)
        attr._next._previous = attr._previous;
    }

    return attr;
  }

  private void appendAttribute(SOAPAttrImpl attr)
  {
    attr._parent = this;

    if (_lastAttribute != null) {
      _lastAttribute._next = attr;
      attr._previous = _lastAttribute;
      _lastAttribute = attr;
    }
    else
      _lastAttribute = _firstAttribute = attr;
  }

  private void setAttribute(NameImpl name, String value)
  {
    Attr attr = null;
    
    if (name.getURI() != null)
      attr = getAttributeNodeNS(name.getURI(), name.getLocalName());
    else
      attr = getAttributeNode(name.getLocalName());

    if (attr != null)
      attr.setValue(value);
    else
      appendAttribute(new SOAPAttrImpl(_factory, name, value));
  }

  public void setAttribute(String name, String value)
  {
    Attr attr = getAttributeNode(name);

    if (attr != null)
      attr.setValue(value);
    else
      appendAttribute(new SOAPAttrImpl(_factory, new NameImpl(name), value));
  }

  public Attr setAttributeNode(Attr newAttr)
  {
    Attr attr = getAttributeNode(newAttr.getName());

    if (attr != null) {
      attr.setValue(newAttr.getValue());
      return attr;
    }
    else {
      appendAttribute((SOAPAttrImpl) newAttr);
      return newAttr;
    }
  }

  public Attr setAttributeNodeNS(Attr newAttr)
  {
    Attr attr = getAttributeNodeNS(newAttr.getNamespaceURI(),
                                   newAttr.getName());

    if (attr != null) {
      attr.setValue(newAttr.getValue());
      return attr;
    }
    else {
      appendAttribute((SOAPAttrImpl) newAttr);
      return newAttr;
    }
  }

  public void setAttributeNS(String namespaceURI, 
                             String qualifiedName, 
                             String value)
  {
    Attr attr = getAttributeNodeNS(namespaceURI, qualifiedName);

    if (attr != null)
      attr.setValue(value);
    else {
      NameImpl name = NameImpl.fromQualifiedName(namespaceURI, qualifiedName);
      appendAttribute(new SOAPAttrImpl(_factory, name, value));
    }
  }

  public void setIdAttribute(String name, boolean isId)
  {
    SOAPAttrImpl attr = (SOAPAttrImpl) getAttributeNode(name);

    if (attr != null)
      attr.setIsId(isId);
  }

  public void setIdAttributeNode(Attr attr, boolean isId)
  {
    if (attr.getParentNode() != this)
      throw new DOMException(DOMException.NOT_FOUND_ERR, 
                             "Attribute does not belong to this element");

    ((SOAPAttrImpl) attr).setIsId(isId);
  }

  public void setIdAttributeNS(String namespaceURI, 
                               String localName, 
                               boolean isId) 
  {
    SOAPAttrImpl attr = 
      (SOAPAttrImpl) getAttributeNodeNS(namespaceURI, localName);

    if (attr != null)
      attr.setIsId(isId);
  }
  
  private static class AttributeIterator implements Iterator {
    private SOAPAttrImpl _current;

    public AttributeIterator(SOAPAttrImpl first)
    {
      _current = first;
    }

    public boolean hasNext()
    {
      return _current != null;
    }

    public Object next()
    {
      if (_current == null)
        throw new NoSuchElementException();

      Name name = _current._name;

      _current = (SOAPAttrImpl) _current.getNextSibling();

      return name;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
  
  protected static class NamedNodeMapImpl implements NamedNodeMap {
    private org.w3c.dom.Node _first;

    NamedNodeMapImpl(org.w3c.dom.Node first)
    {
      _first = first;
    }

    public int getLength()
    {
      int length = 0;

      for (org.w3c.dom.Node node = _first; 
           node != null; 
           node = node.getNextSibling())
        length++;

      return length;
    }

    public org.w3c.dom.Node getNamedItem(String name)
    {
      for (org.w3c.dom.Node node = _first; 
           node != null;
           node = node.getNextSibling()) {
        if (name.equals(node.getNodeName()))
          return node;
      }

      return null;
    }

    public org.w3c.dom.Node getNamedItemNS(String namespaceURI, 
                                           String localName)
    {
      for (org.w3c.dom.Node node = _first; 
           node != null; 
           node = node.getNextSibling()) {
        if (localName.equals(node.getLocalName()) &&
            namespaceURI.equals(node.getNamespaceURI()))
          return node;
      }

      return null;
    }

    public org.w3c.dom.Node item(int index)
    {
      int i = 0;

      for (org.w3c.dom.Node node = _first; 
           node != null; 
           node = node.getNextSibling()) {
        if (i == index)
          return node;

        i++;
      }

      return null;
    }

    public org.w3c.dom.Node removeNamedItem(String name)
    {
      SOAPNodeImpl node = (SOAPNodeImpl) getNamedItem(name);

      node._parent = null;

      if (node._previous != null)
        node._previous._next = node._next;

      if (node._next != null)
        node._next._previous = node._previous;

      node._previous = null;
      node._next = null;

      return node;
    }

    public org.w3c.dom.Node removeNamedItemNS(String namespaceURI, 
                                              String localName)
    {
      SOAPNodeImpl node = 
        (SOAPNodeImpl) getNamedItemNS(namespaceURI, localName);

      node._parent = null;

      if (node._previous != null)
        node._previous._next = node._next;

      if (node._next != null)
        node._next._previous = node._previous;

      node._previous = null;
      node._next = null;

      return node;
    }

    public org.w3c.dom.Node setNamedItem(org.w3c.dom.Node arg)
    {
      org.w3c.dom.Node node = getNamedItem(arg.getNodeName());

      if (arg.getParentNode() != null)
        return arg.getParentNode().replaceChild(arg, node);

      return null;
    }

    public org.w3c.dom.Node setNamedItemNS(org.w3c.dom.Node arg)
    {
      org.w3c.dom.Node node = getNamedItemNS(arg.getNamespaceURI(), 
                                             arg.getLocalName());

      if (arg.getParentNode() != null)
        return arg.getParentNode().replaceChild(arg, node);

      return null;
    }
  }

  protected class NodeIterator implements Iterator
  { 
    private QName _name;
    private SOAPNodeImpl _node;

    public NodeIterator(QName name)
    {
      _name = name;

      if (_name == null)
        _node = _firstChild;
      else if (_name.getNamespaceURI() == null) {
        for (_node = _firstChild; _node != null; _node = _node._next) {
          if (_name.getLocalPart().equals(_node.getLocalName()))
            break;
        }
      }
      else {
        for (_node = _firstChild; _node != null; _node = _node._next) {
          if (_name.getNamespaceURI().equals(_node.getNamespaceURI()) &&
              _name.getLocalPart().equals(_node.getLocalName()))
            break;
        }
      }
    }

    public NodeIterator()
    {
      this(null);
    }

    public boolean hasNext()
    {
      return _node != null;
    }

    public Object next()
    {
      if (_node == null)
        throw new NoSuchElementException();

      if (_name == null) {
        SOAPNodeImpl next = _node;

        _node = _node._next;

        return next;
      }
      else if (_name.getNamespaceURI() == null) {
        SOAPNodeImpl next = _node;

        for (_node = _node._next; _node != null; _node = _node._next) {
          if (_name.getLocalPart().equals(_node.getNodeName()))
            break;
        }

        return next;
      }
      else {
        SOAPNodeImpl next = _node;

        for (_node = _node._next; _node != null; _node = _node._next) {
          if (_name.getLocalPart().equals(_node.getLocalName()) &&
              _name.getNamespaceURI().equals(_node.getNamespaceURI()))
            break;
        }

        return next;
      }
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  protected class SelectiveNodeListImpl
    implements NodeList 
  { 
    private String _name;
    private String _namespace;

    public SelectiveNodeListImpl(String namespace, String name)
    {
      _namespace = namespace;
      _name = name;
    }

    public SelectiveNodeListImpl(String name)
    {
      this(null, name);
    }

    public int getLength()
    {
      int length = 0; 

      for (org.w3c.dom.Node node = _firstChild; 
           node != null; 
           node = node.getNextSibling()) {
        if (_namespace != null) {
          if (_namespace.equals(node.getNamespaceURI()) &&
              _name.equals(node.getLocalName()))
            length++;
        }
        else if (_name.equals(node.getNodeName()))
          length++;
      }

      return length;
    }

    public org.w3c.dom.Node item(int i)
    {
      int j = 0;

      for (org.w3c.dom.Node node = _firstChild; 
           node != null; 
           node = node.getNextSibling()) {
        if (i == j)
          return node;

        if (_namespace != null) {
          if (_namespace.equals(node.getNamespaceURI()) &&
              _name.equals(node.getLocalName()))
            j++;
        }
        else if (_name.equals(node.getNodeName()))
          j++;
      }

      return null;
    }
  }

}
