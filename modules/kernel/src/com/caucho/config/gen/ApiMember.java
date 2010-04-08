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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.gen;

import com.caucho.config.reflect.AnnotatedElementImpl;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.enterprise.inject.spi.Annotated;

/**
 * Represents an introspected method.
 */
abstract public class ApiMember extends AnnotatedElementImpl {
  private ApiClass _declaringClass;
  
  /**
   * Creates a new method.
   *
   * @param topClass the top class
   * @param method the introspected method
   */
  public ApiMember(ApiClass declaringClass,
		   Type type,
		   Annotated annotated,
		   Annotation []annotations)
  {
    super(type, annotated, annotations);

    _declaringClass = declaringClass;
  }

  /**
   * Returns the declaring ApiClass
   */
  public ApiClass getDeclaringClass()
  {
    return _declaringClass;
  }

  abstract public Member getJavaMember();
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJavaMember() + "]";
  }
}
