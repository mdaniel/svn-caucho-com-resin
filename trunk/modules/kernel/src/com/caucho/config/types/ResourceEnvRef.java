/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.types;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.FieldValueProgram;
import com.caucho.config.program.MethodValueProgram;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.lang.reflect.*;

/**
 * Configuration for the init-param pattern.
 */
public class ResourceEnvRef extends BaseRef {
  private static L10N L = new L10N(ResourceEnvRef.class);

  private String _name;
  private Class _type;


  public ResourceEnvRef()
  {
  }

  public ResourceEnvRef(Path modulePath, String sourceEjbName)
  {
    super(modulePath, sourceEjbName);
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Sets the name
   */
  public void setResourceEnvRefName(String name)
  {
    _name = name;
  }

  /**
   * Sets the type
   */
  public void setResourceEnvRefType(Class type)
  {
    _type = type;
  }

  /**
   * Initialize the resource.
   */
  // XXX TCK: @PostConstruct, needs QA, called from EjbConfig.
  /*
  public void initBinding(AbstractServer ejbServer)
    throws Exception
  {
    String jndiName = null;

    // XXX TCK: merge this code with the one in InjectIntrospector.
    if (javax.ejb.SessionContext.class.equals(_type)) {
      jndiName = "java:comp/env/sessionContext";
    }

    Object targetValue = null;

    if (jndiName != null) {
      targetValue = Jndi.lookup(jndiName);
    }

    // XXX TCK, needs QA
    if (_injectionTarget != null && targetValue != null) {
      String className = _injectionTarget.getInjectionTargetClass();
      String fieldName = _injectionTarget.getInjectionTargetName();

      if (ejbServer != null) {
        Class cl = getJavaClass(className);

        AccessibleObject field = getFieldOrMethod(cl, fieldName);

        if (field != null) {
          ConfigProgram program;

          if (field instanceof Method)
            program = new MethodValueProgram((Method) field, targetValue);
          else
            program = new FieldValueProgram((Field) field, targetValue);

          ejbServer.getInitProgram().addProgram(program);
        }
      }
    }
  }
  */

  public String toString()
  {
    return "ResourceEnvRef[" + _name + "]";
  }
}
