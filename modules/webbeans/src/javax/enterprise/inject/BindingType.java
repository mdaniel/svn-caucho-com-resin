/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package javax.enterprise.inject;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Custom binding annotations are marked with @BindingType
 * as a meta-annotation.
 *
 * <h2>Example: creating a custom binding type</h2>
 *
 * <code><pre>
 * package example;
 *
 * import static java.lang.annotation.ElementType.*;
 * import static java.lang.annotation.RetentionPolicy.Runtime;
 * import java.lang.annotation.*;
 *
 * import javax.enterprise.inject.BindingType;
 *
 * {@literal @BindingType}
 * {@literal @Documented}
 * Target({TYPE, METHOD, FIELD, PARAMETER})
 * Retention(RUNTIME)
 * public {@literal @interface} MyBinding {
 * }
 * </pre></code>
 *
 * <h2>Example: injecting a servlet using a custom binding type</h2>
 *
 * <code><pre>
 * package example;
 *
 * import example.MyBinding;
 * import javax.servlet.*;
 * import java.io.*;
 *
 * public class MyServlet extends GenericServlet {
 *   {@literal @MyBinding} MyBean _bean;
 *
 *   public void service(ServletRequest req, ServletResponse res)
 *     throws IOException
 *   {
 *     PrintWriter out = res.getWriter();
 *
 *     out.println("my-bean: " + _bean);
 *   }
 * }
 * </pre></code>
 *
 * <h2>Example: configuring using a custom binding type</h2>
 *
 * META-INF/beans.xml
 *
 * <code><pre>
 * &lt;Beans xmlns="urn:java:ee" xmlns:example="urn:java:example">
 *
 *   &lt;example:MyBean>
 *     &lt;example:MyBinding/>
 *   &lt;/example:MyBean>
 *
 * &lt;/Beans>
 * </pre></code>
 */
@Documented  
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface BindingType {
}
