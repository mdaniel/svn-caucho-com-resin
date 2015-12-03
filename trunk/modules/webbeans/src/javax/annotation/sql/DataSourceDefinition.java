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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.annotation.sql;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Defines a custom datasource
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface DataSourceDefinition {
  public String className();
  public String name();

  public String description() default "";
  public String url() default "";
  public String user() default "";
  public String password() default "";
  public String databaseName() default "";
  public int portNumber() default -1;
  public String serverName() default "localhost";
  public int isolationLevel() default -1;
  public boolean transactional() default true;
  public int initialPoolSize() default -1;
  public int maxPoolSize() default -1;
  public int minPoolSize() default -1;
  public int maxIdleTime() default -1;
  public int maxStatements() default -1;
  public String []properties() default {};
  public int loginTimeout() default 0;
}
