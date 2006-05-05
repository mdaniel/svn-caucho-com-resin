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

package com.caucho.quercus.program;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusRuntimeException;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.env.*;

import com.caucho.quercus.expr.Expr;

import com.caucho.quercus.gen.PhpWriter;

/**
 * Represents a Quercus class definition
 */
abstract public class ClassDef {
  private final static L10N L = new L10N(ClassDef.class);
  
  private final String _name;
  private final String _parentName;

  protected ClassDef(String name, String parentName)
  {
    _name = name;
    _parentName = parentName;
  }
  
  /**
   * Returns the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the parent name.
   */
  public String getParentName()
  {
    return _parentName;
  }

  /**
   * Initialize the quercus class.
   */
  public void initClass(QuercusClass cl)
  {
  }

  /**
   * Creates a new instance.
   */
  public Value newInstance(Env env, QuercusClass qcl)
    throws Throwable
  {
    return new ObjectExtValue(qcl);
  }

  /**
   * Returns value for instanceof.
   */
  public boolean isA(String name)
  {
    return _name.equals(name);
  }

  /**
   * Returns the constructor
   */
  abstract public AbstractFunction findConstructor();

  /**
   * Finds the matching constant
   */
  public Expr findConstant(String name)
  {
    return null;
  }

  public String toString()
  {
    return "Class[" + getName() + "]";
  }
  
  public Set<Map.Entry<String, Expr>> fieldSet()
  {
  	return null;
  }
  
  public Set<Map.Entry<String, AbstractFunction>> functionSet()
  {
  	return null;
  }
}

