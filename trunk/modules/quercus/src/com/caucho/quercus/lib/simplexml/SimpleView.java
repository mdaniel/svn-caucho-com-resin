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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.WriteStream;

/**
 * A simplexml abstraction of the dom Node.
 */
public abstract class SimpleView
{
  private final Document _doc;

  public SimpleView(Document doc)
  {
    _doc = doc;
  }

  public final Document getOwnerDocument()
  {
    return _doc;
  }

  public String getNodeName()
  {
    throw new UnsupportedOperationException();
  }

  public String getNodeValue()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public final String getEncoding()
  {
    Document doc = _doc;

    String encoding = doc.getInputEncoding();

    if (encoding == null) {
      encoding = doc.getXmlEncoding();
    }

    if (encoding == null) {
      encoding = "utf-8";
    }

    return encoding;
  }

  public ChildrenView getChildren(String namespace, String prefix)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public AttributeListView getAttributes(String namespace)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public SimpleView addChild(Env env,
                             String name,
                             String value,
                             String namespace)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public void addAttribute(Env env,
                           String name,
                           String value,
                           String namespace)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public List<SimpleView> xpath(Env env,
                                SimpleNamespaceContext context,
                                String expression)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  protected Node getNode()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public HashMap<String,String> getNamespaces(boolean isRecursive,
                                              boolean isFromRoot,
                                              boolean isCheckUsage)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public abstract SimpleView getIndex(Env env, Value indexV);
  public abstract SimpleView setIndex(Env env, Value indexV, Value value);

  public abstract SimpleView getField(Env env, Value indexV);
  public abstract SimpleView setField(Env env, Value indexV, Value value);

  public Iterator<Map.Entry<IteratorIndex,SimpleView>> getIterator()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public Set<Map.Entry<Value,Value>> getEntrySet(Env env, QuercusClass cls)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public abstract String toString(Env env);

  public abstract boolean toXml(Env env, StringBuilder sb);

  public Value toDumpValue(Env env, QuercusClass cls, boolean isChildren)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public void varDump(Env env,
                      WriteStream out,
                      int depth,
                      IdentityHashMap<Value, String> valueSet,
                      QuercusClass cls)
    throws IOException
  {
    Value value = toDumpValue(env, cls, false);

    value.varDump(env, out, depth, valueSet);
  }

  public void printR(Env env,
                     WriteStream out,
                     int depth,
                     IdentityHashMap<Value, String> valueSet,
                     QuercusClass cls)
    throws IOException
  {
    Value value = toDumpValue(env, cls, false);

    value.printR(env, out, depth, valueSet);
  }

  public int getCount()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  protected static SimpleView create(Node node)
  {
    int nodeType = node.getNodeType();

    switch (nodeType) {
      case Node.ELEMENT_NODE:
        return new ElementView(node);

      case Node.ATTRIBUTE_NODE:
        return new AttributeView((Attr) node);

      case Node.TEXT_NODE:
        return new TextView((Text) node);

      case Node.DOCUMENT_NODE:
        return new DocumentView((Document) node);

      default:
        throw new IllegalStateException(node.getClass().getSimpleName());
    }
  }

  protected static List<SimpleView> xpath(Node node,
                                          SimpleNamespaceContext context,
                                          String expression)
    throws XPathExpressionException
  {
    XPath xpath = context.getXPath();

    NodeList nodes = null;

    try {
      XPathExpression expr = xpath.compile(expression);

      nodes = (NodeList) expr.evaluate(node,
                                       XPathConstants.NODESET);
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    int nodeLength = nodes.getLength();

    if (nodeLength == 0) {
      return null;
    }

    ArrayList<SimpleView> result = new ArrayList<SimpleView>();

    for (int i = 0; i < nodeLength; i++) {
      Node nodeResult = nodes.item(i);

      SimpleView view = SimpleView.create(nodeResult);

      result.add(view);
    }

    return result;
  }
}
