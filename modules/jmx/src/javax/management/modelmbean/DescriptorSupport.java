/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

package javax.management.modelmbean;

import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;

/**
 * Support class for a descriptor.
 */
public class DescriptorSupport implements Descriptor {
  public String currClass;

  private HashMap descriptor;

  /**
   * Zero-arg constructor.
   */
  public DescriptorSupport()
  {
    this.descriptor = new HashMap();
  }

  /**
   * Initializes number of fields
   */
  public DescriptorSupport(int initNumFields)
    throws MBeanException, RuntimeOperationsException
  {
    this.descriptor = new HashMap(initNumFields);
  }

  /**
   * Clone constructor.
   */
  public DescriptorSupport(DescriptorSupport descriptor)
  {
    this.descriptor = new HashMap(descriptor.descriptor);
  }

  /**
   * XML constructor.
   */
  public DescriptorSupport(String xml)
    throws MBeanException, RuntimeOperationsException, XMLParseException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * constructor.
   */
  public DescriptorSupport(String []fieldNames, Object []fieldValues)
    throws RuntimeOperationsException
  {
    descriptor = new HashMap();

    for (int i = 0; i < fieldNames.length; i++)
      descriptor.put(fieldNames[i], fieldValues[i]);
  }

  /**
   * constructor.
   */
  public DescriptorSupport(String []fields)
    throws RuntimeOperationsException
  {
    descriptor = new HashMap();

    for (int i = 0; i < fields.length; i++) {
      String field = fields[i];

      int p = field.indexOf('=');
      
      descriptor.put(field.substring(0, p), field.substring(p + 1));
    }
  }
  
  /**
   * Returns the value for a field name.
   *
   * @param fieldName the field name to retrieve
   *
   * @return the field value.
   */
  public Object getFieldValue(String fieldName)
    throws RuntimeOperationsException
  {
    return this.descriptor.get(fieldName);
  }
  
  /**
   * Sets a field value.
   *
   * @param fieldName the field name to set
   * @param fieldValue the field name to retrieve
   *
   * @return the field value.
   */
  public void setField(String fieldName, Object fieldValue)
    throws RuntimeOperationsException
  {
    this.descriptor.put(fieldName, fieldValue);
  }
  
  /**
   * Returns all the fields in the descriptor.
   *
   * @return all the fields.
   */
  public String []getFieldNames()
  {
    Set keySet = descriptor.keySet();

    int size = keySet.size();

    String []fields = new String[size];
    keySet.toArray(fields);

    return fields;
  }
  
  /**
   * Returns all the fields in the descriptor.
   *
   * @return all the fields.
   */
  public String []getFields()
  {
    Set keySet = descriptor.keySet();

    int size = keySet.size();

    String []fields = new String[size];

    Iterator iter = keySet.iterator();
    int i = 0;
    while (iter.hasNext()) {
      String name = (String) iter.next();

      fields[i++] = name + '=' + descriptor.get(name);
    }

    return fields;
  }
  
  /**
   * Removes a field from the descriptor.
   *
   * @param fieldName the field to remove
   */
  public void removeField(String fieldName)
  {
    descriptor.remove(fieldName);
  }
  
  /**
   * Sets a field from the descriptor.
   *
   * @param fieldNames the fields names to set
   * @param fieldValues the fields values to set
   */
  public void setFields(String []fieldNames, Object []fieldValues)
    throws RuntimeOperationsException
  {
    for (int i = 0; i < fieldNames.length; i++)
      descriptor.put(fieldNames[i], fieldValues[i]);
  }
  
  /**
   * Gets fields from the descriptor.
   *
   * @param fieldNames the fields names to get
   */
  public Object []getFieldValues(String []fieldNames)
    throws RuntimeOperationsException
  {
    Object []values = new Object[fieldNames.length];

    for (int i = 0; i < fieldNames.length; i++)
      values[i] = descriptor.get(fieldNames[i]);
    
    return values;
  }
  
  /**
   * Returns a clone of the descriptor.
   */
  public Object clone()
    throws RuntimeOperationsException
  {
    return new DescriptorSupport(this);
  }
  
  /**
   * Returns true if the field values are valid.
   */
  public boolean isValid()
    throws RuntimeOperationsException
  {
    return true;
  }
  
  /**
   * Converts to an XML string.
   */
  public String toXMLString()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("<top>\n");
    
    Iterator iter = descriptor.keySet().iterator();

    while (iter.hasNext()) {
      String key = (String) iter.next();

      Object value = this.descriptor.get(key);

      sb.append("<" + key + ">");

      if (value != null) {
	String sValue = value.toString();

	for (int i = 0; i < sValue.length(); i++) {
	  char ch = sValue.charAt(i);

	  if (ch == '<')
	    sb.append("&lt;");
	  else if (ch == '&')
	    sb.append("&amp;");
	  else
	    sb.append(ch);
	}
      }
      
      sb.append("</" + key + ">\n");
    }

    sb.append("</top>");

    return sb.toString();
  }
}
