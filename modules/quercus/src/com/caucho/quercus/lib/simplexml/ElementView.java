/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.ObjectValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;

public class ElementView extends SimpleView
{
  private static final Logger log
    = Logger.getLogger(ElementView.class.getName());

  private final Node _node;

  public ElementView(Node node)
  {
    super(node.getOwnerDocument());

    _node = node;
  }

  @Override
  public String getNodeName()
  {
    String name = _node.getNodeName();

    int i = name.indexOf(':');
    if (i >= 0) {
      name = name.substring(i + 1);
    }

    return name;
  }

  @Override
  public ChildrenView getChildren(String namespace, String prefix)
  {
    ArrayList<SimpleView> childList = new ArrayList<SimpleView>();

    Node node = getNode();

    Node child = node.getFirstChild();
    while (child != null) {
      String childName = child.getNodeName();

      if (child.getNodeType() == Node.ELEMENT_NODE
          && (namespace == null || SimpleUtil.isSameNamespace(child, namespace))
          && (prefix == null || prefix.equals(SimpleUtil.getPrefix(childName)))) {
        ElementView view = new ElementView((Element) child);

        childList.add(view);
      }

      child = child.getNextSibling();
    }

    ArrayList<AttributeView> attrList = new ArrayList<AttributeView>();

    NamedNodeMap attrMap = _node.getAttributes();

    for (int i = 0; i < attrMap.getLength(); i++) {
      Attr attr = (Attr) attrMap.item(i);

      AttributeView view = new AttributeView(attr);

      attrList.add(view);
    }

    return new ChildrenView(this, childList, attrList);
  }

  @Override
  public AttributeListView getAttributes(String namespace)
  {
    ArrayList<AttributeView> attrList = new ArrayList<AttributeView>();
    NamedNodeMap attrMap = _node.getAttributes();

    for (int i = 0; i < attrMap.getLength(); i++) {
      Attr attr = (Attr) attrMap.item(i);

      String name = attr.getNodeName();
      String prefix = SimpleUtil.getPrefix(name);

      if ("xmlns".equals(name) || "xmlns".equals(prefix)) {
        continue;
      }
      else if (namespace == null && prefix == null) {
      }
      else if (namespace == null || prefix == null) {
        continue;
      }
      else {
        String nsAttrName = "xmlns:" + prefix;

        String attrNamespace = getNamespace(nsAttrName);

        if (! namespace.equals(attrNamespace)) {
          continue;
        }
      }

      AttributeView view = new AttributeView(attr);

      attrList.add(view);
    }

    AttributeListView view = new AttributeListView(getOwnerDocument(), attrList);

    return view;
  }

  private String getNamespace(String nsAttrName)
  {
    Node node = _node;
    while (node != null) {
      NamedNodeMap attrMap = node.getAttributes();

      if (attrMap == null) {
        break;
      }

      Attr attr = (Attr) attrMap.getNamedItem(nsAttrName);

      if (attr != null) {
        return attr.getNodeValue();
      }

      node = node.getParentNode();
    }

    return null;
  }

  @Override
  public SimpleView addChild(Env env,
                             String name,
                             String value,
                             String namespace)
  {
    Document doc = _node.getOwnerDocument();

    Element e;

    int i = name.indexOf(':');
    String prefix = null;

    if (i >= 0) {
      if (namespace != null) {
        prefix = name.substring(0, i);
      }
      else {
        name = name.substring(i + 1);
      }
    }

    if (prefix != null && namespace != null) {
      e = doc.createElementNS(namespace, name);
    }
    else {
      e = doc.createElement(name);
    }

    _node.appendChild(e);

    if (namespace != null && ! SimpleUtil.hasNamespace(_node, prefix, namespace)) {
      Attr attr;

      if (prefix != null) {
        attr = doc.createAttribute("xmlns:" + prefix);
      }
      else {
        attr = doc.createAttribute("xmlns");
      }

      attr.setNodeValue(namespace);

      addAttribute(e, attr);
    }

    if (value != null) {
      e.setTextContent(value);
    }

    ElementView view = new ElementView(e);

    return view;
  }

  @Override
  public void addAttribute(Env env,
                           String name,
                           String value,
                           String namespace)
  {
    Document doc = getOwnerDocument();

    String prefix = SimpleUtil.getPrefix(name);

    if (namespace != null && ! SimpleUtil.hasNamespace(_node, prefix, namespace)) {
      Attr namespaceAttr;

      if (prefix != null) {
        namespaceAttr = doc.createAttribute("xmlns:" + prefix);
      }
      else {
        namespaceAttr = doc.createAttribute("xmlns");
      }

      namespaceAttr.setNodeValue(namespace);

      addAttribute(_node, namespaceAttr);
    }

    Attr attr = doc.createAttribute(name);
    attr.setNodeValue(value);

    addAttribute(_node, attr);
  }

  private static void addAttribute(Node node, Attr attr)
  {
    NamedNodeMap attrMap = node.getAttributes();
    attrMap.setNamedItem(attr);
  }

  @Override
  public HashMap<String,String> getNamespaces(boolean isRecursive,
                                              boolean isFromRoot,
                                              boolean isCheckUsage)
  {
    HashMap<String,String> newMap = new LinkedHashMap<String,String>();
    HashMap<String,String> usedMap = new LinkedHashMap<String,String>();

    Node node;

    if (isFromRoot) {
      node = getOwnerDocument().getDocumentElement();
    }
    else {
      node = _node;
    }

    getNamespaces(newMap, usedMap, node, isRecursive, isCheckUsage, true);

    if (! isCheckUsage) {
      usedMap = newMap;
    }

    return usedMap;
  }

  private void getNamespaces(HashMap<String,String> newMap,
                             HashMap<String,String> usedMap,
                             Node node,
                             boolean isRecursive,
                             boolean isCheckUsage,
                             boolean isTop)
  {
    NamedNodeMap attrMap = node.getAttributes();

    if ((isTop || isRecursive) && attrMap != null) {
      for (int i = 0; i < attrMap.getLength(); i++) {
        Attr attr = (Attr) attrMap.item(i);

        String attrName = attr.getNodeName();

        if (attrName.equals("xmlns") || attrName.startsWith("xmlns:")) {
          String prefix = "";

          int j = attrName.indexOf(':');

          if (j >= 0) {
            prefix = attrName.substring(j + 1);
          }

          newMap.put(prefix, attr.getNodeValue());
        }
      }
    }

    if (isCheckUsage) {
      String nodeName = node.getNodeName();
      int i = nodeName.indexOf(':');

      if (i >= 0) {
        String prefix = nodeName.substring(0, i);
        String namespace = newMap.get(prefix);

        if (namespace != null) {
          usedMap.put(prefix, namespace);
        }
      }
    }

    Node child = node.getFirstChild();
    while (child != null) {
      getNamespaces(newMap, usedMap, child, isRecursive, isCheckUsage, false);

      child = child.getNextSibling();
    }
  }

  @Override
  public List<SimpleView> xpath(Env env,
                                SimpleNamespaceContext context,
                                String expression)
  {
    try {
      return SimpleView.xpath(_node, context, expression);
    }
    catch (XPathExpressionException e) {
      log.log(Level.FINE, e.getMessage());
      env.warning(e);

      return null;
    }
  }

  @Override
  protected Node getNode()
  {
    return _node;
  }

  @Override
  public SimpleView getIndex(Env env, Value indexV)
  {
    if (indexV.isString()) {
      String name = indexV.toString();

      Attr attr = getAttribute(name);

      if (attr == null) {
        return null;
      }

      AttributeView view = new AttributeView(attr);

      return view;
    }
    else if (indexV.isLongConvertible()) {
      int i = indexV.toInt();

      if (i == 0) {
        return this;
      }

      Node next = _node.getNextSibling();

      while (next != null && i >= 0) {
        if (next.getNodeName().equals(_node.getNodeName())
            && --i == 0) {

          ElementView view = new ElementView(next);

          return view;
        }

        next = next.getNextSibling();
      }

      return null;
    }
    else {
      return null;
    }
  }

  @Override
  public SimpleView setIndex(Env env, Value indexV, Value value)
  {
    String str = value.toString();


    Node node = _node;
    Document doc = node.getOwnerDocument();

    if (indexV.isLongConvertible()) {
      node.setTextContent(str);
    }
    else {
      Attr attr = doc.createAttribute(indexV.toString());
      attr.setValue(str);

      NamedNodeMap attrMap = node.getAttributes();
      attrMap.setNamedItem(attr);
    }

    return this;
  }

  @Override
  public SimpleView getField(Env env, Value indexV)
  {
    String nodeName = indexV.toStringValue(env).toString();

    ArrayList<SimpleView> childList = new ArrayList<SimpleView>();

    Node child = getNode().getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        String childName = child.getNodeName();

        if (nodeName.equals(childName)) {
          ElementView view = new ElementView(child);

          childList.add(view);
        }
      }

      child = child.getNextSibling();
    }

    ArrayList<AttributeView> attrList = new ArrayList<AttributeView>();

    SelectedView view
      = new SelectedView(this, nodeName, childList, attrList);

    return view;
  }

  private Node getChild(String name)
  {
    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeName().equals(name)) {
        return child;
      }

      child = child.getNextSibling();
    }

    return null;
  }

  private Attr getAttribute(String name)
  {
    NamedNodeMap map = _node.getAttributes();

    Attr attr = (Attr) map.getNamedItem(name);

    return attr;
  }

  @Override
  public SimpleView setField(Env env, Value indexV, Value value)
  {
    String name = indexV.toString();

    Node child = getChild(name);

    if (child == null) {
      Document doc = _node.getOwnerDocument();

      Element e = doc.createElement(name);
      e.setTextContent(value.toString());

      _node.appendChild(e);
    }
    else {
      child.setTextContent(value.toString());
    }

    return this;
  }

  @Override
  public int getCount()
  {
    int count = 0;

    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        count++;
      }

      child = child.getNextSibling();
    }

    return count;
  }

  @Override
  public String toString(Env env)
  {
    StringBuilder sb = new StringBuilder();

    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.TEXT_NODE) {
        sb.append(child.getNodeValue());
      }
      else if (child.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
        String name = child.getNodeName();

        int ch = SimpleUtil.fromEntity(name);

        if (ch >= 0) {
          sb.append((char) ch);
        }
        else {
          sb.append('&');
          sb.append(name);
          sb.append(';');
        }
      }

      child = child.getNextSibling();
    }

    return sb.toString();
  }

  @Override
  public Iterator<Map.Entry<IteratorIndex,SimpleView>> getIterator()
  {
    ChildrenView view = getChildren(null, null);

    return view.getIterator();
  }

  @Override
  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    LinkedHashMap<Value,Value> map
      = new LinkedHashMap<Value,Value>();

    NamedNodeMap attrMap = _node.getAttributes();

    if (attrMap.getLength() > 0) {
      ArrayValue array = new ArrayValueImpl();

      for (int i = 0; i < attrMap.getLength(); i++) {
        Attr attr = (Attr) attrMap.item(i);

        String value = attr.getNodeValue();

        array.put(env.createString(attr.getNodeName()),
                  env.createString(value));
      }

      map.put(env.createString("@attributes"), array);
    }

    ArrayList<Element> elementList = new ArrayList<Element>();

    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        elementList.add((Element) child);
      }

      child = child.getNextSibling();
    }

    if (elementList.size() == 0) {
      String text = toString(env);

      if (text != null) {
        map.put(LongValue.ZERO, env.createString(text));
      }
    }
    else if (elementList.size() == 1) {
      Element node = elementList.get(0);
      StringValue name = env.createString(node.getNodeName());

      ElementView view = new ElementView(node);

      SimpleXMLElement e = new SimpleXMLElement(cls, view);
      Value value = e.wrapJava(env);

      map.put(name, value);
    }
    else if (elementList.size() > 1) {
      for (Element node : elementList) {
        StringValue name = env.createString(node.getNodeName());

        ElementView view = new ElementView(node);
        SimpleXMLElement e = new SimpleXMLElement(cls, view);

        Value value = e.wrapJava(env);
        Value oldValue = map.get(name);

        if (oldValue == null) {
          map.put(name, value);
        }
        else if (oldValue.isArray()) {
          oldValue.toArrayValue(env).append(value);
        }
        else {
          ArrayValue array = new ArrayValueImpl();

          array.append(oldValue);
          array.append(value);

          map.put(name, array);
        }
      }
    }

    return map.entrySet();
  }

  @Override
  public boolean toXml(Env env, StringBuilder sb)
  {
    SimpleUtil.toXml(env, sb, _node);

    return true;
  }

  @Override
  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    ArrayList<ElementView> elementList = new ArrayList<ElementView>();
    ArrayList<AttributeView> attrList = new ArrayList<AttributeView>();

    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        ElementView view = new ElementView((Element) child);

        elementList.add(view);
      }

      child = child.getNextSibling();
    }

    NamedNodeMap attrMap = _node.getAttributes();

    if (attrMap.getLength() > 0) {
      for (int i = 0; i < attrMap.getLength(); i++) {
        Attr attr = (Attr) attrMap.item(i);

        AttributeView view = new AttributeView(attr);

        attrList.add(view);
      }
    }

    ObjectValue obj = env.createObject();
    obj.setClassName(cls.getName());

    if (attrList.size() > 0) {
      ArrayValue array = new ArrayValueImpl();

      for (AttributeView view : attrList) {
        StringValue attrName = env.createString(view.getNodeName());
        StringValue attrValue = env.createString(view.getNodeValue());

        array.append(attrName, attrValue);
      }

      obj.putField(env, env.createString("@attributes"), array);
    }

    if (elementList.size() == 0) {
      String text = toString(env);

      if (text != null && text.length() > 0) {
        if (isChildren) {
          StringValue value = env.createString(text);
          obj.putField(env, env.createString("0"), value);

          return obj;
        }
        else {
          return env.createString(text);
        }
      }
      else {
        return obj;
      }
    }
    else {
      for (ElementView view : elementList) {
        StringValue name = env.createString(view.getNodeName());

        SimpleXMLElement e = new SimpleXMLElement(cls, view);

        Value value = e.wrapJava(env);
        Value oldValue = obj.getField(env, name);

        if (oldValue == UnsetValue.UNSET) {
          obj.putField(env, name, value);
        }
        else if (oldValue.isArray()) {
          oldValue.toArrayValue(env).append(value);
        }
        else {
          ArrayValue array = new ArrayValueImpl();

          array.append(oldValue);
          array.append(value);

          obj.putField(env, name, array);
        }
      }

      return obj;
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    Node child = _node.getFirstChild();

    while (child != null) {
      if (child.getNodeType() == Node.TEXT_NODE) {
        String text = child.getNodeValue().trim();
        text = text.replace("\n", " ");

        sb.append(text);
      }

      child = child.getNextSibling();
    }

    if (sb.length() > 0) {
      return getClass().getSimpleName() + "[name=" + _node.getNodeName() + ",text=" + sb + ",hash=" + _node.hashCode() + "]";
    }
    else {
      return getClass().getSimpleName() + "[name=" + _node.getNodeName() + ",hash=" + _node.hashCode() + "]";
    }

  }
}
