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

package com.caucho.doclet;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;

import com.caucho.log.Log;

import com.caucho.java.JavaWriter;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

/**
 * Top-level of the EJB doclet.
 */
public class XmlDoclet {
  private static final Logger log = Log.open(EjbDoclet.class);

  private JavaWriter _out;
  private int _depth;
  private boolean _isLineStart;

  private ArrayList<ClassDoc> _mainClasses;

  /**
   * Checks for known obtions.
   */
  public static int optionLength(String option)
  {
    if (option.equals("-path"))
      return 2;
    else
      return 0;
  }

  /**
   * Start of the doclet.
   */
  public static boolean start(RootDoc root)
  {
    String [][]options = root.options();

    Path path = null;

    for (int i = 0; i < options.length; i++) {
      String []args = options[i];

      if (args[0].equals("-path"))
	path = Vfs.lookup(args[1]);
    }

    if (path == null)
      throw new IllegalArgumentException("-path is expected");
    
    try {
      WriteStream os = path.openWrite();

      try {
        new XmlDoclet().printRootDoc(os, root);
      } finally {
        os.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return true;
  }

  private void printRootDoc(WriteStream os, RootDoc root)
    throws IOException
  {
    _out = new JavaWriter(os);

    _out.println("<doclet>");

    _mainClasses = new ArrayList<ClassDoc>();
    ArrayList<ClassDoc> classList = new ArrayList<ClassDoc>();
    
    ClassDoc []classes = root.specifiedClasses();

    for (int i = 0; i < classes.length; i++) {
      _mainClasses.add(classes[i]);

      addClasses(classList, classes[i]);
    }

    Collections.sort(classList, new ClassCmp());

    for (int i = 0; i < classList.size(); i++)
      printClassDoc(classList.get(i));
        
    _out.println("</doclet>");
  }

  private void addClasses(ArrayList<ClassDoc> classList, ClassDoc cl)
  {
    if (cl == null)
      return;
    if (classList.contains(cl))
      return;

    classList.add(cl);

    addClasses(classList, cl.superclass());

    ClassDoc []interfaces = cl.interfaces();
    for (int i = 0; i < interfaces.length; i++)
      addClasses(classList, interfaces[i]);
  }

  private void printClassDoc(ClassDoc classDoc)
    throws IOException
  {
    boolean isFull = _mainClasses.contains(classDoc);
    
    println("<class>");
    pushDepth();
    println("<name>" + classDoc.qualifiedName() + "</name>");
    printPosition(classDoc);

    ClassDoc superclass = classDoc.superclass();
    if (superclass != null) {
      println("<superclass>" + superclass.qualifiedName() + "</superclass>");
    }

    ClassDoc []interfaces = classDoc.interfaces();
    for (int i = 0; i < interfaces.length; i++) {
      println("<interface>" + interfaces[i].qualifiedName() + "</interface>");
    }

    if (isFull) {
      printTags(classDoc);
    
      MethodDoc []methods = classDoc.methods();

      for (int i = 0; i < methods.length; i++)
        printMethodDoc(methods[i]);
    }
    
    popDepth();
    println("</class>");
  }

  private void printMethodDoc(MethodDoc methodDoc)
    throws IOException
  {
    println("<method>");
    pushDepth();
    println("<name>" + methodDoc.name() + "</name>");
    printPosition(methodDoc);

    Type type = methodDoc.returnType();

    println("<return-type>" + type.qualifiedTypeName() + "</return-type>");    

    Parameter []parameters = methodDoc.parameters();
    for (int i = 0; i < parameters.length; i++) {
      println("<param>");
      pushDepth();
      println("<name>" + parameters[i].name() + "</name>");
      println("<type>" + parameters[i].typeName() + "</type>");
      popDepth();
      println("</param>");
    }

    printTags(methodDoc);

    popDepth();
    println("</method>");
  }

  private void printTags(Doc doc)
    throws IOException
  {
    Tag []tags = doc.tags();

    for (int i = 0; i < tags.length; i++) {
      println("<tag>");
      println("  <name>" + tags[i].name() + "</name>");
      print("  <text>");
      printEscape(tags[i].text());
      println("</text>");
      println("</tag>");
    }
  }

  private void printPosition(Doc doc)
    throws IOException
  {
    SourcePosition position = doc.position();

    if (position != null) {
      println("<position>" + position.file().toString() + ':' +
              position.line() + ": </position>");
    }
  }

  private void print(String s)
    throws IOException
  {
    _out.print(s);
  }
  
  private void println()
    throws IOException
  {
    _out.println();
  }
  
  private void println(String s)
    throws IOException
  {
    _out.println(s);
  }

  private void pushDepth()
    throws IOException
  {
    _out.pushDepth();
  }

  private void popDepth()
    throws IOException
  {
    _out.popDepth();
  }

  private void printEscape(String string)
    throws IOException
  {
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '<':
	_out.print("&lt;");
	break;
      case '>':
	_out.print("&gt;");
	break;
      case '&':
	_out.print("&amp;");
	break;
      default:
	_out.print((char) ch);
      }
    }
  }

  static class ClassCmp implements Comparator<ClassDoc> {
    public int compare(ClassDoc cl1, ClassDoc cl2)
    {
      if (cl1.equals(cl2))
        return 0;

      if (isAssignableFrom(cl1, cl2))
        return -1;
      
      if (isAssignableFrom(cl2, cl1))
        return 1;

      return 0;
    }

    private static boolean isAssignableFrom(ClassDoc cl1, ClassDoc cl2)
    {
      if (cl2 == null)
        return false;
      if (cl1.equals(cl2))
        return true;

      if (isAssignableFrom(cl1, cl2.superclass()))
        return true;

      ClassDoc []interfaces = cl2.interfaces();
      for (int i = 0; i < interfaces.length; i++)
        if (isAssignableFrom(cl1, interfaces[i]))
          return true;

      return false;
    }
  }
}
