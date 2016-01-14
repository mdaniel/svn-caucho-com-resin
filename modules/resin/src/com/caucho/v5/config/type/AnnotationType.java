/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.type;

import java.lang.annotation.Annotation;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.L10N;

public class AnnotationType extends ConfigType<Annotation>
{
  protected static final L10N L = new L10N(AnnotationType.class);

  @Override
  public Class<Annotation> getType()
  {
    return Annotation.class;
  }
  
  /**
   * Returns the type's configured value
   *
   * @param builder the context builder
   * @param node the configuration node
   * @param parent
   */
  @Override
  public Object valueOf(String text)
  {
    return parseAnnotation(text);
  }

  /**
   * Parses the function signature.
   */
  private Annotation parseAnnotation(String signature)
    throws ConfigException
  {
    int p = signature.indexOf('(');
    
    String className;

    if (p > 0)
      className = signature.substring(0, p);
    else
      className = signature;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class<?> cl = Class.forName(className, false, loader);

      Annotation ann = (Annotation) cl.newInstance();

      return ann;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
}
