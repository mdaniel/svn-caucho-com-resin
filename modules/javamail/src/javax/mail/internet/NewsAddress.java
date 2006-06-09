/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

/**
 * This class models an RFC1036 newsgroup address.
 * See Also:Serialized Form
 */
public class NewsAddress extends Address {

  /**
   * required by API
   */
  protected String host;

  /**
   * required by API
   */
  protected String newsgroup;

  /**
   * Default constructor.
   */
  public NewsAddress()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Construct a NewsAddress with the given newsgroup.
   * newsgroup - the newsgroup
   */
  public NewsAddress(String newsgroup)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Construct a NewsAddress with the given newsgroup and host.
   * newsgroup - the newsgrouphost - the host
   */
  public NewsAddress(String newsgroup, String host)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * The equality operator.
   */
  public boolean equals(Object a)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the host.
   */
  public String getHost()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Get the newsgroup.
   */
  public String getNewsgroup()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the type of this address. The type of a NewsAddress is "news".
   */
  public String getType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Compute a hash code for the address.
   */
  public int hashCode()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parse the given comma separated sequence of newsgroup into
   * NewsAddress objects.
   */
  public static NewsAddress[] parse(String newsgroups) throws AddressException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the host.
   */
  public void setHost(String host)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the newsgroup.
   */
  public void setNewsgroup(String newsgroup)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert this address into a RFC 1036 address.
   */
  public String toString()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert the given array of NewsAddress objects into a comma
   * separated sequence of address strings. The resulting string
   * contains only US-ASCII characters, and hence is mail-safe.
   */
  public static String toString(Address[] addresses)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
