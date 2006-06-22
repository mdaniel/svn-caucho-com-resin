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
import javax.mail.*;

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

  public InternetAddress(String address, boolean strict)
    throws AddressException
  {
    // XXX: what should we do here?
    this(address);
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
    // XXX: is a clean rfc-822 address; use RFC 2047 encoding
    
    return this.address;
  }

  /**
   * Return a copy of this InternetAddress object.
   */
  public Object clone()
  {
    try {
      return new InternetAddress(address, personal);
    }
    catch (java.io.UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the members of a group address. A group may have zero,
   * one, or more members. If this address is not a group, null is
   * returned. The strict parameter controls whether the group list is
   * parsed using strict RFC 822 rules or not. The parsing is done
   * using the parseHeader method.
   */
  public InternetAddress[] getGroup(boolean strict) throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");    
  }

  /**
   * Return an InternetAddress object representing the current
   * user. The entire email address may be specified in the
   * "mail.from" property. If not set, the "mail.user" and "mail.host"
   * properties are tried. If those are not set, the "user.name"
   * property and InetAddress.getLocalHost method are tried. Security
   * exceptions that may occur while accessing this information are
   * ignored. If it is not possible to determine an email address,
   * null is returned.
   */
  public static InternetAddress getLocalAddress(Session session)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Indicates whether this address is an RFC 822 group address. Note
   * that a group address is different than the mailing list addresses
   * supported by most mail servers. Group addresses are rarely used;
   * see RFC 822 for details.
   */
  public boolean isGroup()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parse the given comma separated sequence of addresses into
   * InternetAddress objects. Addresses must follow RFC822 syntax.
   */
  public static InternetAddress[] parse(String addresslist)
    throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parse the given sequence of addresses into InternetAddress
   * objects. If strict is false, simple email addresses separated by
   * spaces are also allowed. If strict is true, many (but not all) of
   * the RFC822 syntax rules are enforced. In particular, even if
   * strict is true, addresses composed of simple names (with no
   * "@domain" part) are allowed. Such "illegal" addresses are not
   * uncommon in real messages.  Non-strict parsing is typically used
   * when parsing a list of mail addresses entered by a human. Strict
   * parsing is typically used when parsing address headers in mail
   * messages.
   */
  public static InternetAddress[] parse(String addresslist,
					boolean strict)
    throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parse the given sequence of addresses into InternetAddress
   * objects. If strict is false, the full syntax rules for individual
   * addresses are not enforced. If strict is true, many (but not all)
   * of the RFC822 syntax rules are enforced.  To better support the
   * range of "invalid" addresses seen in real messages, this method
   * enforces fewer syntax rules than the parse method when the strict
   * flag is false and enforces more rules when the strict flag is
   * true. If the strict flag is false and the parse is successful in
   * separating out an email address or addresses, the syntax of the
   * addresses themselves is not checked.
   */
  public static InternetAddress[] parseHeader(String addresslist,
					      boolean strict)
    throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert the given array of InternetAddress objects into a comma
   * separated sequence of address strings. The resulting string
   * contains only US-ASCII characters, and hence is mail-safe.  The
   * 'used' parameter specifies the number of character positions
   * already taken up in the field into which the resulting address
   * sequence string is to be inserted. It is used to determine the
   * line-break positions in the resulting address sequence string.
   */
  public static String toString(Address[] addresses, int used)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns a properly formatted address (RFC 822 syntax) of Unicode
   * characters.
   */
  public String toUnicodeString()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Validate that this address conforms to the syntax rules of RFC
   * 822. The current implementation checks many, but not all, syntax
   * rules. Note that even though the syntax of the address may be
   * correct, there's no guarantee that a mailbox of that name exists.
   */
  public void validate() throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");
  }
}



