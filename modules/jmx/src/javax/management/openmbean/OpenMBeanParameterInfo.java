/*
 * Copyright (c) 2001-2002 Caucho Technology, Inc.  All rights reserved.
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

/**
 * Information about an MBean parameter.
 */
public interface OpenMBeanParameterInfo {
  /**
   * Returns the description of the parameter.
   */
  public String getDescription();
  
  /**
   * Returns the parameter name.
   */
  public String getName();

  /**
   * returns the open type.
   */
  public OpenType getOpenType();

  /**
   * Returns the default value.
   */
  public Object getDefaultValue();

  /**
   * Returns the set of legal values.
   */
  public Set getLegalValues();

  /**
   * Returns the minimal value.
   */
  public Comparable getMinValue();

  /**
   * Returns the maximal value.
   */
  public Comparable getMaxValue();

  /**
   * Returns true if this parameter has a specified default value.
   */
  public boolean hasDefaultValue();

  /**
   * Returns true if this parameter has a set of legal values.
   */
  public boolean hasLegalValues();

  /**
   * Returns true if this parameter has a minimal values.
   */
  public boolean hasMinValue();

  /**
   * Returns true if this parameter has a maximal values.
   */
  public boolean hasMaxValue();


  /**
   * Tests whether obj is a valid value.
   */
  public boolean isValue(Object obj);
}
