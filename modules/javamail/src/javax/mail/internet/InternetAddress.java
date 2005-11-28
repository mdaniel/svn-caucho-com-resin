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

package javax.mail.internet;

import javax.mail.Address;

/**
 * Represents an internet address
 */
public class InternetAddress extends Address {
  protected String address;
  protected String encodedPersonalName;
  protected String personal;

  public InternetAddress()
  {
  }

  public InternetAddress(String address)
  {
    this.address = address;
  }

  public InternetAddress(String address, String personalName)
    throws java.io.UnsupportedEncodingException
  {
    this.address = address;

    setPersonal(personalName);
  }

  public InternetAddress(String address, String personalName, String charset)
    throws java.io.UnsupportedEncodingException
  {
    this.address = address;

    setPersonal(personalName, charset);
  }

  public String getType()
  {
    return "rfc822";
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  public String getAddress()
  {
    return this.address;
  }

  public void setPersonal(String personalName)
    throws java.io.UnsupportedEncodingException
  {
    this.personal = personalName;
    this.encodedPersonalName = personalName;
  }

  public void setPersonal(String personalName, String charset)
    throws java.io.UnsupportedEncodingException
  {
    this.personal = personalName;
    this.encodedPersonalName = personalName;
  }

  public String getPersonal()
  {
    if (this.encodedPersonalName != null)
      return this.encodedPersonalName;
    else
      return this.personal;
  }

  public int hashCode()
  {
    return this.address.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (this instanceof InternetAddress))
      return false;

    InternetAddress addr = (InternetAddress) o;

    return this.address.equals(addr.address);
  }

  public String toString()
  {
    // XXX: is a clean rfc-822 address
    
    return this.address;
  }
}
