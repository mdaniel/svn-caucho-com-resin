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

package javax.management;

import java.security.Permission;

/**
 * Permission set.
 */
public class MBeanPermission extends Permission {
  private String name;
  private String actions;
  private String className;
  private String member;
  private ObjectName objectName;

  /**
   * Constructor.
   *
   * @param name the name of the parameter
   * @param type the type of the parameter
   * @param description a description of the parameter
   */
  public MBeanPermission(String name, String actions)
  {
    super(name);
    
    this.actions = actions;
  }

  /**
   * Constructor.
   *
   * @param name the name of the parameter
   * @param type the type of the parameter
   * @param description a description of the parameter
   */
  public MBeanPermission(String className, String member,
			 ObjectName objectName, String actions)
  {
    super(className);

    this.member = member;
    this.objectName = objectName;
    this.actions = actions;
  }

  /**
   * Returns the actions
   */
  public String getActions()
  {
    return this.actions;
  }

  /**
   * Implements the implies.
   */
  public boolean implies(Permission permission)
  {
    return equals(permission);
  }

  /**
   * Return the hash code.
   */
  public int hashCode()
  {
    return this.name.hashCode();
  }

  /**
   * Return the hash code.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof MBeanPermission))
      return false;

    MBeanPermission perm = (MBeanPermission) o;

    return this.name.equals(perm.name);
  }
}
