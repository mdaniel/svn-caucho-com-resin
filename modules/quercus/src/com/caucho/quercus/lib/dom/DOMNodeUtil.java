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

package com.caucho.quercus.lib.dom;

import com.caucho.quercus.env.Env;

import org.w3c.dom.*;

public class DOMNodeUtil {

  public static DOMNode createDOMNode(Env env, Node node)
  {
    if (node == null)
      return null;

    switch (node.getNodeType()) {
      case Node.ATTRIBUTE_NODE:
        return new DOMAttr(env, (Attr) node);
      case Node.ELEMENT_NODE:
        return new DOMElement(env, (Element) node);
      case Node.COMMENT_NODE:
        return new DOMComment(env, (Comment) node);
      case Node.TEXT_NODE:
        return new DOMText(env, (Text) node);
      case Node.DOCUMENT_NODE:
        return new DOMDocument(env, (Document) node);
      case Node.DOCUMENT_TYPE_NODE:
        return new DOMDocumentType(env, (DocumentType) node);
      case Node.DOCUMENT_FRAGMENT_NODE:
        return new DOMDocumentFragment(env, (DocumentFragment) node);
      case Node.ENTITY_NODE:
        return new DOMEntity(env, (Entity) node);
      case Node.ENTITY_REFERENCE_NODE:
        return new DOMEntityReference(env, (EntityReference) node);
      case Node.NOTATION_NODE:
        return new DOMNotation(env, (Notation) node);
      case Node.PROCESSING_INSTRUCTION_NODE:
        return new DOMProcessingInstruction(env, (ProcessingInstruction) node);
      default:
        return null;
    }
  }

  public static StringBuilder asXML(Node node)
  {
    return asXMLVersion(node, "1.0");
  }

  public static StringBuilder asXMLVersion(Node node,
                                           String version)
  {
    StringBuilder result = new StringBuilder();

    result.append("<?xml version=\"").append(version).append("\"?>\n");
    generateXML(node, result);

    return result;
  }

  public static StringBuilder asXMLVersionEncoding(Node node,
                                                   String version,
                                                   String encoding)
  {
    StringBuilder result = new StringBuilder();

    result.append("<?xml version=\"").append(version).append("\"");

    if (!"".equals(encoding)) {
      result.append(" encoding=\"").append(encoding).append("\"");
    }

    result.append("?>\n");
    generateXML(node, result);

    return result;
  }

  /**
   * recursive helper function for asXML
   * @return XML in string buffer
   */
  public static StringBuilder generateXML(Node node,
                                          StringBuilder sb)
  {
    if (node == null)
        return sb;

    // If this is a text node, then just return the text
    if (node.getNodeType() == Node.TEXT_NODE) {
      sb.append(node.getNodeValue());
      return sb;
    }

    // not a text node
    sb.append("<");

    sb.append(node.getNodeName());

    // add attributes, if any
    NamedNodeMap attrs = node.getAttributes();
    int attrLength = attrs.getLength();

    for (int i=0; i < attrLength; i++) {
      Node attribute = attrs.item(i);
      sb.append(" ")
        .append(attribute.getNodeName())
        .append("=\"")
        .append(attribute.getNodeValue())
        .append("\"");
    }

    // recurse through children, if any
    NodeList children = node.getChildNodes();
    int nodeLength = children.getLength();

    if (nodeLength == 0) {
      sb.append(" />");
      return sb;
    }

    sb.append(">");

    // there are children
    for (int i=0; i < nodeLength; i++) {
      Node child = children.item(i);

      if (child.getNodeType() == Node.TEXT_NODE) {
        sb.append(child.getNodeValue());
        continue;
      }
      generateXML(child, sb);
    }

    // add closing tag
    sb.append("</").append(node.getNodeName()).append(">");

    return sb;
  }
}
