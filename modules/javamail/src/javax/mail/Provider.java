/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail;


/**
 * Represents a mail transport provider
 */
public class Provider {
  private String _className;
  private String _protocol;
  private Type _type;
  private String _vendor;
  private String _version;
  
  Provider()
  {
    // XXX:
  }
  
  Provider(String className, String protocol, Type type,
	   String vendor, String version)
  {
    _className = className;
    _protocol = protocol;
    _type = type;
    _vendor = vendor;
    _version = version;
  }

  /**
   * Return the implementing class name.
   */
  public String getClassName()
  {
    return _className;
  }

  /**
   * Return the implementing class name.
   */
  String setClassName(String className)
  {
    return _className;
  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    return _protocol;
  }

  /**
   * Returns the type
   */
  public Type getType()
  {
    return _type;
  }

  /**
   * Returns the vendor.
   */
  public String getVendor()
  {
    return _vendor;
  }

  /**
   * Returns the version.
   */
  public String getVersion()
  {
    return _version;
  }

  public String toString()
  {
    return getClass().getName() + "[" + getProtocol() + "," + getClassName() + "]";
  }

  public static class Type {
    public static final Type STORE = new Type("store");
    public static final Type TRANSPORT = new Type("transport");

    private final String _name;

    Type(String name)
    {
      _name = name;
    }

    public String toString()
    {
      return "Type[" + _name + "]";
    }
  }
}
