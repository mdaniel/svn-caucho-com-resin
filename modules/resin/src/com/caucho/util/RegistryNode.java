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

import javax.servlet.jsp.el.VariableResolver;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.el.*;
import com.caucho.config.types.Period;

/**
 * The Registry is a configuration tree based on a key, value pair
 * structure, essentially like an AList.
 */
public class RegistryNode {
  static final L10N L = new L10N(RegistryNode.class);
  
  private static final long DAY = 24L * 3600L * 1000L;

  private static QDate _calendar = new QDate();
  
  private Registry _root;
  RegistryNode _parent;
  private ArrayList<RegistryNode> _children;

  String _name;
  String _id;
  String _value;

  private String _filename;
  private int _line;

  /**
   * Zero-arg constructor.
   */
  public RegistryNode()
  {
  }

  /**
   * Creates a new registry node.
   *
   * @param root the owning registry
   * @param name the name of the attribute
   * @param value the value of the attribute
   * @param filename the filename defining the attribute.
   * @param line the line of the attribute.
   */
  RegistryNode(Registry root, String name, String value,
               String filename, int line)
  {
    _root = root;
    
    _name = name;
    _value = value;
    _filename = filename;
    _line = line;
  }

  /**
   * Returns true if the underlying files have been modified.
   */
  public boolean isModified()
  {
    return _root.isModified();
  }

  /**
   * Returns the attribute's name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the attribute's value.
   */
  public String getValue()
  {
    return _value;
  }

  /**
   * Returns the source filename for the configuration node.
   */
  public String getFilename()
  {
    return _filename;
  }

  /**
   * Returns the source line number for the configuration node.
   */
  public int getLine()
  {
    return _line;
  }

  /**
   * Returns the parent node.
   */
  public RegistryNode getParent()
  {
    return _parent;
  }

  /**
   * Returns the first child node or null if there are no children.
   */
  public RegistryNode getFirstChild()
  {
    if (_children != null && _children.size() > 0)
      return _children.get(0);
    else
      return null;
  }

  /**
   * Return the root node
   */
  public Registry getRoot()
  {
    return _root;
  }

  /**
   * Return the root node
   */
  public void setRoot(Registry root)
  {
    _root = root;
  }

  /**
   * Returns the dependency list.
   */
  public ArrayList<Depend> getDependList()
  {
    if (_root != null)
      return _root.getDependList();
    else
      return null;
  }

  /**
   * Appends the children of the next node as children of this node.
   *
   * @param next parent of the children to be added
   */
  public void append(RegistryNode next)
  {
    if (next == null || next._children == null)
      return;

    for (int i = 0; i < next._children.size(); i++) {
      RegistryNode child = next._children.get(i);

      if (_children == null)
        _children = new ArrayList<RegistryNode>();

      _children.add(child);
    }
  }

  /**
   * Adds the node as a child of the current node
   *
   * @param child new child to be added
   */
  public void addChild(RegistryNode child)
  {
    if (_children == null)
      _children = new ArrayList<RegistryNode>();

    _children.add(child);
  }

  /**
   * Returns a boolean value, returning true if no value is specified.
   *
   * <pre>
   * &lt;flag>
   * &lt;flag>true&lt;/flag>
   * &lt;flag>yes&lt;/flag>
   * </pre>
   *
   * @exception RegistryException if the value isn't true or false.
   */
  public boolean getBoolean()
    throws RegistryException
  {
    String value = getValue();

    if (value == null || value.equals("") || value.equals(getName()))
      return true;
    else if (value.equals("true") || value.equals("yes"))
      return true;
    else if (value.equals("false") || value.equals("no"))
      return false;
    else
      throw error(L.l("expected `true' or 'false' for boolean value"));
  }

  /**
   * Gets a boolean value from a subnode with a default.
   *
   * @param path hierarchical path specifying a child node
   * @param deflt default value if no node is found
   */
  public boolean getBoolean(String path, boolean deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getBoolean();
  }

  /**
   * Returns the node corresponding to the path.
   *
   * <pre>
   * RegistryNode subnode = node.lookup("web-app/servlet");
   * </pre>
   *
   * @param path slash-separated path selecting a descendent node.
   */
  public RegistryNode lookup(String path)
  {
    if (path == null)
      return this;
    
    int head = 0;
    int length = path.length();

    for (; head < length && path.charAt(head) == '/'; head++) {
    }
    
    int tail = head;

    for (; tail < length && path.charAt(tail) != '/'; tail++) {
    }

    if (tail <= head)
      return this;

    if (_children == null || _children.size() == 0)
      return null;
    
    String seq = path.substring(head, tail);
    String end = tail < length ? path.substring(tail + 1) : "";

    for (int i = 0; i < _children.size(); i++) {
      RegistryNode child = _children.get(i);
      
      if (child._name.equals(seq)) {
        RegistryNode match = child.lookup(end);
        if (match != null)
          return match;
      }
    }

    return null;
  }

  /**
   * Returns the first child matching name.
   *
   * @param name key to match
   *
   * @return the first matching child or null if none match
   */
  private RegistryNode getLink(String name)
  {
    int length = _children == null ? 0 : _children.size();

    for (int i = 0; i < length; i++) {
      RegistryNode child = _children.get(i);

      if (child._name.equals(name))
	return child;
    }

    return null;
  }

  /**
   * Returns the node's value interpreted as an integer
   *
   * @exception throws RegistryException if the value isn't a valid
   * integer
   */
  public int getInt()
    throws RegistryException
  {
    String value = getValue();

    if (value == null || value.equals("") || value.equals(getName()))
      return 0;
    else {
      try {
	return Integer.parseInt(value);
      } catch (Exception e) {
        throw error(e);
      }
    }
  }

  /**
   * Returns a subnode's value interpreted as an integer
   *
   * @param path the path to the subnode
   * @param deflt default value for the int
   *
   * @return the specified value or the default if none specified.
   *
   * @exception throws RegistryException if the value isn't a valid
   * integer
   */
  public int getInt(String path, int deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getInt();
  }

  /**
   * Returns the node's value interpreted as a double.
   *
   * @exception throws RegistryException if the value isn't a valid
   * double
   */
  public double getDouble()
    throws RegistryException
  {
    String value = getValue();

    if (value == null || value.equals("") || value.equals(getName()))
      return 0;
    else {
      try {
	return Double.valueOf(value).doubleValue();
      } catch (Exception e) {
        
	throw error(e);
      }
    }
  }

  /**
   * Returns a subnode's value interpreted as a double
   *
   * @param path the path to the subnode
   * @param deflt default value for the double
   *
   * @return the specified value or the default if none specified.
   *
   * @exception throws RegistryException if the value isn't a valid
   * double
   */
  public double getDouble(String path, double deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getDouble();
  }

  /**
   * Returns the node's value interpreted as a string.
   */
  public String getString()
  {
    String value = getValue();

    return value;
  }

  /**
   * Returns a subnode's value interpreted as a string.
   *
   * @param path the path to the subnode
   * @param deflt default value for the string
   *
   * @return the specified value or the default if none specified.
   */
  public String getString(String path, String deflt)
  {
    RegistryNode node = lookup(path);
    if (node == null)
      return deflt;
    else
      return node.getString();
  }

  /**
   * Returns the node's value interpreted as a path.
   * <code>getPath</code> automatically looks up variables in
   * the System.properties so $foo/bar.xml resolves to the value of $foo.
   */
  public String getPath()
  {
    String value = getValue();

    if (value.startsWith("$")) {
      int p = value.indexOf('/');
      
      if (p > 0) {
        String name = value.substring(1, p);
        String property = System.getProperty(name);

        if (property != null)
          value = property + value.substring(p);
      }
      else {
        String name = value.substring(1);
        String property = System.getProperty(name);
        
        if (property != null)
          value = property;
      }
    }

    return value;
  }

  /**
   * Returns a subnode's value interpreted as a path.
   *
   * @param path the path to the subnode
   * @param deflt default value for the string
   *
   * @return the specified value or the default if none specified.
   */
  public String getPath(String path, String deflt)
  {
    RegistryNode node = lookup(path);
    if (node == null)
      return deflt;
    else
      return node.getPath();
  }

  public QDate getDate()
    throws RegistryException
  {
    String value = getValue();

    throw error("no date");
  }

  public QDate getDate(String path, QDate deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getDate();
  }

  /**
   * Returns the value as a period.
   *
   * <table>
   * <tr><td>s<td>seconds
   * <tr><td>m<td>minutes
   * <tr><td>h<td>hours
   * <tr><td>D<td>days
   * <tr><td>W<td>weeks
   * <tr><td>M<td>months
   * <tr><td>Y<td>years
   * </table>
   *
   * @return the period in milliseconds, 0 if no period.
   *
   * @exception throws RegistryException if the value isn't a valid
   * period
   */
  public long getPeriod()
    throws RegistryException
  {
    try {
      return calculatePeriod(getValue());
    } catch (RegistryException e) {
      throw error(e.getMessage());
    }
  }

  /**
   * Converts a period string to a time.
   *
   * <table>
   * <tr><td>s<td>seconds
   * <tr><td>m<td>minutes
   * <tr><td>h<td>hours
   * <tr><td>D<td>days
   * <tr><td>W<td>weeks
   * <tr><td>M<td>months
   * <tr><td>Y<td>years
   * </table>
   */
  public static long calculatePeriod(String value)
    throws RegistryException
  {
    try {
      return Period.toPeriod(value);
    } catch (Exception e) {
      throw new RegistryException(e.getMessage());
    }
  }

  /**
   * Returns the value as a period.
   *
   * <table>
   * <tr><td>s<td>seconds
   * <tr><td>m<td>minutes
   * <tr><td>h<td>hours
   * <tr><td>D<td>days
   * <tr><td>W<td>weeks
   * <tr><td>M<td>months
   * <tr><td>Y<td>years
   * </table>
   *
   * @param path the path to the configuration subnode
   * @param deflt default period
   *
   * @return the period in milliseconds, 0 if no period.
   *
   * @exception throws RegistryException if the value isn't a valid
   * period
   */
  public long getPeriod(String path, long deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getPeriod();
  }

  /**
   * Calculates the next period end.
   */
  public static long periodEnd(long now, long period)
  {
    if (period < 0)
      return Long.MAX_VALUE;
    else if (period == 0)
      return now;

    if (period < 30 * DAY)
      return now + (period - (now + 4 * DAY) % period);

    if (period % (30 * DAY) == 0) {
      int months = (int) (period / (30 * DAY));

      synchronized (_calendar) {
        _calendar.setGMTTime(now);
        long year = _calendar.getYear();
        int month = _calendar.getMonth();

        _calendar.setGMTTime(0);
      
        return _calendar.setDate(year, month + months, 0);
      }
    }

    if (period % (365 * DAY) == 0) {
      long years = (period / (365 * DAY));

      synchronized (_calendar) {
        _calendar.setGMTTime(now);
        long year = _calendar.getYear();

        _calendar.setGMTTime(0);

        long newYear = year + (years - year % years);
      
        return _calendar.setDate(newYear, 0, 0);
      }
    }
    
    return now + (period - (now + 4 * DAY) % period);
  }

  /**
   * Returns a network
   *
   * <pre>
   * 192.168.9.0/24
   * </pre>
   */
  public InetNetwork getNetwork(String path, InetNetwork deflt)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;
    else
      return node.getNetwork();
  }

  /**
   * Returns a network
   *
   * <pre>
   * 192.168.9.0/24
   * </pre>
   */
  public InetNetwork getNetwork()
    throws RegistryException
  {
    try {
      return parseNetwork(getValue());
    } catch (RegistryException e) {
      throw error(e.getMessage());
    }
  }
    
  /**
   * Parses a network
   *
   * <pre>
   * 192.168.9.0/24
   * </pre>
   *
   * @return the InetNetwork
   *
   * @exception throws RegistryException if the value isn't a valid
   * period
   */
  public static InetNetwork parseNetwork(String network)
    throws RegistryException
  {
    return InetNetwork.create(network);
  }

  /**
   * Returns an boolean for a node, allowing EL expressions
   *
   * @param env the variable environment
   */
  public boolean getELBoolean(VariableResolver env)
    throws RegistryException
  {
    String value = getValue();

    if (value == null || value.equals("") || value.equals(getName()))
      return true;
    else if (value.equals("true") || value.equals("yes"))
      return true;
    else if (value.equals("false") || value.equals("no"))
      return false;

    try {
      Expr expr = new ELParser(value).parse();

      return expr.evalBoolean(env);
    } catch (Exception e) {
      throw error(e);
    }
  }

  /**
   * Returns a string for a node, allowing EL expressions
   *
   * @param path the registry path
   */
  public String getELString(VariableResolver env)
    throws RegistryException
  {
    try {
      String value = getValue();

      if (value == null || value.equals(""))
        return value;
      
      Expr expr = new ELParser(value).parse();

      return expr.evalString(env);
    } catch (Exception e) {
      throw error(e);
    }
  }

  /**
   * Returns an boolean for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public boolean getELBoolean(String path,
                              boolean deflt,
                              VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    try {
      return node.getELBoolean(env);
    } catch (Exception e) {
      throw error(e);
    }
  }

  /**
   * Returns a string for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public String getELString(String path,
                            String deflt,
                            VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    return node.getELString(env);
  }

  /**
   * Returns an integer for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public int getELInt(String path,
                      int deflt,
                      VariableResolver env)
    throws RegistryException
  {
    return (int) getELLong(path, deflt, env);
  }

  /**
   * Returns a long for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public long getELLong(String path,
                        long deflt,
                        VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    try {
      Expr expr = new ELParser(node.getString()).parse();

      return expr.evalLong(env);
    } catch (Exception e) {
      throw error(e);
    }
  }

  /**
   * Returns a double for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public double getELDouble(String path,
                            double deflt,
                            VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    try {
      Expr expr = new ELParser(node.getString()).parse();

      return expr.evalDouble(env);
    } catch (Exception e) {
      throw error(e);
    }
  }

  /**
   * Returns a Path for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public Path getELPath(String path,
                        Path deflt,
                        VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    try {
      Expr expr = new ELParser(node.getString()).parse();

      Object obj = expr.evalObject(env);

      if (obj instanceof Path)
        return (Path) obj;

      String value = Expr.toString(obj, null);
      Path pwd = (Path) env.resolveVariable("resin:pwd");
      
      if (pwd != null)
        return pwd.lookup(value);
      else
        return Vfs.lookup(value);
    } catch (Exception e) {
      throw node.error(e);
    }
  }

  /**
   * Returns a period for a node, allowing EL expressions
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public long getELPeriod(String path,
                          long deflt,
                          VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    try {
      Expr expr = new ELParser(node.getString()).parse();

      return expr.evalPeriod(env);
    } catch (Exception e) {
      throw new RegistryException(e);
    }
  }

  /**
   * Returns a number of bytes, allowing EL expressions.  Default is bytes.
   *
   * <pre>
   * b  : bytes
   * k  : kilobytes
   * kb : kilobytes
   * m  : megabytes
   * mb : megabytes
   * g  : gigabytes
   * </pre>
   *
   * @param path the registry path
   * @param deflt the default value
   * @param env the variable environment
   */
  public long getELBytes(String path,
                          long deflt,
                          VariableResolver env)
    throws RegistryException
  {
    RegistryNode node = lookup(path);

    if (node == null)
      return deflt;

    String bytes;
      
    try {
      Expr expr = new ELParser(node.getString()).parse();

      bytes = expr.evalString(env);
    } catch (Exception e) {
      throw new RegistryException(e);
    }

    long value = 0;
    long sign = 1;
    int i = 0;
    int length = bytes.length();

    if (length == 0)
      return -1;
    
    if (bytes.charAt(i) == '-') {
      sign = -1;
      i++;
    }
    else if (bytes.charAt(i) == '+') {
      i++;
    }

    if (length <= i)
      return -1;

    int ch;
    for (; i < length && (ch = bytes.charAt(i)) >= '0' && ch <= '9'; i++)
      value = 10 * value + ch - '0';

    value = sign * value;
    
    if (bytes.endsWith("gb") || bytes.endsWith("g")) {
      return value * 1024 * 1024 * 1024;
    }
    else if (bytes.endsWith("mb") || bytes.endsWith("m")) {
      return value * 1024 * 1024;
    }
    else if (bytes.endsWith("kb") || bytes.endsWith("k")) {
      return value * 1024;
    }
    else if (bytes.endsWith("b")) {
      return value;
    }
    else {
      return value;
    }
  }

  /**
   * Returns an iterator over the node's children.
   */
  public Iterator<RegistryNode> iterator()
  {
    return new RegistryIterator();
  }

  /**
   * Returns an iterator over the node's children which match the key.
   *
   * @param key the key for the children to match
   */
  public Iterator<RegistryNode> select(String key)
  {
    return new RegistryIterator(key);
  }

  public List<RegistryNode> getChildren()
  {
    return _children;
  }

  public boolean hasChildren()
  {
    return _children != null && _children.size() > 0;
  }

  /**
   * Adds a new node as a child of the current node.
   *
   * @param name the child's key
   * @param value the child's value
   * @param filename the source filename for the child node
   * @param line the source line for the child node
   *
   * @return the new child node
   */
  RegistryNode add(String name, String value, String filename, int line)
  {
    RegistryNode child = new RegistryNode(_root, name, value, filename, line);
    if (_children == null)
      _children = new ArrayList<RegistryNode>();

    _children.add(child);
    child._parent = this;

    return child;
  }

  /**
   * Appends a new RegistryNode as the child of the current node.
   *
   * @param child the new child.
   *
   * @return the new child.
   */
  RegistryNode add(RegistryNode child)
  {
    if (_children == null)
      _children = new ArrayList<RegistryNode>();
    
    _children.add(child);
    child._parent = this;

    return child;
  }

  private RegistryNode getLink(String name, String value)
  {
    int length = _children == null ? 0 : _children.size();

    for (int i = 0; i < length; i++) {
      RegistryNode child = _children.get(i);

      if (child._name.equals(name) && 
	  child._value != null && child._value.equals(value))
	return child;
    }

    return null;
  }


  /**
   * Returns a configuration error for a given configuration line number.
   *
   * @param msg the error message.
   */
  public RegistryException error(String msg)
  {
    return new RegistryException(getErrorMessage(msg));
  }


  /**
   * Returns a configuration error for a given configuration line number.
   *
   * @param node the configuration node throwing the error.
   * @param e exception thrown for that node.
   */
  public RegistryException error(Exception e)
  {
    if (e instanceof RegistryException)
      return (RegistryException) e;
    else
      return new RegistryException(getErrorMessage(e.toString()), e);
  }

  /**
   * Adds the filename and line to the error message.
   *
   * <pre>
   * resin.conf:14: unknown tag `foo'
   * </pre>
   *
   * @param message the error message
   *
   * @return the error message with filename and line number.
   */
  public String getErrorMessage(String message)
  {
    return _filename + ":" + _line + ": " + message;
  }

  /**
   * Prints the RegistryNode tree for debugging.
   *
   * @param os the write stream to print the tree to.
   */
  public void print(WriteStream os) throws IOException
  {
    print(os, 0);
  }

  /**
   * Recursively prints the node of the registry tree.
   *
   * @param os the write stream to print the tree to.
   * @param depth the current indentation depth
   */
  private void print(WriteStream os, int depth) throws IOException
  {
    printSpaces(os, depth);
    os.print("<" + getName());
    if (getValue() != null)
      os.print(" id=\"" + getValue() + "\"");
    int length = _children == null ? 0 : _children.size();
    boolean hasElementChildren = false;
    for (int i = 0; i < length; i++) {
      RegistryNode child = _children.get(i);

      if (! child.hasChildren())
        os.print(" " + child.getName() + "=\"" + child.getValue() + "\"");
      else {
        hasElementChildren = true;
        break;
      }
    }

    if (! hasElementChildren) {
      os.println("/>");
      return;
    }

    os.println(">");
    
    boolean printChild = false;
    for (int i = 0; i < length; i++) {
      RegistryNode child = _children.get(i);

      if (printChild || child.hasChildren()) {
        printChild = true;
        child.print(os, depth + 2);
      }
    }

    printSpaces(os, depth);
    os.println("</" + getName() + ">");
  }

  /**
   * Indents the specified number of spaces.
   *
   * @param os destination write stream
   * @param depth indentation depth.
   */
  private void printSpaces(WriteStream os, int depth) throws IOException
  {
    for (int i = 0; i < depth; i++)
      os.print(' ');
  }

  /**
   * Clones the current node.  Children and parents are not copied, just
   * assigned by reference.
   *
   * @return the cloned object.
   */
  public Object clone()
  {
    RegistryNode clone = new RegistryNode(_root, _name, _value,
                                          _filename, _line);

    clone._parent = _parent;

    if (_children != null) {
      clone._children = new ArrayList<RegistryNode>();
      for (int i = 0; i < _children.size(); i++)
        clone._children.add(_children.get(i));
    }

    return clone;
  }

  public String toString()
  {
    return "RegistryNode[" + _name + " " + _value + "]";
  }

  /**
   * An iterator of the children nodes
   */
  class RegistryIterator implements Iterator<RegistryNode> {
    int _i;
    String _key;

    public RegistryIterator()
    {
    }

    public RegistryIterator(String key)
    {
      _key = key;
      
      findNext();
    }
    
    private void findNext()
    {
      if (_key == null || _children == null)
	return;

      for (; _i < _children.size(); _i++) {
	RegistryNode registry = _children.get(_i);
	if (registry._name.equals(_key)) 
	  return;
      }
    }

    public boolean hasNext()
    {
      return (_children != null && _i < _children.size());
    }

    public RegistryNode next()
    {
      if (_children == null || _children.size() <= _i)
	return null;

      RegistryNode next = _children.get(_i);

      _i++;
      findNext();

      return next;
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
