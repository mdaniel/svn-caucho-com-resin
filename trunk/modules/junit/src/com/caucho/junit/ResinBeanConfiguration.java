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

package com.caucho.junit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * Creates a Resin bean container custom configuration for a given JUnit test.
 * </p>
 * <p>
 * If no beans.xml files are specified, the default META-INF/beans.xml will be
 * loaded. If one or more beans.xml files are specified, the default beans.xml
 * file will be ignored. An array of beans.xml files may be specified. Multiple
 * beans.xml are combined together to create the final test configuration.
 * </p>
 * <p>
 * If no persistence.xml file is specified, the default persistence.xml will be
 * loaded. If a persistence.xml file is specified, the default file will be
 * ignored in favor of the specified JPA configuration.
 * </p>
 * <p>
 * Unless otherwise specified, files are loaded from the class-path. You can
 * explicitly specify that a file is loaded from the class-path by using the
 * 'classpath:' prefix (e.g. classpath:META-INF/beans.xml). You may specify that
 * a file is loaded from the file-system using the 'file:' prefix (e.g.
 * file:web/WEB-INF/beans.xml). Note that you can use either absolute or
 * relative file paths (e.g. file:web/WEB-INF/beans.xml or
 * file:/workspace/projectabc/tests/test123/web/WEB-INF/beans.xml).
 * </p>
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface ResinBeanConfiguration {
  /**
   * Specifies .jar files and directories with classes to add dynamically to
   * classpath. Specified elements will also be scanned for Managed Beans (CDI)
   *
   * @return
   */
  String[] classPath() default {};

  String[] beansXml() default {};

  String persistenceXml() default "";
}
