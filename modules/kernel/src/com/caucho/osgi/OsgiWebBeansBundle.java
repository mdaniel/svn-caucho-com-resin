/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.osgi;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.SingletonBean;
import com.caucho.config.manager.InjectManager;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.Loader;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.config.Names;
import com.caucho.webbeans.manager.CurrentLiteral;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.jar.*;
import java.util.zip.*;

import org.osgi.framework.*;

/**
 * An osgi-bundle for exporting web-beans events
 */
public class OsgiWebBeansBundle extends AbstractOsgiBundle
  implements ServiceListener
{
  private static final L10N L = new L10N(OsgiWebBeansBundle.class);
  private static final Logger log
    = Logger.getLogger(OsgiWebBeansBundle.class.getName());

  private InjectManager _webBeans;

  OsgiWebBeansBundle(OsgiManager manager)
  {
    super(manager, "WebBeansBundle");

    _webBeans = InjectManager.create();
  }

  @Override
  protected ClassLoader getClassLoader()
  {
    return getManager().getParentLoader();
  }

  //
  // Bundle API
  //

  /**
   * Start the bundle
   */
  public void start()
    throws BundleException
  {
    getManager().addServiceListener(this, this, null);
  }

  //
  // service listener
  //
  
  /**
   * Called on a service event
   */
  public void serviceChanged(ServiceEvent event)
  {
    ServiceReference ref = event.getServiceReference();
    int eventType = event.getType();

    if (eventType == ServiceEvent.UNREGISTERING) {
      // XXX: need to remove from WebBeans
      return;
    }

    try {
      Object service = getService(ref);

      String []classNames = (String []) ref.getProperty("objectClass");
      Class []types = new Class[classNames.length];

      for (int i = 0; i < types.length; i++)
	types[i] = ref.getBundle().loadClass(classNames[i]);

      String name = (String) ref.getProperty("javax.webbeans.Named");

      ArrayList<Annotation> bindingList
	= new ArrayList<Annotation>();

      bindingList.add(new CurrentLiteral());
      if (name != null)
	bindingList.add(Names.create(name));

      Annotation []bindings = new Annotation[bindingList.size()];
      bindingList.toArray(bindings);

      SingletonBean bean = new SingletonBean(service, null, name,
					     bindings, types);
    
      _webBeans.addBean(bean);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + getBundleId()
	    + "," + getSymbolicName()
	    + "," + getLocation() + "]");
  }
}
