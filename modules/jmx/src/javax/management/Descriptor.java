/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package javax.management;

/**
 * Represents a descriptor for the ModelMBean.
 */
public interface Descriptor extends java.io.Serializable, Cloneable {
  /**
   * Returns the value for a field name.
   *
   * @param fieldName the field name to retrieve
   *
   * @return the field value.
   */
  public Object getFieldValue(String fieldName)
    throws RuntimeOperationsException;
  
  /**
   * Sets a field value.
   *
   * @param fieldName the field name to set
   * @param fieldValue the field name to retrieve
   *
   * @return the field value.
   */
  public void setField(String fieldName, Object fieldValue)
    throws RuntimeOperationsException;
  
  /**
   * Returns all the fields in the descriptor.
   *
   * @return all the fields.
   */
  public String []getFields();
  
  /**
   * Returns all the fields in the descriptor.
   *
   * @return all the fields.
   */
  public String []getFieldNames();
  
  /**
   * Removes a field from the descriptor.
   *
   * @param fieldName the field to remove
   */
  public void removeField(String fieldName);
  
  /**
   * Sets a field from the descriptor.
   *
   * @param fieldNames the fields names to set
   * @param fieldValues the fields values to set
   */
  public void setFields(String []fieldNames, Object []fieldValues)
    throws RuntimeOperationsException;
  
  /**
   * Gets fields from the descriptor.
   *
   * @param fieldNames the fields names to set
   */
  public Object []getFieldValues(String []fieldNames)
    throws RuntimeOperationsException;
  
  /**
   * Returns a clone of the descriptor.
   */
  public Object clone()
    throws RuntimeOperationsException;
  
  /**
   * Returns true if the field values are valid.
   */
  public boolean isValid()
    throws RuntimeOperationsException;
}
