/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */

package com.caucho.portal.generic;

import java.util.*;

public class ResourceBundleFactory
{
  private String _name;
  private Map<String,Object> _defaultMap;
  private Map<Locale, PortletResourceBundle> _bundleMap
    = new HashMap<Locale, PortletResourceBundle>();

  public ResourceBundleFactory() 
  {
    _defaultMap = new LinkedHashMap<String, Object>();
    addDefault("javax.portlet.title", "");
    addDefault("javax.portlet.short-title", "");
    addDefault("javax.portlet.keywords", "");
  }

  /**
   * Set a name for the parent resoruce bundle.  The name
   * is used for a call to ResourceBundle.getBundle()
   * for each locale.
   */
  public void setName(String name)
  {
    _name = name;
    _bundleMap.clear();
  }

  /**
   * Typical keys for defaults with portlets are:
   *
   * addDefault("javax.portlet.title", getPortletInfoTitle());
   * addDefault("javax.portlet.short-title", getPortletInfoShortTitle());
   * addDefault("javax.portlet.keywords", getPortletInfoKeywords());
   */ 
  public void addDefault(String key, String value)
  {
    _defaultMap.put(key, value);
  }

  public void addDefault(NameValuePair nameValuePair)
  {
    addDefault(nameValuePair.getName(), nameValuePair.getValue());
  }

  public ResourceBundle getResourceBundle(Locale locale)
  {
    PortletResourceBundle bundle = _bundleMap.get(locale);

    if (bundle == null) {
      bundle = new PortletResourceBundle(_name, locale, _defaultMap);
      _bundleMap.put(locale, bundle);
    }

    return bundle;
  }

  static class PortletResourceBundle extends ResourceBundle 
  {
    private Locale _locale;
    private Map<String, Object> _defaultMap;
    private ResourceBundle _parent;

    public PortletResourceBundle( String name, 
                                  Locale locale, 
                                  Map<String, Object> defaultMap )
    {
      _locale = locale;
      _defaultMap = defaultMap;

      if (name != null) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        _parent = ResourceBundle.getBundle(name, _locale, loader);

        super.setParent(_parent);
      }
    }

    public Locale getLocale()
    {
      return _parent == null ? _locale : _parent.getLocale();
    }

    public Object handleGetObject(String key)
    {
      if (_parent == null) {
        if (_defaultMap != null)
          return _defaultMap.get(key);
        else
          return null;
      }

      try {
        return _parent.getObject(key);
      }
      catch (MissingResourceException ex) {
        if (_defaultMap != null)
          return _defaultMap.get(key);
        else
          return null;
      }
    }

    public Enumeration getKeys()
    {
      if (_defaultMap != null)
        return Collections.enumeration(_defaultMap.keySet());
      else
        return Collections.enumeration(Collections.EMPTY_LIST);
    }
  }
}


