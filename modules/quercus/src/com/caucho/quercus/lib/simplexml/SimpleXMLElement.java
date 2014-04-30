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
import java.io.StringReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.caucho.quercus.annotation.EntrySet;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * SimpleXMLElement object oriented API facade.
 * Also acts as the DOM document.
 */
public class SimpleXMLElement extends SimpleXMLNode
{
  private static final Logger log
    = Logger.getLogger(SimpleXMLElement.class.getName());
  private static final L10N L = new L10N(SimpleXMLElement.class);

  protected SimpleXMLElement(QuercusClass cls, SimpleView view)
  {
    super(cls, view);
  }

  protected static Value create(Env env,
                                QuercusClass cls,
                                Value data,
                                int options,
                                boolean dataIsUrl,
                                Value namespaceV,
                                boolean isPrefix)
  {
    if (data.length() == 0) {
      env.warning(L.l("xml data must have length greater than 0"));
      return BooleanValue.FALSE;
    }

    try {
      String namespace = null;

      if (! namespaceV.isNull()) {
        namespace = namespaceV.toString();
      }

      Document doc = parse2(env, data, options, dataIsUrl, namespace, isPrefix);

      if (doc == null) {
        return BooleanValue.FALSE;
      }

      DocumentView view = buildNode(env, cls, doc, namespace, isPrefix);

      SimpleXMLElement e = new SimpleXMLElement(cls, view);

      return e.wrapJava(env);

    } catch (IOException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    }
    catch (ParserConfigurationException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    }
    catch (SAXException e) {
      e.printStackTrace();

      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  public static Value __construct(Env env,
                                  Value data,
                                  @Optional int options,
                                  @Optional boolean dataIsUrl,
                                  @Optional Value namespaceV,
                                  @Optional boolean isPrefix)
  {
    QuercusClass cls = env.getCallingClass();

    if (cls == null) {
      cls = env.getClass("SimpleXMLElement");
    }

    return create(env, cls,
                  data, options, dataIsUrl, namespaceV, isPrefix);
  }

  //
  // XML parsing and generation
  //

  private static Document parse(Env env,
                                Value data,
                                int options,
                                boolean dataIsUrl,
                                String namespace,
                                boolean isPrefix)
    throws IOException,
           ParserConfigurationException,
           SAXException
  {
    SimpleHandler resolver = new SimpleHandler(null);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    //factory.setAttribute("http://xml.org/sax/properties/lexical-handler", resolver);
    factory.setExpandEntityReferences(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(resolver);

    /*
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    XMLReader reader = parser.getXMLReader();
    reader.setProperty("http://xml.org/sax/properties/lexical-handler", resolver);
    reader.setEntityResolver(resolver);
    */

    Document document = null;

    if (dataIsUrl) {
      Path path = env.lookup(data.toStringValue());

      // PHP throws an Exception instead
      if (path == null) {
        log.log(Level.FINE, L.l("Cannot read file/URL '{0}'", data));
        env.warning(L.l("Cannot read file/URL '{0}'", data));

        return null;
      }

      ReadStream is = path.openRead();

      try {
        document = builder.parse(is);
      } finally {
        is.close();
      }
    }
    else {
      StringReader stringReader = new java.io.StringReader(data.toString());

      document = builder.parse(new InputSource(stringReader));
    }

    return document;
  }

  private static Document parse2(Env env,
                                 Value data,
                                 int options,
                                 boolean dataIsUrl,
                                 String namespace,
                                 boolean isPrefix)
    throws IOException,
           ParserConfigurationException,
           SAXException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    //factory.setNamespaceAware(true);

    DocumentBuilder builder = factory.newDocumentBuilder();
    DOMImplementation impl = builder.getDOMImplementation();

    SimpleHandler handler = new SimpleHandler(impl);

    SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    saxFactory.setNamespaceAware(true);

    SAXParser parser = saxFactory.newSAXParser();
    XMLReader reader = parser.getXMLReader();

    //reader.setFeature("http://apache.org/xml/features/scanner/notify-char-refs", true);
    //reader.setFeature("http://xml.org/sax/features/external-general-entities", true);
    //reader.setFeature("http://xml.org/sax/features/lexical-handler/parameter-entities", true);

    reader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

    reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

    if (dataIsUrl) {
      Path path = env.lookup(data.toStringValue());

      // PHP throws an Exception instead
      if (path == null) {
        log.log(Level.FINE, L.l("Cannot read file/URL '{0}'", data));
        env.warning(L.l("Cannot read file/URL '{0}'", data));

        return null;
      }

      ReadStream is = path.openRead();

      try {
        parser.parse(is, handler);
      } finally {
        is.close();
      }
    }
    else {
      StringReader stringReader = new java.io.StringReader(data.toString());

      parser.parse(new InputSource(stringReader), handler);
    }

    Document doc = handler.getDocument();

    return doc;
  }

  private static DocumentView buildNode(Env env,
                                        QuercusClass cls,
                                        Document doc,
                                        String namespace,
                                        boolean isPrefix)
  {
    DocumentView view = new DocumentView(doc);

    return view;
  }

  /**
   * Required for 'foreach'. When only values are specified in
   * the loop <code>foreach($a as $b)</code>, this method
   * should return an iterator that contains Java objects
   * that will be wrapped in a Value.
   *
   * When a 'foreach' loop with name/value pairs
   * i.e. <code>foreach($a as $b=>$c)</code>
   * invokes this method, it expects an iterator that
   * contains objects that implement Map.Entry.
   */
  public Iterator iterator()
  {
    // php/1x05

    Iterator<Map.Entry<IteratorIndex,SimpleView>> iter = _view.getIterator();

    return new ViewIterator(iter);
  }

  @EntrySet
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    Env env = Env.getInstance();

    return _view.getEntrySet(env, _cls);
  }

  public void varDumpImpl(Env env,
                          Value obj,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _view.varDump(env, out, depth, valueSet, _cls);
  }

  public void printRImpl(Env env,
                         WriteStream out,
                         int depth,
                         IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    _view.printR(env, out, depth, valueSet, _cls);
  }

  private String getEncoding()
  {
    return _view.getEncoding();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _view + "]";
  }

  class ViewIterator implements Iterator<Map.Entry<Value,SimpleXMLElement>> {
    private final Iterator<Map.Entry<IteratorIndex,SimpleView>> _iter;

    ViewIterator(Iterator<Map.Entry<IteratorIndex,SimpleView>> iter)
    {
      _iter = iter;
    }

    @Override
    public boolean hasNext()
    {
      return _iter.hasNext();
    }

    @Override
    public Map.Entry<Value,SimpleXMLElement> next()
    {
      Map.Entry<IteratorIndex,SimpleView> entry = _iter.next();

      Value index = entry.getKey().toValue(Env.getInstance(), getEncoding());
      SimpleView view = entry.getValue();

      SimpleXMLElement e = new SimpleXMLElement(_cls, view);

      return new SimpleEntry<Value,SimpleXMLElement>(index, e);
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  class ElementIterator implements Iterator<ElementEntry> {
    private Node _node;

    ElementIterator(Node node)
    {
      _node = node;

    }

    @Override
    public boolean hasNext()
    {
      Node node = _node;

      while (node != null) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          return true;
        }

        node = node.getNextSibling();
      }

      return false;
    }

    @Override
    public ElementEntry next()
    {
      Node node = _node;

      while (node != null) {
        _node = node.getNextSibling();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
          return new ElementEntry(node);
        }

        node = _node;
      }

      return null;
    }

    @Override
    public void remove()
    {
    }
  }

  class ElementEntry implements Map.Entry<Value,Value> {
    private Node _node;

    public ElementEntry(Node node)
    {
      _node = node;
    }

    @Override
    public Value getKey()
    {
      Env env = Env.getInstance();

      return env.createString(_node.getNodeName());
    }

    @Override
    public Value getValue()
    {
      Env env = Env.getInstance();

      if (_node.getFirstChild() == null) {
        String text = _node.getTextContent();

        return env.createString(text);
      }
      else {
        ElementView view = new ElementView(_node);

        SimpleXMLElement e = new SimpleXMLElement(_cls, view);

        return e.wrapJava(env);
      }
    }

    @Override
    public Value setValue(Value value)
    {
      throw new UnsupportedOperationException();
    }
  }
}
