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

/**
 * Represents an mbean instance.
 */
public class ObjectInstance implements java.io.Serializable {
  // The class name
  private String className;
  
  // The object name
  private ObjectName name;

  /**
   * Creates an ObjectInstance.
   *
   * @param name the object's name
   * @param className the object's class
   */
  public ObjectInstance(ObjectName objectName, String className)
  {
    this.name = objectName;
    this.className = className;
  }

  /**
   * Creates an ObjectInstance.
   *
   * @param name the object's name
   * @param className the object's class
   */
  public ObjectInstance(String name, String className)
    throws MalformedObjectNameException
  {
    this.name = new ObjectName(name);
    this.className = className;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return name;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * Returns the hashCode of the instance.
   */
  public int hashCode()
  {
    return 65521 * name.hashCode() + className.hashCode();
  }

  /**
   * Returns true if the object is equal.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof ObjectInstance))
      return false;

    ObjectInstance instance = (ObjectInstance) obj;

    return name.equals(instance.name) && className.equals(instance.className);
  }

  /**
   * Returns a viewable version of the instance.
   */
  public String toString()
  {
    return "ObjectInstance[" + name + "]";
  }
}
