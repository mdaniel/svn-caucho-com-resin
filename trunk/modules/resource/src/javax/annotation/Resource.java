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

package javax.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The resource annotation.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, FIELD, METHOD})
public @interface Resource {
  public enum AuthenticationType {
    CONTAINER,
    APPLICATION
  }

  AuthenticationType authenticationType()
    default AuthenticationType.CONTAINER;

  String description() default "";

  /**
   * JNDI name.
   */
  String name() default "";

  String lookup() default "";

  boolean shareable() default true;

  /**
   * Java type of the resource.
   */
  Class type() default Object.class;

  /**
   * Product-specific name.
   */
  String mappedName() default "";
}
