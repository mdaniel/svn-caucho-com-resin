/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.manager;

import javax.annotation.ManagedBean;
import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.ejb.EJBs;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import com.caucho.loader.enhancer.AbstractScanClass;
import com.caucho.vfs.Path;

/**
 * Environment-based container.
 */
class EjbScanClass extends AbstractScanClass {
  private static final char []STATELESS
    = javax.ejb.Stateless.class.getName().toCharArray();
  
  private static final char []STATEFUL
    = javax.ejb.Stateful.class.getName().toCharArray();
  
  private static final char []SINGLETON
    = javax.ejb.Singleton.class.getName().toCharArray();
  
  private static final char []MESSAGE_DRIVEN
    = javax.ejb.MessageDriven.class.getName().toCharArray();
  
  private static final char []MANAGED_BEAN
    = javax.annotation.ManagedBean.class.getName().toCharArray();
  
  private static final char []EJBS
    = javax.ejb.EJBs.class.getName().toCharArray();
  private static final int EJBS_LENGTH = 14;
  
  private static final char []DATA_SOURCE_DEFINITION
    = DataSourceDefinition.class.getName().toCharArray();
  
  private static final char []DATA_SOURCE_DEFINITIONS
    = DataSourceDefinitions.class.getName().toCharArray();
  
  private Path _root;
  private String _className;
  private EjbManager _ejbContainer;
  private boolean _isEjb;
  
  EjbScanClass(Path root, String className, EjbManager container)
  {
    _root = root;
    _className = className;
    _ejbContainer = container;
  }

  @Override
  public void addClassAnnotation(char[] buffer, int offset, int length)
  {
    if (isMatch(buffer, offset, length, STATELESS)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, STATEFUL)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, SINGLETON)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, MESSAGE_DRIVEN)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, MANAGED_BEAN)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, EJBS)) {
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, DATA_SOURCE_DEFINITION)) {
      // ejb/3042
      _isEjb = true;
    }
    else if (isMatch(buffer, offset, length, DATA_SOURCE_DEFINITIONS)) {
      _isEjb = true;
    }
  }

  @Override
  public void addInterface(char[] buffer, int offset, int length)
  {
  }

  @Override
  public void addPoolString(char[] buffer, int offset, int length)
  {
  }

  @Override
  public void addSuperClass(char[] buffer, int offset, int length)
  {
  }

  @Override
  public boolean finishScan()
  {
    if (_isEjb) {
      _ejbContainer.addScanClass(_root, _className);
      
      return true;
    }
    else
      return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _className + "]";
  }
}
