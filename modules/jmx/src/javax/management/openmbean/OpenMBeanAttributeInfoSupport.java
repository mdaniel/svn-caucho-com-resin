/*
 * Copyright (c) 2001-2003 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
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
 * @author Scott Ferguson
 */

package javax.management.openmbean;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import javax.management.MBeanAttributeInfo;

/**
 * Information about an MBean parameter.
 */
public class OpenMBeanAttributeInfoSupport
  extends MBeanAttributeInfo
  implements OpenMBeanAttributeInfo {
  private OpenType openType;
  private Object defaultValue;
  private Set legalValues;
  private Comparable minValue;
  private Comparable maxValue;
    
  public OpenMBeanAttributeInfoSupport(String name,
				       String description,
				       OpenType openType,
				       boolean isReadable,
				       boolean isWritable,
				       boolean isIs)
    throws OpenDataException
  {
    super(name, openType.getTypeName(), description,
	  isReadable, isWritable, isIs);

    this.openType = openType;
  }
    
  public OpenMBeanAttributeInfoSupport(String name,
				       String description,
				       OpenType openType,
				       boolean isReadable,
				       boolean isWritable,
				       boolean isIs,
				       Object defaultValue)
    throws OpenDataException
  {
    this(name, description, openType,
	 isReadable, isWritable, isIs);

    this.defaultValue = defaultValue;
  }
    
  public OpenMBeanAttributeInfoSupport(String name,
				       String description,
				       OpenType openType,
				       boolean isReadable,
				       boolean isWritable,
				       boolean isIs,
				       Object defaultValue,
				       Object []legalValues)
    throws OpenDataException
  {
    this(name, description, openType,
	  isReadable, isWritable, isIs);

    this.defaultValue = defaultValue;
    this.legalValues = new HashSet();

    for (int i = 0; i < legalValues.length; i++)
      this.legalValues.add(legalValues[i]);

    this.legalValues = Collections.unmodifiableSet(this.legalValues);
  }
    
  public OpenMBeanAttributeInfoSupport(String name,
				       String description,
				       OpenType openType,
				       boolean isReadable,
				       boolean isWritable,
				       boolean isIs,
				       Object defaultValue,
				       Comparable minValue,
				       Comparable maxValue)
    throws OpenDataException
  {
    this(name, description, openType,
	 isReadable, isWritable, isIs);

    this.defaultValue = defaultValue;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /**
   * Returns the open type.
   */
  public OpenType getOpenType()
  {
    return this.openType;
  }

  /**
   * Returns the default value.
   */
  public Object getDefaultValue()
  {
    return this.defaultValue;
  }

  /**
   * Returns the legal values.
   */
  public Set getLegalValues()
  {
    return this.legalValues;
  }

  /**
   * Returns the minimal legal value.
   */
  public Comparable getMinValue()
  {
    return this.minValue;
  }

  /**
   * Returns the maximal legal value.
   */
  public Comparable getMaxValue()
  {
    return this.maxValue;
  }

  /**
   * Returns true if there is a default value.
   */
  public boolean hasDefaultValue()
  {
    return this.defaultValue != null;
  }

  /**
   * Returns true if there is a legal value set
   */
  public boolean hasLegalValues()
  {
    return this.legalValues != null;
  }

  /**
   * Returns true if there is a min value
   */
  public boolean hasMinValue()
  {
    return this.minValue != null;
  }

  /**
   * Returns true if there is a max value
   */
  public boolean hasMaxValue()
  {
    return this.maxValue != null;
  }

  /**
   * Returns true if the object is a legal value.
   */
  public boolean isValue(Object obj)
  {
    return getOpenType().isValue(obj);
  }

  /**
   * Returns the equality relation.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof OpenMBeanAttributeInfoSupport))
      return false;

    OpenMBeanAttributeInfoSupport info = (OpenMBeanAttributeInfoSupport) o;

    if (! getOpenType().equals(info.getOpenType()))
      return false;

    // XXX:
    
    return getName().equals(info.getName());
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return getOpenType().hashCode() + getName().hashCode();
  }
}
