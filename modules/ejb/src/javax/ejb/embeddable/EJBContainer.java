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
 * @author Emil Ong
 */

package javax.ejb.embeddable;

import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.spi.EJBContainerProvider;

import javax.naming.Context;

/**
 * The main ejb context.
 */
public abstract class EJBContainer {
  public static final String APP_NAME = "javax.ejb.embeddable.appName";
  public static final String MODULES = "javax.ejb.embeddable.modules";
  public static final String PROVIDER = "javax.ejb.embeddable.provider";

  private static final String CAUCHO_PROVIDER_CLASS
    = "com.caucho.ejb.embeddable.EJBContainerProvider";

  public static EJBContainer createEJBContainer()
    throws EJBException
  {
    return createEJBContainer(null);
  }
  
  public static EJBContainer createEJBContainer(Map<?,?> properties)
    throws EJBException
  {
    Class cl = null;

    String providerClass = CAUCHO_PROVIDER_CLASS;

    if (properties != null && properties.get(PROVIDER) != null)
      providerClass = (String) properties.get(PROVIDER);

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      cl = Class.forName(providerClass, false, loader);

      if (! EJBContainerProvider.class.isAssignableFrom(cl)) {
        throw new EJBException("Provider class '" + providerClass + "' does not implement javax.ejb.spi.EJBContainerProvider");
      }

      EJBContainerProvider provider = (EJBContainerProvider) cl.newInstance();

      return provider.createEJBContainer(properties);
    }
    catch (IllegalAccessException e) {
      throw new EJBException(e);
    }
    catch (InstantiationException e) {
      throw new EJBException(e);
    }
    catch (ClassNotFoundException e) {
      throw new EJBException(e);
    }
  }
  
  public abstract Context getContext();
  public abstract void close();
}
