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

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.ObjectExtJavaValue;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;

public class SimpleUtil
{
  public static void toXml(Env env, StringBuilder sb, Node node)
  {
    int nodeType = node.getNodeType();

    switch (nodeType) {
      case Node.DOCUMENT_NODE:
        toXmlImpl(env, sb, (Document) node);
        break;

      case Node.ELEMENT_NODE:
        toXmlImpl(env, sb, (Element) node);
        break;

      case Node.TEXT_NODE:
        toXmlImpl(env, sb, (Text) node);
        break;

      case Node.ATTRIBUTE_NODE:
        toXmlImpl(env, sb, (Attr) node);
        break;

      case Node.DOCUMENT_TYPE_NODE:
        toXmlImpl(env, sb, (DocumentType) node);
        break;

      case Node.ENTITY_REFERENCE_NODE:
        toXmlImpl(env, sb, (EntityReference) node);
        break;

      case Node.COMMENT_NODE:
        toXmlImpl(env, sb, (Comment) node);
        break;

      default:
        throw new UnsupportedOperationException(node.getClass().getName());
    }
  }

  public static void toXmlImpl(Env env, StringBuilder sb, Document doc)
  {
    sb.append("<?xml version=");

    sb.append('"');
    sb.append(doc.getXmlVersion());
    sb.append('"');

    if (doc.getXmlEncoding() != null) {
      sb.append(' ');
      sb.append("encoding=");

      sb.append('"');
      sb.append(doc.getXmlEncoding());
      sb.append('"');
    }

    sb.append("?>\n");

    Node child = doc.getFirstChild();
    while (child != null) {
      toXml(env, sb, child);
      sb.append('\n');

      child = child.getNextSibling();
    }

  }

  public static void toXmlImpl(Env env, StringBuilder sb, Element node)
  {
    sb.append('<');
    sb.append(node.getNodeName());

    NamedNodeMap attrMap = node.getAttributes();

    for (int i = 0; i < attrMap.getLength(); i++) {
      Node attr = attrMap.item(i);

      toXml(env, sb, attr);
    }

    if (node.getFirstChild() == null) {
      sb.append('/');
      sb.append('>');
    }
    else {
      sb.append('>');

      Node child = node.getFirstChild();

      while (child != null) {
        toXml(env, sb, child);

        child = child.getNextSibling();
      }

      sb.append('<');
      sb.append('/');
      sb.append(node.getNodeName());
      sb.append('>');
    }
  }

  public static void toXmlImpl(Env env, StringBuilder sb, Attr node)
  {
    sb.append(' ');
    sb.append(node.getNodeName());
    sb.append('=');

    sb.append('"');
    sb.append(node.getNodeValue());
    sb.append('"');
  }

  public static void toXmlImpl(Env env, StringBuilder sb, Text node)
  {
    String str = node.getNodeValue();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      String entity = SimpleUtil.toEntity(ch);

      if (entity != null) {
        sb.append('&');
        sb.append(entity);
        sb.append(';');
      }
      else {
        sb.append(ch);
      }
    }
  }

  public static void toXmlImpl(Env env, StringBuilder sb, DocumentType node)
  {
    sb.append("<!DOCTYPE");

    sb.append(' ');
    sb.append(node.getName());

    sb.append(' ');
    sb.append("SYSTEM");

    sb.append(' ');
    sb.append('"');
    sb.append(node.getSystemId());
    sb.append('"');

    NamedNodeMap entities = node.getEntities();

    if (entities != null && entities.getLength() > 0) {
      sb.append(' ');
      sb.append('[');
      sb.append('\n');

      for (int i = 0; i < entities.getLength(); i++) {
        Entity entity = (Entity) entities.item(i);

        sb.append("<!ENTITY");

        sb.append(' ');
        sb.append(entity.getNodeName());


        sb.append(' ');
        sb.append('"');
        sb.append(entity.getTextContent());
        sb.append('"');

        sb.append('>');
        sb.append('\n');
      }

      sb.append(']');
    }

    sb.append('>');
  }

  public static void toXmlImpl(Env env, StringBuilder sb, EntityReference node)
  {
    sb.append('&');
    sb.append(node.getNodeName());
    sb.append(';');
  }

  public static void toXmlImpl(Env env, StringBuilder sb, Comment node)
  {
    sb.append("<!--");

    sb.append(node.getNodeValue());

    sb.append("-->");
  }

  public static String toEntity(char ch)
  {
    switch (ch) {
      case '<':
        return "lt";
      case '>':
        return "gt";
      case '&':
        return "amp";
      case '"':
        return "quot";
      case '\'':
        return "apos";
      default:
        return null;
    }
  }

  public static int fromEntity(String name)
  {
    if ("lt".equals(name)) {
      return '<';
    }
    else if ("gt".equals(name)) {
      return '>';
    }
    else if ("amp".equals(name)) {
      return '&';
    }
    else if ("quot".equals(name)) {
      return '"';
    }
    else if ("apos".equals(name)) {
      return '\'';
    }
    else {
      return -1;
    }
  }

  public static boolean isSameNamespace(Node node, String namespace)
  {
    String prefix = getPrefix(node.getNodeName());

    return hasNamespace(node, prefix, namespace);
  }

  public static String getPrefix(String name)
  {
    int i = name.indexOf(':');

    if (i < 0) {
      return null;
    }

    String prefix = name.substring(0, i);

    return prefix;
  }

  public static boolean hasNamespace(Node node, String prefix, String namespace)
  {
    if (namespace == null) {
      return true;
    }

    String attrName;

    if (prefix != null && prefix.length() > 0) {
      attrName = "xmlns:" + prefix;
    }
    else {
      attrName = "xmlns";
    }

    while (node != null) {
      NamedNodeMap attrMap = node.getAttributes();

      if (attrMap == null) {
        break;
      }

      Attr attr = (Attr) attrMap.getNamedItem(attrName);

      if (attr != null) {
        return attr.getNodeValue().equals(namespace);
      }

      node = node.getParentNode();
    }

    return false;
  }

  public static Value wrapJava(Env env,
                               QuercusClass cls,
                               SimpleXMLNode node)
  {
    if (! "SimpleXMLElement".equals(cls.getName())) {
      return new ObjectExtJavaValue(env, cls, node, cls.getJavaClassDef());
    }
    else {
      return new JavaValue(env, node, cls.getJavaClassDef());
    }
  }
}
