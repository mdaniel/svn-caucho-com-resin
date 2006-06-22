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
import java.util.*;

/**
 * This class models an RFC1036 newsgroup address.
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
    this(null, null);
  }

  /**
   * Construct a NewsAddress with the given newsgroup.
   * newsgroup - the newsgroup
   */
  public NewsAddress(String newsgroup)
  {
    this(newsgroup, null);
  }

  /**
   * Construct a NewsAddress with the given newsgroup and host.
   * newsgroup - the newsgrouphost - the host
   */
  public NewsAddress(String newsgroup, String host)
  {
    this.host = host;
    this.newsgroup = newsgroup;
  }

  /**
   * The equality operator.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof NewsAddress))
      return false;
    
    NewsAddress na = (NewsAddress)o;
    return equal(na.host, host) && equal(na.newsgroup, newsgroup);
  }

  private static boolean equal(Object o1, Object o2) {

    if (o1 == null && o2 == null)
      return true;

    if (o1 == null || o2 == null)
      return false;

    return o1.equals(o2);

  }

  /**
   * Get the host.
   */
  public String getHost()
  {
    return host;
  }

  /**
   * Get the newsgroup.
   */
  public String getNewsgroup()
  {
    return newsgroup;
  }

  /**
   * Return the type of this address. The type of a NewsAddress is "news".
   */
  public String getType()
  {
    return "news";
  }

  /**
   * Compute a hash code for the address.
   */
  public int hashCode()
  {
    return newsgroup.hashCode() * 65521 + newsgroup.hashCode();
  }

  /**
   * Parse the given comma separated sequence of newsgroup into
   * NewsAddress objects.
   */
  public static NewsAddress[] parse(String newsgroups) throws AddressException
  {
    ArrayList newsAddresses = new ArrayList();
    String s = newsgroups;

    while(true) {
      int comma = s.indexOf(',');
      if (comma == -1) {
	newsAddresses.add(new NewsAddress(s));
	break;
      }
      newsAddresses.add(new NewsAddress(s.substring(0, comma)));
      s = s.substring(comma+1);
    }

    NewsAddress[] returnArray = new NewsAddress[newsAddresses.size()];

    return (NewsAddress[])newsAddresses.toArray(returnArray);
  }

  /**
   * Set the host.
   */
  public void setHost(String host)
  {
    this.host = host;
  }

  /**
   * Set the newsgroup.
   */
  public void setNewsgroup(String newsgroup)
  {
    this.newsgroup = newsgroup;
  }

  /**
   * Convert this address into a RFC 1036 address.
   */
  public String toString()
  {
    return newsgroup;
  }

  /**
   * Convert the given array of NewsAddress objects into a comma
   * separated sequence of address strings. The resulting string
   * contains only US-ASCII characters, and hence is mail-safe.
   */
  public static String toString(Address[] addresses)
  {
    StringBuffer sb = new StringBuffer();

    for(int i=0; i<addresses.length; i++) {
      if (i>0)
	sb.append(',');

      NewsAddress na = (NewsAddress)addresses[i];

      sb.append(na.getNewsgroup());
    }

    return sb.toString();
  }

}
