/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.webbeans.cfg;

import com.caucho.bytecode.*;
import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.webbeans.WebBeans;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.annotation.PostConstruct;
import javax.webbeans.*;

/**
 * Configuration for the top-level web bean
 */
public class WbWebBeans {
  private static final Logger log
    = Logger.getLogger(WbWebBeans.class.getName());
  
  private WebBeans _webBeans;
  private Path _root;
  
  private ArrayList<WbComponentType> _componentTypeList;
  
  private ArrayList<WbComponent> _componentList
    = new ArrayList<WbComponent>();
  
  private ArrayList<WbInterceptor> _interceptorList
    = new ArrayList<WbInterceptor>();

  public WbWebBeans(WebBeans webBeans, Path root)
  {
    _webBeans = webBeans;
    _root = root;
  }
  
  /**
   * Gets the web beans root directory
   */
  public Path getRoot()
  {
    return _root;
  }

  /**
   * Adds a component.
   */
  public void addComponent(WbComponent comp)
  {
    _componentList.add(comp);
  }

  @PostConstruct
  public void init()
  {
    if (_componentTypeList == null) {
      _componentTypeList = new ArrayList<WbComponentType>();
      _componentTypeList.add(new WbComponentType(Standard.class, 0));
      _componentTypeList.add(new WbComponentType(Component.class, 1));
    }

    ByteCodeClassMatcher scanner = new ClassScanner();

    try {
      if (_root instanceof JarPath)
	scanForJarClasses(((JarPath) _root).getContainer(), scanner);
      else
	scanForClasses(_root, _root, scanner);
    } catch (IOException e) {
      throw new ConfigException(e);
    }
  }

  private void scanForClasses(Path root,
			      Path path,
			      ByteCodeClassMatcher scanner)
    throws IOException
  {
    if (path.isDirectory()) {
      for (String name : path.list())
	scanForClasses(root, path.lookup(name), scanner);

      return;
    }

    if (! path.getPath().endsWith(".class"))
      return;

    try {
      ReadStream is = path.openRead();
      try {
	byte []buffer = new byte[(int) path.getLength()];
	is.readAll(buffer, 0, buffer.length);

	ByteCodeClassScanner classScanner =
	  new ByteCodeClassScanner(path.getPath(),
				   buffer, 0, buffer.length,
				   scanner);

	if (classScanner.scan()) {
	  String rootName = root.getPath();
	  String name = path.getPath();
	  int p = name.lastIndexOf('.');

	  loadComponentType(name.substring(rootName.length(), p));
	}
      } finally {
	is.close();
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private void scanForJarClasses(Path path, ByteCodeClassMatcher scanner)
  {
    ZipFile zipFile = null;

    try {
      zipFile = new ZipFile(path.getNativePath());

      Enumeration<? extends ZipEntry> e = zipFile.entries();

      while (e.hasMoreElements()) {
	ZipEntry entry = e.nextElement();

	if (! entry.getName().endsWith(".class"))
	  continue;

	int size = (int) entry.getSize();
      
	ReadStream is = Vfs.openRead(zipFile.getInputStream(entry));
	try {
	  byte []buffer = new byte[size];
	  is.readAll(buffer, 0, buffer.length);
	  
	  ByteCodeClassScanner classScanner =
	    new ByteCodeClassScanner(path.getPath(),
				     buffer, 0, buffer.length,
				     scanner);

	  if (classScanner.scan()) {
	    String name = entry.getName();
	    int p = name.lastIndexOf('.');

	    loadComponentType(name.substring(0, p));
	  }
	} finally {
	  is.close();
	}
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (zipFile != null)
	  zipFile.close();
      } catch (Exception e) {
      }
    }
  }

  private void loadComponentType(String className)
  {
    try {
      if (className.startsWith("/"))
	className = className.substring(1);

      className = className.replace('/', '.');
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      Annotation compTypeAnn = null;
      Annotation scopeAnn = null;
      ArrayList<Annotation> bindList = new ArrayList<Annotation>();
      for (Annotation ann : cl.getAnnotations()) {
	if (ann.annotationType().isAnnotationPresent(ComponentType.class)) {
	  compTypeAnn = ann;
	}
	
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  scopeAnn = ann;
	}
	
	if (ann.annotationType().isAnnotationPresent(BindingType.class)) {
	  bindList.add(ann);
	}
      }
      
      WbComponentType type = null;
      for (int i = 0; i < _componentTypeList.size(); i++) {
	type = _componentTypeList.get(i);

	if (type.getType().equals(compTypeAnn.annotationType()))
	  break;
      }

      WbComponent component = new WbComponent();
      component.setClass(cl);
      component.setType(type);
      component.setFromClass(true);

      if (scopeAnn != null)
	component.setScopeAnnotation(scopeAnn);

      for (int i = 0; i < bindList.size(); i++) {
	Annotation bindAnn = bindList.get(i);
	
	WbBinding binding = new WbBinding(bindAnn);

	component.addBinding(binding);
      }

      _webBeans.addComponent(cl, component);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  class ClassScanner implements ByteCodeClassMatcher {
    /**
     * Returns true if the class is a match.
     */
    public boolean isClassMatch(String className)
    {
      return false;
    }
  
    /**
     * Returns true if the annotation class is a match.
     */
    public boolean isMatch(CharBuffer annotationClassName)
    {
      for (int i = 0; i < _componentTypeList.size(); i++) {
	WbComponentType type = _componentTypeList.get(i);

	if (annotationClassName.matches(type.getType().getName()))
	  return true;
      }
      
      return false;
    }
  }
}
