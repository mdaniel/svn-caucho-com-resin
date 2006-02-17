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

package com.caucho.aop;

import java.util.ArrayList;

import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.JavaField;
import com.caucho.bytecode.JavaMethod;

import com.caucho.java.WorkDir;

import com.caucho.java.gen.JavaClassGenerator;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;

/**
 * Prepares a class for enhancement.
 */
public class AopPrepare {
  private static final L10N L = new L10N(AopPrepare.class);

  private static final int ACC_PUBLIC = 0x1;
  private static final int ACC_PRIVATE = 0x2;
  private static final int ACC_PROTECTED = 0x4;
  
  private JavaClassGenerator _javaGen = new JavaClassGenerator();

  private Path _workPath;

  private String _baseSuffix = "";
  private String _extSuffix = "__ResinExt";

  private boolean _isParentStarted;

  /**
   * Creates a new environment class loader.
   */
  public AopPrepare()
  {
  }

  /**
   * Gets the work path.
   */
  public Path getWorkPath()
  {
    if (_workPath != null)
      return _workPath;
    else
      return WorkDir.getLocalWorkDir();
  }

  /**
   * Sets the work path.
   */
  public void setWorkPath(Path workPath)
  {
    _workPath = workPath;
  }

  /**
   * Gets the work path.
   */
  public final Path getPreWorkPath()
  {
    return getWorkPath().lookup("pre-enhance");
  }

  /**
   * Gets the work path.
   */
  public final Path getPostWorkPath()
  {
    return getWorkPath().lookup("post-enhance");
  }

  /**
   * Moves the old class.
   */
  protected JavaClass renameClass(JavaClass jClass, String targetClass)
  {
    String cpOldName = jClass.getThisClass();
    String cpClassName = targetClass.replace('.', '/');
	  
    int utf8Index = jClass.getConstantPool().addUTF8(cpClassName).getIndex();
    jClass.getConstantPool().getClass(cpOldName).setNameIndex(utf8Index);

    jClass.setThisClass(cpClassName);

    // need to set descriptors, too

    // set private fields to protected
    ArrayList<JavaField> fields = jClass.getFieldList();
    for (int i = 0; i < fields.size(); i++) {
      JavaField field = fields.get(i);

      int accessFlags = field.getAccessFlags();

      if ((accessFlags & ACC_PRIVATE) != 0) {
	accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
	field.setAccessFlags(accessFlags);
      }
    }

    // set private methods to protected
    ArrayList<JavaMethod> methods = jClass.getMethodList();
    for (int i = 0; i < methods.size(); i++) {
      JavaMethod method = methods.get(i);

      int accessFlags = method.getAccessFlags();

      if ((accessFlags & ACC_PRIVATE) != 0) {
	accessFlags = (accessFlags & ~ ACC_PRIVATE) | ACC_PROTECTED;
	method.setAccessFlags(accessFlags);
      }
    }

    return jClass;
  }
}
