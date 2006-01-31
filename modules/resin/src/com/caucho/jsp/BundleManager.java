/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.jsp;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import java.net.URL;

import javax.servlet.jsp.*;
import javax.servlet.jsp.jstl.fmt.*;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.TimedCache;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;

import com.caucho.make.DependencyContainer;

/**
 * Manages i18n bundles
 */
public class BundleManager {
  private static final L10N L = new L10N(BundleManager.class);
  private static final Logger log = Log.open(BundleManager.class);

  static LocalizationContext NULL_BUNDLE =
    new LocalizationContext();

  private static EnvironmentLocal<BundleManager> _envBundle =
    new EnvironmentLocal<BundleManager>();

  private TimedCache<String,LocalizationContext> _bundleCache;

  private BundleManager()
  {
    long updateInterval = Environment.getDependencyCheckInterval();
    
    _bundleCache = new TimedCache(256, updateInterval);
  }

  /**
   * Returns the environment's bundle.
   */
  public static BundleManager create()
  {
    BundleManager manager;

    synchronized (_envBundle) {
      manager = _envBundle.get();
      if (manager == null) {
        manager = new BundleManager();
	_envBundle.set(manager);
      }
    }

    return manager;
  }
  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name, String cacheKey,
                                       Enumeration<Locale> locales)
  {
    LocalizationContext cachedValue = _bundleCache.get(cacheKey);

    if (cachedValue != null)
      return cachedValue == NULL_BUNDLE ? null : cachedValue;

    while (locales.hasMoreElements()) {
      Locale locale = locales.nextElement();

      LocalizationContext bundle = getBundle(name, locale);

      if (bundle != null) {
        _bundleCache.put(cacheKey, bundle);
        return bundle;
      }
    }

    _bundleCache.put(cacheKey, NULL_BUNDLE);

    return null;
  }

  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name, Locale locale)
  {
    String cacheName = (name + '_' +
                       locale.getLanguage() + '_' +
                       locale.getCountry() + '_' +
                       locale.getVariant());
    
    LocalizationContext bundle;

    bundle = _bundleCache.get(cacheName);
    
    if (bundle != null)
      return bundle != NULL_BUNDLE ? bundle : null;

    String fullName = cacheName;

    ResourceBundle resourceBundle;
    resourceBundle = getBaseBundle(fullName);
    if (resourceBundle != null) {
      bundle = new LocalizationContext(resourceBundle, locale);
      _bundleCache.put(cacheName, bundle);
      return bundle;
    }
      
    fullName = name + '_' + locale.getLanguage() + '_' + locale.getCountry();
    resourceBundle = getBaseBundle(fullName);
    if (resourceBundle != null) {
      bundle = new LocalizationContext(resourceBundle, locale);
      _bundleCache.put(cacheName, bundle);
      return bundle;
    }
    
    fullName = name + '_' + locale.getLanguage();
    resourceBundle = getBaseBundle(fullName);
    if (resourceBundle != null) {
      bundle = new LocalizationContext(resourceBundle, locale);
      _bundleCache.put(cacheName, bundle);
      return bundle;
    }

    _bundleCache.put(cacheName, NULL_BUNDLE);

    return null;
  }
  

  /**
   * Returns the named ResourceBundle.
   */
  public LocalizationContext getBundle(String name)
  {
    LocalizationContext bundle = _bundleCache.get(name);
    if (bundle != null)
      return bundle != NULL_BUNDLE ? bundle : null;
    
    ResourceBundle resourceBundle = getBaseBundle(name);
    if (resourceBundle != null) {
      bundle = new LocalizationContext(resourceBundle);
      _bundleCache.put(name, bundle);
      return bundle;
    }

    _bundleCache.put(name, NULL_BUNDLE);

    return null;
  }

  /**
   * Returns the base resource bundle.
   */
  private ResourceBundle getBaseBundle(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    try {
      Class cl = Class.forName(name, false, loader);

      if (cl != null) {
        ResourceBundle rb = (ResourceBundle) cl.newInstance();

        if (rb != null)
          return rb;
      }
    } catch (Throwable e) {
    }
    
    try {
      InputStream is = loader.getResourceAsStream('/' + name.replace('.', '/') + ".properties");

      if (is instanceof ReadStream) {
        Path path = ((ReadStream) is).getPath();
        Environment.addDependency(new Depend(path));
      }

      ResourceBundle bundle = new PropertyResourceBundle(is);

      is.close();

      return bundle;
    } catch (Exception e) {
    }

    return null;
  }
}
