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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.caucho.log.Log;

import com.caucho.xml.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.make.Dependency;

/**
 * The Registry is a configuration tree based on a key, value pair
 * structure, essentially like an AList.
 */
public final class Registry {
  static final L10N L = new L10N(Registry.class);
  static final Logger log = Log.open(Registry.class);
  
  private static Registry _defaultRegistry;

  private static int _changeCount;
  private static ArrayList<ChangeListener> _listeners;

  private RegistryNode _top;
  // source files for change detection
  private ArrayList<Depend> _dependList = new ArrayList<Depend>();
  // Last time the dependencies were checked
  private long _lastModifiedCheck;
  // True if the config has been modified
  private boolean _isModified;
  
  public Registry()
  {
  }

  /**
   * Create a new RegistryNode.  Use is discouraged.
   *
   * @param name name of the node
   * @param value value of the node
   *
   * @return the new node
   */
  public RegistryNode createNode(String name, String value)
  {
    return new RegistryNode(this, name, value, null, 0);
  }

  /**
   * Returns true if any of the source configuration files have changed.
   */
  public boolean isModified()
  {
    long now = Alarm.getCurrentTime();

    if (_isModified)
      return true;
    
    if (now < _lastModifiedCheck + 1000)
      return false;

    _lastModifiedCheck = now;

    for (int i = _dependList.size() - 1; i >= 0; i--) {
      Dependency depend = _dependList.get(i);

      if (depend.isModified()) {
        _isModified = true;
        return true;
      }
    }

    return false;
  }

  /**
   * Returns the dependency list.
   */
  public ArrayList<Depend> getDependList()
  {
    return _dependList;
  }

  /**
   * Returns the top node.
   */
  public RegistryNode getTop()
  {
    return _top;
  }

  /**
   * Parses a registry tree from a file.
   *
   * @param path the file containing the configuration.
   *
   * @return the root of the configuration tree
   */
  public static Registry parse(Path path)
    throws IOException, SAXException
  {
    ReadStream is = null;
    try {
      is = path.openRead();
      return parse(is);
    } finally {
      if (is != null)
	is.close();
    }
  }

  /**
   * Parses a registry tree from an input stream.
   *
   * @param is a stream containing the configuration.
   *
   * @return the root of the configuration tree.
   */
  public static Registry parse(InputStream is)
    throws IOException, SAXException
  {
    ReadStream rs = Vfs.openRead(is);

    try {
      return parse(rs);
    } finally {
      rs.close();
    }
  }
  
  /**
   * Parses a registry tree from a file.
   *
   * @param is a stream containing the configuration.
   *
   * @return the root of the configuration tree.
   */
  public static Registry parse(ReadStream is)
    throws IOException, SAXException
  {
    Registry root = new Registry();
    RegistryNode top = new RegistryNode(root, null, null, null, 0);
    root._top = top;

    parse(is, top);

    return root;
  }

  /**
   * Parses a registry tree from a file.
   *
   * @param is a stream containing the configuration.
   * @param top parent node for attaching the children
   *
   * @return the top node
   */
  static RegistryNode parse(ReadStream is, RegistryNode top)
    throws IOException, SAXException
  {
    XmlParser parser = new Xml();

    parser.setResinInclude(true);

    Path path = is.getPath();
    if (path != null)
      top.getRoot()._dependList.add(new Depend(path));
    
    SaxHandler handler = new SaxHandler(path, top);
    parser.setContentHandler(handler);

    parser.parse(is);

    return top;
  }

  /**
   * Parses a registry tree from a file.
   *
   * @param path the file containing the configuration.
   *
   * @return the root of the configuration tree
   */
  public static Registry parse(Path path, Schema schema)
    throws IOException, SAXException
  {
    ReadStream is = null;
    try {
      is = path.openRead();
      return parse(is, schema);
    } finally {
      if (is != null)
	is.close();
    }
  }

  /**
   * Parses a registry tree from an input stream.
   *
   * @param is a stream containing the configuration.
   *
   * @return the root of the configuration tree.
   */
  public static Registry parse(InputStream is, Schema schema)
    throws IOException, SAXException
  {
    ReadStream rs = Vfs.openRead(is);

    try {
      return parse(rs, schema);
    } finally {
      rs.close();
    }
  }
  
  /**
   * Parses a registry tree from a file.
   *
   * @param is a stream containing the configuration.
   *
   * @return the root of the configuration tree.
   */
  public static Registry parse(ReadStream is, Schema schema)
    throws IOException, SAXException
  {
    Registry root = new Registry();
    RegistryNode top = new RegistryNode(root, null, null, null, 0);
    root._top = top;

    parse(is, top, schema);

    return root;
  }

  /**
   * Parses a registry tree from a file.
   *
   * @param is a stream containing the configuration.
   * @param top parent node for attaching the children
   * @param schema validation schema
   *
   * @return the top node
   */
  static RegistryNode parse(ReadStream is, RegistryNode top, Schema schema)
    throws IOException, SAXException
  {
    throw new UnsupportedOperationException();
    
    /*
    try {
      XmlParser xmlParser = new Xml();

      xmlParser.setResinInclude(true);

      Path path = is.getPath();
      if (path != null) {
        xmlParser.setSearchPath(path.getParent());
        top.getRoot()._dependList.add(new Depend(path));
      }

      XMLReader parser = xmlParser;

      SaxHandler handler = new SaxHandler(path, top);

      if (schema != null) {
        Verifier verifier = schema.newVerifier();
        VerifierFilter filter = verifier.getVerifierFilter();

        filter.setParent(parser);
        parser = filter;
      }

      parser.setContentHandler(handler);
      parser.setErrorHandler(handler);

      InputSource source = new InputSource(is);
      source.setSystemId(is.getUserPath());
      parser.parse(source);

      return top;
    } catch (IOException e) {
      throw e;
    } catch (SAXException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      SAXException e1 = new SAXException(e);
      e1.initCause(e);
      
      throw e1;
    }
    */
  }

  /**
   * Sets the default configuration
   *
   * @param registry configuration to be used as the new default
   */
  public static synchronized Registry setDefault(Registry registry)
  {
    return setRegistry(registry);
  }

  public static synchronized Registry setRegistry(Registry registry)
  {
    Registry old = _defaultRegistry;
    _defaultRegistry = registry;

    if (old != null)
      old._isModified = true;
    if (registry != null)
      registry._isModified = false;
    
    handleChange();

    return old;
  }

  /**
   * Returns the default registry.
   */
  public static synchronized Registry getRegistry()
  {
    return _defaultRegistry;
  }

  /**
   * Looks up a registry node based on the path.
   *
   * @param path relative path into the registry.
   */
  public static RegistryNode lookup(String path)
  {
    Registry registry = getRegistry();

    if (registry == null)
      return null;
    
    RegistryNode node = registry._top;

    return node == null ? null : node.lookup(path);
  }

  public static boolean getBoolean(String path, boolean deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getBoolean();
  }

  public static int getInt(String path, int deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getInt();
  }

  public static double getDouble(String path, double deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getDouble();
  }

  public static String getString(String path, String deflt)
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getString();
  }

  /**
   * Returns a configuration value, interpreted as a file path.
   *
   * @param path the registry path
   * @param deflt default value if the configuration isn't specified.
   *
   * @return the matching registry value or null
   */
  public static String getPath(String path, String deflt)
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getPath();
  }

  public static QDate getDate(String path, QDate deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getDate();
  }

  public static long getPeriod(String path, long deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    return node == null ? deflt : node.getPeriod();
  }

  public synchronized static void addListener(ChangeListener listener)
  {
    if (_listeners == null)
      _listeners = new ArrayList<ChangeListener>();
    
    _listeners.add(listener);
  }

  public static int getChangeCount()
  {
    return _changeCount;
  }

  public synchronized static void removeListener(ChangeListener listener)
  {
    if (_listeners != null)
      _listeners.remove(listener);
  }

  /**
   * Callback when the state changes
   */
  static void handleChange()
  {
    _changeCount++;
    
    if (_listeners == null)
      return;

    for (int i = 0; i < _listeners.size(); i++) {
      ChangeListener listener = _listeners.get(i);
      listener.handleChange(null);
    }
  }

  /**
   * Internal handler for parsing the resin.conf file.
   */
  static class SaxHandler extends DefaultHandler {
    Path _path;
    private Locator _locator;
    ArrayList _elements = new ArrayList();

    // the current node
    RegistryNode _node;
    // true if the current node has some text
    private boolean _hasText;
    private boolean _hasTag;
    private boolean _preserveSpace;

    SaxHandler(Path path, RegistryNode top)
    {
      _path = path;
      _node = top;
    }

    /**
     * Returns the root of the parsed registry.
     */
    Registry getRoot()
    {
      return _node.getRoot();
    }

    /**
     * Saves the SAX parser's document locator.
     *
     * @param locator the locator containing the current filename and line.
     */
    public void setDocumentLocator(Locator locator)
    {
      _locator = locator;
    }

    /**
     * Callback when starting a new element
     *
     * @param name the element's name
     * @param attrs attributes of the element
     */
    public void startElement(String uri, String localName,
                             String qName, Attributes attrs)
      throws SAXException
    {
      if (_hasText)
        throw error(L.l("Element <{0}> is forbidden because parent tag <{1}> already contains text.  Tags must either contain other tags or text, not both.",
                        qName, _node.getName()));
      
      if (qName.equals("resin:include")) {
        doInclude(attrs);
        return;
      }

      String name = localName;
      
      RegistryNode child;

      if (_locator instanceof XmlParser)
        child = _node.add(name, null,
                          ((XmlParser) _locator).getFilename(),
                          _locator.getLineNumber());
      else
        child = _node.add(name, null,
                          _locator.getSystemId(),
                          _locator.getLineNumber());
      
      _node = child;
      
      int length = attrs.getLength();
      for (int i = 0; i < length; i++) {
	String key = attrs.getQName(i);
	String value = attrs.getValue(i);

	if (key.equals("id")) {
          child._id = value;
	  child._value = value;
        }
        else if (key.equals("xml:space")) {
          if (value.equals("preserve"))
            _preserveSpace = true;
        }
        else if (key.equals("xmlns") || key.startsWith("xmlns:")) {
        }
	else
	  child.add(key, value, _locator.getSystemId(),
                    _locator.getLineNumber());
      }

      _hasText = false;
      _hasTag = false;
    }

    /**
     * Handles the resin:include element.
     *
     * @param args the attributes for the resin:include
     */
    private void doInclude(Attributes args)
      throws SAXException
    {
      try {
        String href = args.getValue("href");
        if (href == null)
          throw new SAXException("resin:include expects `href' attribute");

        Path subpath = _path.getParent().lookup(href);
        
        if (! subpath.canRead())
          throw new SAXException(L.l("can't find `{0}' in resin:include",
                                     subpath));

        ReadStream is = null;
        try {
          is = subpath.openRead();
          
          Registry.parse(is, _node);

          RegistryNode child = _node.getFirstChild();
          if (child != null)
            _node = child;
        } finally {
          if (is != null)
            is.close();
        }
      } catch (IOException e) {
        throw new SAXException(e.toString());
      }
    }

    /**
     * Callback when ending an element
     *
     * @param name the element's name
     */
    public void endElement(String uri, String localName, String qName)
    {
      if (_node != null)
	_node = _node._parent;

      _hasText = false;
      _preserveSpace = false;
      _hasTag = true;
    }

    /**
     * Returns true if the character is whitespace.
     *
     * @param ch the character to test.
     */
    private boolean isWhitespace(char ch)
    {
      switch (ch) {
      case ' ': case '\t': case '\n': case '\r':
	return true;

      default:
	return false;
      }
    }

    /**
     * Handles text characters.  If the text is entirely whitespace, ignore
     * it, otherwise use it as the value of the element.
     *
     * @param chars buffer containing the text characters
     * @param offset the offset starting the charactersoffseht eo
     * @param length length of the text characters.
     */
    public void characters(char []chars, int offset, int length)
      throws SAXException
    {
      char ch = ' ';
      
      if (! _preserveSpace) {
        for (; length > 0; length--) {
          ch = chars[offset];

          if (! (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'))
            break;

          offset++;
        }

        ch = ' ';
        for (; length > 0; length--) {
          ch = chars[offset + length - 1];

          if (! (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r'))
            break;
        }
      }
      else if (length > 0)
        ch = chars[offset];

      if (length <= 0)
        return;

      if (_hasTag) {
        throw error(L.l("Text `{0}' is forbidden because parent tag <{1}> already contains a tag.  Tags must either contain other tags or text, not both.",
                        new String(chars, offset, length), _node.getName()));
      }
      
      if (_node._id != null)
        throw error("can't use both id=value and text at `" + ch + "'");

      if (_node._value == null)
	_node._value = new String(chars, offset, length);
      else
        _node._value += new String(chars, offset, length);

      if (! _preserveSpace)
        _node._value = _node._value.trim();

      _hasText = true;
    }
  
    public void fatalError(SAXParseException e)
      throws SAXException
    {
      log.log(Level.FINER, e.toString(), e);
      throw error(e.getMessage());
    }

    public void error(SAXParseException e)
      throws SAXException
    {
      log.log(Level.FINER, e.toString(), e);
      throw error(e.getMessage());
    }

    public void warning(SAXParseException e)
      throws SAXException
    {
      log.log(Level.WARNING, e.toString(), e);
      // _os.log("warning: " + String.valueOf(e));
    }

    /**
     * Returns an error including filename and line.
     */
    SAXException error(String text)
    {
      return new SAXCompileException(_locator.getSystemId() + ":" +
                                     _locator.getLineNumber() + ": " +
                                     text);
    }
    
    /**
     * Throws an appropriate error.
     */
    public SAXException createError(Exception e)
    {
      if (e instanceof SAXException)
        return (SAXException) e;
      else
        return new SAXException(e);
    }
  }

  static class SAXCompileException extends SAXException
    implements LineCompileException {
    public SAXCompileException(String msg)
    {
      super(msg);
    }
  }
}
