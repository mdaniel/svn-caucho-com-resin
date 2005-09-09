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

package com.caucho.jmx;

import javax.management.Descriptor;
import javax.management.RuntimeOperationsException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

/**
 * A descriptor that is created by {@link IntrospectionMBean} to describe
 * the bean or a feature of the bean.
 * <h3>Closure</h3>
 *
 * The value for a field can returning a dynamic value by implementing
 * {@link IntrospectionClosure}, when the descriptor is requested for the value of a filed
 * the value of {@link IntrospectionClosure#call()} is returned.
 *
 * <h3>Fields typically used by Resin</h3>
 *
 * <table>
 * <tr><th>Name
 *     <th>Applies to
 *     <th>Description
 * <tr><td>com.caucho.jmx.description
 *     <td>Bean, Attribute, Operation
 *     <td>A description, becomes the value of
 *         {@link javax.management.MBeanInfo#getDescription()}
 *         or {@link javax.management.MBeanFeatureInfo#getDescription()}
 * <tr><td>com.caucho.admin.title
 *     <td>Bean
 *     <td>A title for the bean
 * <tr><td>com.caucho.admin.category
 *     <td>Operation
 *     <td>"query", "command" (default)
 * <tr><td>com.caucho.admin.category
 *     <td>Attribute
 *     <td>"general" (default), "configuration", "statistic"
 * <tr><td>com.caucho.admin.ignored
 *     <td>Bean, Operation, Attribute
 *     <td>if "true", do not display in the administration service
 * <tr><td>com.caucho.admin.sortOrder
 *     <td>Operation, Attribute
 *     <td>an integer to use for establishing a sort order, default is infinity
 * <tr><td>deprecated
 *     <td>Bean, Operation, Attribute
 *     <td>if present, the feature should not be used
 * <tr><td>enabled
 *     <td>Operation, Attribute
 *     <td>"true" (default), or "false"
 * </table>
 */
public class IntrospectionDescriptor
  implements Descriptor
{
  private final HashMap<String, Object> _map = new HashMap<String, Object>();

  private String _description;

  IntrospectionDescriptor()
  {
  }

  private IntrospectionDescriptor(IntrospectionDescriptor introspectionDescriptor)
  {
    _map.putAll(introspectionDescriptor._map);
  }

  public Object getFieldValue(String fieldName)
    throws RuntimeOperationsException
  {
    Object obj = _map.get(fieldName);

    obj = getValue(obj);

    return obj;
  }

  private Object getValue(Object obj)
    throws RuntimeOperationsException
  {
    if (obj instanceof IntrospectionClosure) {
      try {
        obj = ((IntrospectionClosure) obj).call();
      }
      catch (RuntimeException ex) {
        throw new RuntimeOperationsException(ex);
      }
      catch (Exception ex) {
        throw new RuntimeOperationsException(new RuntimeException(ex));
      }
    }

    return obj;
  }

  public void setField(String fieldName, Object fieldValue)
    throws RuntimeOperationsException
  {
    _map.put(fieldName, fieldValue);
  }

  public String[] getFields()
  {
    String[] fields = new String[_map.keySet().size()];

    int i = 0;

    for (Map.Entry<String, Object> entry : _map.entrySet()) {
      String name = entry.getKey();
      Object value = getValue(entry.getValue());

      fields[i++] = name + '=' + value;
    }

    return fields;
  }

  public String[] getFieldNames()
  {
    Set<String> keySet = _map.keySet();

    String[] fields = new String[keySet.size()];
    keySet.toArray(fields);

    return fields;
  }

  public Object[] getFieldValues(String[] fieldNames)
  {
    Object[] fieldValues = new String[_map.keySet().size()];

    for (int i = 0; i < fieldNames.length; i++) {
      fieldValues[i] = getFieldValue(fieldNames[i]);
    }

    return fieldValues;
  }

  public void removeField(String fieldName)
  {
    _map.remove(fieldName);
  }

  public void setFields(String[] fieldNames, Object[] fieldValues)
    throws RuntimeOperationsException
  {
    for (int i = 0; i < fieldNames.length; i++)
      _map.put(fieldNames[i], fieldValues[i]);
  }

  public Object clone()
    throws RuntimeOperationsException
  {
    return new IntrospectionDescriptor(this);
  }

  public boolean isValid()
    throws RuntimeOperationsException
  {
    return true;
  }

  /**
   * Set the description that is returned by
   * {@link javax.management.MBeanInfo#getDescription()}
   * or {@link javax.management.MBeanFeatureInfo#getDescription()}.
   * The default is a short description based on the name of the feature
   * or the class name of the mbean.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  public String getDescription()
  {
    return _description;
  }
}
