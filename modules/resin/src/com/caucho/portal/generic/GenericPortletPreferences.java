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

import javax.portlet.*;

import java.util.*;
import java.io.IOException;

public class GenericPortletPreferences implements PortletPreferences
{
  Map<String, PortletPreference> _preferenceMap;
  ArrayList<PreferencesValidator> _preferencesValidators;

  public void addPreference(PortletPreference preference)
  {
    if (_preferenceMap == null)
      _preferenceMap = new LinkedHashMap<String, PortletPreference>();

    _preferenceMap.put(preference.getName(), preference);
  }

  public void addPreferencesValidator(String className)
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class cl = Class.forName(className, false, loader);
      PreferencesValidator validator = (PreferencesValidator) cl.newInstance();

      if (_preferencesValidators == null)
        _preferencesValidators = new ArrayList<PreferencesValidator>();

      _preferencesValidators.add(validator);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }

  }

  public ArrayList<PreferencesValidator> getPreferencesValidators()
  {
    return _preferencesValidators;
  }
  
  public boolean isReadOnly(String key)
  {
    if (_preferenceMap == null)
      return false;

    PortletPreference preference = _preferenceMap.get(key);

    if (preference == null)
      return false;

    return preference.isReadOnly();
  }

  public String getValue(String key, String def)
  {
    if (_preferenceMap == null)
      return def;

    PortletPreference preference = _preferenceMap.get(key);

    if (preference == null)
      return def;

    return preference.getValue();
  }

  public String[] getValues(String key, String[] def)
  {
    if (_preferenceMap == null)
      return def;

    PortletPreference preference = _preferenceMap.get(key);

    if (preference == null)
      return def;

    return preference.getValues();
  }

  public Enumeration getNames()
  {
    if (_preferenceMap == null)
      return Collections.enumeration(Collections.EMPTY_LIST);
    else
      return Collections.enumeration(_preferenceMap.keySet());
  }

  public Map getMap()
  {
    return _preferenceMap;
  }

  public void reset(String key) 
    throws ReadOnlyException
  {
    throw new UnsupportedOperationException();
  }

  public void setValue(String key, String value)  
    throws ReadOnlyException
  {
    throw new UnsupportedOperationException();
  }

  public void setValues(String key, String[] values) 
    throws ReadOnlyException
  {
    throw new UnsupportedOperationException();
  }

  public void store() 
    throws IOException, ValidatorException
  {
    throw new UnsupportedOperationException();
  }
}




