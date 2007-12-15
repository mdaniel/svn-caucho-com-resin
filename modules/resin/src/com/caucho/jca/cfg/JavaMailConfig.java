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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jca.cfg;

import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration for a javamail.
 */
public class JavaMailConfig {
  private static final L10N L = new L10N(JavaMailConfig.class);
  private static final Logger log = Log.open(JavaMailConfig.class);

  private Properties _props = new Properties();
  
  public JavaMailConfig()
  {
  }

  /**
   * Sets an attribute.
   */
  public void setAttribute(String name, String value)
  {
    _props.put(name, value);
  }

  public Object replaceObject()
    throws ConfigException
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class sessionClass = Class.forName("javax.mail.Session", false, loader);
      Class authClass = Class.forName("javax.mail.Authenticator", false, loader);

      Method method = sessionClass.getMethod("getInstance",
                                             new Class[] { Properties.class,
                                                           authClass });
      Object obj = method.invoke(null, new Object[] { _props, null });

      return obj;
    } catch (ClassNotFoundException e) {
      throw new ConfigException(L.l("javax.mail.Session is not available.  JavaMail must be downloaded separately from Sun."),
				e);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
