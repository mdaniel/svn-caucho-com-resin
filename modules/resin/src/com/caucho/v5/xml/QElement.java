/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.xml;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.CharBuffer;

import org.w3c.dom.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * Resin's implementation of the DOM element.
 */
public class QElement extends QAttributedNode implements CauchoElement {
  private NameCfg _name;

  /**
   * Create a new element.
   */
  public QElement()
  {
  }

  /**
   * Create a new named element.
   *
   * @param name the element's name.
   */
  public QElement(String name)
  {
    _name = new NameCfg(name);
  }

  /**
   * Create a new named element.
   *
   * @param name the element's name.
   */
  public QElement(String name, String namespace)
  {
    _name = new NameCfg(name, namespace);
  }

  /**
   * Create a new named element.
   *
   * @param name the element's name.
   */
  public QElement(NameCfg name)
  { 
    _name = name; 
  }

  protected QElement(QDocument owner, NameCfg name)
  {
    _owner = owner;
    _name = name;
  }

  /**
   * Create a new named element with initial parameters.
   *
   * @param name the element's name.
   * @param attributes the element's attributes.
   */
  QElement(NameCfg name, HashMap attributes)
  { 
    _name = name; 
  }

  /**
   * Assign a name to the element.  Not normally called by external
   * API.
   *
   * @param name the element's name.
   */
  public void setName(NameCfg name)
  {
    _name = name;
  }

  /**
   * Returns the qname
   */
  public NameCfg getQName()
  {
    return _name;
  }

  /**
   * Returns the element's qualified-name as the node name.
   */
  public String getNodeName()
  {
    return _name.getName();
  }

  /**
   * Returns the element's qualified-name as the node name.
   */
  public String getTagName()
  {
    return _name.getName();
  }

  /**
   * Returns the local part of the element's name.
   */
  public String getLocalName()
  {
    return _name.getLocalName();
  }

  /**
   * Returns the namespace prefix for the element.
   */
  public String getPrefix()
  {
    return _name.getPrefix();
  }

  /**
   * Returns the canonical name of the element.
   */
  public String getCanonicalName()
  {
    return _name.getCanonicalName();
  }

  /**
   * Returns the namespace of the element.
   */
  public String getNamespaceURI()
  {
    return _name.getNamespace();
  }

  /**
   * Given a prefix, returns the namespace in effect at this element.
   *
   * @param prefix the prefix to test.
   * @return the namespace URL matching the prefix or null.
   */
  public String getNamespace(String prefix)
  {
    if (prefix == null)
      return getNamespace("", "xmlns");
    else
      return getNamespace(prefix, "xmlns:" + prefix);
  }

  private String getNamespace(String prefix, String xmlns)
  {
    Attr namespace = getAttributeNode(xmlns);

    if (namespace != null)
      return namespace.getNodeValue();

    if (_parent instanceof QElement)
      return ((QElement) _parent).getNamespace(prefix, xmlns);

    return _owner.getNamespace(prefix);
  }

  /**
   * Returns the DOM NodeType, ELEMENT_NODE.
   */
  public short getNodeType()
  {
    return ELEMENT_NODE;
  }

  /**
   * Returns the schema type.
   */
  public TypeInfo getSchemaTypeInfo()
  {
    return null;
  }

  /**
   * Returns a list of elements, given a tag name.
   */
  public NodeList getElementsByTagName(String tagName)
  {
    QAbstractNode child = (QAbstractNode) getFirstChild();
    
    if (child != null)
      return new QDeepNodeList(this, child, new TagPredicate(tagName));
    else
      return new QDeepNodeList(this, null, new TagPredicate(tagName));
  }
  
  /**
   * Returns a list of elements, given a namespace and a local name.
   */
  public NodeList getElementsByTagNameNS(String uri, String name)
  {
    QAbstractNode child = (QAbstractNode) getFirstChild();
    
    if (child != null)
      return new QDeepNodeList(this, child, new NSTagPredicate(uri, name));
    else
      return new QDeepNodeList(this, null, new NSTagPredicate(uri, name));
  }

  /**
   * Appends a new node as the last child of the element.
   *
   * @param child the new child.
   * @return the child.
   */
  public Node appendChild(Node child)
    throws DOMException
  {
    Node result = super.appendChild(child);

    if (child instanceof QElement) {
      QElement elt = (QElement) child;
      NameCfg name = elt._name;

      if (name.getNamespace() != "") {
        addNamespace(name);
      }

      for (QAttr attr = (QAttr) elt.getFirstAttribute();
           attr != null;
           attr = (QAttr) attr.getNextSibling()) {
        name = attr._name;

        if (name.getNamespace() != "") {
          addNamespace(name);
        }
      }
    }

    return result;
  }

  /**
   * Adds the name to the global namespace, if possible.
   */
  void addNamespace(NameCfg name)
  {
    _owner.addNamespace(name.getPrefix(), name.getNamespace());
  }

  /**
   * Normalize the element, i.e. smash all neighboring text nodes together.
   */
  public void normalize()
  {
    Node node = _firstChild;

    while (node != null) {
      if (node.getNodeType() == TEXT_NODE &&
          node.getNextSibling() != null &&
          node.getNextSibling().getNodeType() == TEXT_NODE) {
        Text text = (Text) node;
        Text next = (Text) node.getNextSibling();
        text.appendData(next.getData());
        removeChild(next);
      } else if (node.getNodeType() == ELEMENT_NODE) {
        Element elt = (Element) node;
        elt.normalize();
        node = node.getNextSibling();
      } else
        node = node.getNextSibling();
    }
  }

  public boolean hasContent() 
  {
    return true;
  }

  public boolean equals(Object arg)
  {
    return this == arg;
  }

  public boolean equals(Node arg, boolean deep)
  {
    return this == arg;
  }

  /**
   * Returns the text value of the element.  For an element, the text
   * value is the smashing together of all the child text nodes.
   */
  public String getTextValue()
  {
    CharBuffer cb = CharBuffer.allocate();

    for (QAbstractNode node = _firstChild; node != null; node = node._next) {
      cb.append(node.getTextValue());
    }

    return cb.close();
  }

  void print(XmlPrinter out) throws IOException
  {
    out.startElement(getNamespaceURI(), getLocalName(), getNodeName());
    for (QAbstractNode node = (QAbstractNode) getFirstAttribute();
         node != null;
         node = (QAbstractNode) node.getNextSibling()) {
      out.attribute(node.getNamespaceURI(),
                    node.getLocalName(),
                    node.getNodeName(),
                    node.getNodeValue());
    }
    for (Node node = getFirstChild();
         node != null;
         node = node.getNextSibling()) {
      ((QAbstractNode) node).print(out);
    }
    out.endElement(getNamespaceURI(), getLocalName(), getNodeName());
  }

  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("Element[" + _name);

    for (QAttr attr = (QAttr) getFirstAttribute();
         attr != null;
         attr = (QAttr) attr.getNextSibling())
      cb.append(" " + attr);
    cb.append("]");

    return cb.close();
  }

  private Object writeReplace()
  {
    return new SerializedXml(this);
  }

  static class TagPredicate implements QNodePredicate  {
    String _name;

    TagPredicate(String name)
    {
      if (name == null)
        name = "*";
      
      _name = name;
    }

    public boolean isMatch(QAbstractNode node)
    {
      return (node.getNodeName().equals(_name) ||
              _name.equals("*") && node instanceof Element);
    }
  }

  static class NSTagPredicate implements QNodePredicate  {
    String _uri;
    String _local;

    NSTagPredicate(String uri, String local)
    {
      _uri = uri;
      _local = local;
    }

    public boolean isMatch(QAbstractNode node)
    {
      return (node instanceof Element &&
              ("*".equals(_local) || _local.equals(node.getLocalName())) &&
              ("*".equals(_uri) || _uri.equals(node.getNamespaceURI()))
      );
    }
  }
}
