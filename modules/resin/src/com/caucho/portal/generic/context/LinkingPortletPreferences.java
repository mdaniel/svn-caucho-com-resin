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

package com.caucho.portal.generic.context;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;
import java.io.IOException;

/**
 * An implementation of PortletPreferences that stores values temporarily,
 * retrieves values from a store or defaults, validates before
 * before storing, and stores the values into the store.
 */
public class LinkingPortletPreferences 
  implements PortletPreferences
{
  static protected final Logger log = 
    Logger.getLogger(LinkingPortletPreferences.class.getName());

  static private String[] DUMMY = new String[] { "<dummy>" };
  static private String[] DELETED = new String[] { "<deleted>" };

  private PortletPreferences _defaults;
  private ArrayList<PreferencesValidator> _validators;
  private Map<String, String[]> _storeMap;

  private Map<String,String[]> _valueMap;

  public LinkingPortletPreferences()
  {
  }

  public void start( PortletPreferences defaults, 
                     ArrayList<PreferencesValidator> validators,
                     Map<String, String[]> storeMap )
  {
    if (_defaults != null || _storeMap != null)
      throw new IllegalStateException("missing finish()?");

    _defaults = defaults;
    _validators = validators;
    _storeMap = storeMap;
  }

  public void finish()
  {
    if (_valueMap != null)
      _valueMap.clear();

    _defaults = null;
    _validators = null;
    _storeMap = null;
  }

  public PortletPreferences getDefaults()
  {
    return _defaults;
  }

  public ArrayList<PreferencesValidator> getValidators()
  {
    return _validators;
  }

  public Map<String, String[]> getStore()
  {
    return _storeMap;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isReadOnly(String key)
  {
    boolean r = false;

    if (_defaults != null && _defaults.isReadOnly(key))
      return true;
    else
      return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getValue(String key, String def)
  {
    String[] values = getValues(key, DUMMY);

    if (values == DUMMY)
      return def;
    else
      return (values == null || values.length == 0) ? null : values[0];
  }

  /**
   * {@inheritDoc}
   */
  public String[] getValues(String key, String[] def)
  {
    String[] v = _valueMap == null ? null : _valueMap.get(key);

    if (v != DELETED) {
      if (v != null || (_valueMap != null && _valueMap.containsKey(key)))
        return v;
    }

    if (_defaults != null)
      def = _defaults.getValues(key, def);

    if (_storeMap != null) {
      String[] storeValues = _storeMap.get(key);

      if (storeValues != null)
        def = storeValues;
    }

    return def;
  }

  /**
   * {@inheritDoc}
   */
  public void setValue(String key, String value)  
    throws ReadOnlyException
  {
    setValues(key, value == null ? null : new String[] { value });
  }

  /**
   * {@inheritDoc}
   */
  public void setValues(String key, String[] values) 
    throws ReadOnlyException
  {
    if (isReadOnly(key))
      throw new ReadOnlyException("key `" + key + "'");
    else {
      if (_valueMap == null)
        _valueMap = new LinkedHashMap<String, String[]>();

      _valueMap.put(key, values);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void reset(String key) 
    throws ReadOnlyException
  {
    if (_storeMap != null && _storeMap.containsKey(key))
      setValues(key, DELETED);
    else if (isReadOnly(key))
      throw new ReadOnlyException("key `" + key + "'");
  }

  /**
   * {@inheritDoc}
   *
   * Returns the unique set of names in this
   * object, the backing store, and the default preferences.
   */
  public Enumeration getNames()
  {
    // XXX: this would be better implemented as a class extending
    // Iterator<String> which could also be used by getMap() below

    // the assumption here is that this will not be called very often,
    // and when it is a sorted list of names is valuable

    TreeSet<String> names = new TreeSet<String>();

    if (_valueMap != null) {
      Iterator<Map.Entry<String, String[]>> iter 
        = _valueMap.entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry<String, String[]> entry = iter.next();

        String key = entry.getKey();
        String[] value = entry.getValue();

        if (value != DELETED)
          names.add(key);
      }
    }

    if (_storeMap != null) {
      Iterator<String> iter = _storeMap.keySet().iterator();

      while (iter.hasNext()) {
        String key = iter.next();

        if (_valueMap != null && _valueMap.get(key) == DELETED)
          continue;

        names.add(key);
      }
    }

    if (_defaults != null) {
      Enumeration e = _defaults.getNames();

      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();

        if (_valueMap != null && _valueMap.get(key) == DELETED)
          continue;

        names.add(key);
      }
    }

    return Collections.enumeration(names);
  }

  /**
   * {@inheritDoc}
   */
  public Map getMap()
  {
    // XXX: this would be better as a custom implementation of AbstractMap

    Map<String,String[]> map = new HashMap<String,String[]>();

    Enumeration e = getNames();

    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      map.put(key, getValues(key, null));
    }

    return map;
  }


  /**
   * {@inheritDoc}
   *
   * This implementation first invokes the validators (if any), and then
   * propogates the properties set in this object to the the store.
   * If the store has not been set then only the invoking of the validators is
   * performed, the values are left unchanged.
   */
  public void store() 
    throws IOException, ValidatorException
  {
    if (_validators != null) {
      for (int i = 0; i < _validators.size(); i--) {
        _validators.get(i).validate(this);
      }
    }

    if (_storeMap != null && _valueMap != null) {
      Iterator<Map.Entry<String, String[]>> iter 
        = _valueMap.entrySet().iterator();

      while (iter.hasNext()) {
        Map.Entry<String, String[]> entry = iter.next();

        String key = entry.getKey();
        String[] values = entry.getValue();

        if (values == DELETED)
          _storeMap.remove(key);
        else
          _storeMap.put(key, values);
      }
    }

    discard();
  }

  /**
   * Discard all changes.
   */
  public void discard()
  {
    if (_valueMap != null)
      _valueMap.clear();
  }
}

