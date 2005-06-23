/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.widget;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class VarDefinition
{
  private static final String DEFAULT_DESCRIPTION = "No description available.";

  private String _name;
  private Map<Locale, String> _descriptionMap;
  private Class _type = Object.class;
  private boolean _isValue;
  private Object _value;
  private boolean _isInherited = false;
  private boolean _isReadOnly = false;
  private boolean _isAllowNull = true;

  public VarDefinition(String name, Class type)
  {
    _name = name;
    _type = type;
  }

  /**
   * For example "com.caucho.widget.Widget.excluded".
   */
  public String getName()
  {
    return _name;
  }

  public Class getType()
  {
    return _type;
  }

  /**
   * Set the description for the default locale, the default if no descriptions
   * are set is null.
   */
  public void setDescription(String description)
  {
    setDescription(Locale.getDefault(), description);
  }

  /**
   * Set the description for a locale.
   */
  public void setDescription(Locale locale, String description)
  {
    if (_descriptionMap == null)
      _descriptionMap = new LinkedHashMap<Locale, String>();

    _descriptionMap.put(locale, description);
  }

  /**
   * Return a description of the purpose of the var, null for no description.
   */
  public String getDescription(Locale locale)
  {
    String description = null;

    if (_descriptionMap != null) {

      while (locale != null) {
        description = _descriptionMap.get(locale);

        if (description != null)
          break;

        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        if (language.length() > 0 && country.length() > 0 && variant.length() > 0)
          locale = new Locale(language, country);
        else if (language.length() > 0 && country.length() > 0)
          locale = new Locale(language);
        else
          locale = null;
      }

      if (description == null)
        description = _descriptionMap.get(Locale.getDefault());

      if (description == null) {
        for (String firstDescription : _descriptionMap.values()) {
          description = firstDescription;
          break;
        }
      }
    }

    return description == null ? DEFAULT_DESCRIPTION : description;
  }

  /**
   * Set a value for the var, the default is for there to be no
   * value.  If called, this value becomes the "default" value for
   * a widget that does not set it's own value.
   */
  public void setValue(Object value)
  {
    _isValue = true;
    _value = value;
  }

  /**
   * Return true if a value has been set with {@link #setValue(Object)}.
   */
  public boolean isValue()
  {
    return _isValue;
  }

  /**
   * Get the value for the var, it may be null.
   */
  public <T> T getValue()
  {
    return (T) _value;
  }

  /**
   * If a var is inherited, it's value is obtained from the parent widget if
   * the value has not been set for the current widget. If no ancestor has a
   * value set, then the default is inherited in a similar manner.
   *
   * Default is false.
   */
  public void setInherited(boolean isInherited)
  {
    _isInherited = isInherited;
  }

  public boolean isInherited()
  {
    return _isInherited;
  }

  /**
   * Set the var as read-only, meaning the widget cannot set the
   * value, the default is false.
   */
  public void setReadOnly(boolean isReadOnly)
  {
    _isReadOnly = isReadOnly;
  }

  /**
   * Return true if the var is read-only.
   */
  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  /**
   * Set whether or not the var can be set to a null value, default is true.
   */
  public void setAllowNull(boolean isAllowNull)
  {
    _isAllowNull = isAllowNull;
  }

  /**
   * Return true if the variable can have a null value.
   */
  public boolean isAllowNull()
  {
    return _isAllowNull;
  }

}

