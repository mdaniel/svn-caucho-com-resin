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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.xml.namespace;

/**
 * Implementation of the XML QName.
 */
public class QName implements java.io.Serializable {
  private String namespaceURI;
  private String localPart;
  private String prefix;
  
  public QName(String localPart)
  {
    this.localPart = localPart;
    this.namespaceURI = "";
    this.prefix = "";
  }
  
  public QName(String uri, String localPart)
  {
    this.localPart = localPart;
    
    if (uri == null)
      uri = "";
    this.namespaceURI = uri;
    
    this.prefix = "";
  }
  
  public QName(String uri, String localPart, String prefix)
  {
    this.localPart = localPart;

    if (uri == null)
      uri = "";
    this.namespaceURI = uri;

    if (prefix == null)
      prefix = "";
    this.prefix = prefix;
  }

  /**
   * Creates by parsing.
   */
  public static QName valueOf(String name)
  {
    int p = name.indexOf('}');

    if (p < 0)
      return new QName(name);
    else {
      return new QName(name.substring(1, p), name.substring(p + 1));
    }
  }

  /**
   * Returns the local part.
   */
  public String getLocalPart()
  {
    return this.localPart;
  }

  /**
   * Returns the prefix.
   */
  public String getPrefix()
  {
    return this.prefix;
  }

  /**
   * Returns the namespace URI.
   */
  public String getNamespaceURI()
  {
    return this.namespaceURI;
  }

  /**
   * Returns a hash code.
   */
  public int hashCode()
  {
    return this.namespaceURI.hashCode() * 65521 + this.localPart.hashCode();
  }

  /**
   * Tests for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof QName))
      return false;

    QName name = (QName) o;

    return (this.localPart.equals(name.localPart) &&
	    this.namespaceURI.equals(name.namespaceURI));
  }

  /**
   * Returns a printable representaion.
   */
  public String toString()
  {
    if ("".equals(this.namespaceURI))
      return this.localPart;
    else
      return "{" + this.namespaceURI + "}" + this.localPart;
  }
}
