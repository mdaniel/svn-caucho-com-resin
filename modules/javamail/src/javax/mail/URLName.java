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

import java.net.URL;
import java.net.MalformedURLException;

/**
 * Represents a mail address name.
 */
public class URLName {
  protected String fullURL;

  private String _protocol;
  private String _host;
  private int _port;
  private String _file;
  private String _userName;
  private String _password;
  
  public URLName(String url)
  {
    this.fullURL = url;

    parseString(url);
  }
  
  public URLName(String protocol, String  host, int port,
		 String file, String userName, String password)
  {
    // XXX:
    
    this.fullURL = protocol + "://" + host + ":" + port;

    _protocol = protocol;
    _host = host;
    _port = port;
    _file = file;
    _userName = userName;
    _password = password;
  }

  public URLName(URL url)
  {
    this.fullURL = url.toString();

    // XXX:

  }

  /**
   * Returns the protocol.
   */
  public String getProtocol()
  {
    return _protocol;
  }

  /**
   * Returns the host.
   */
  public String getHost()
  {
    return _host;
  }

  /**
   * Returns the port.
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Returns the file.
   */
  public String getFile()
  {
    return _file;
  }

  /**
   * Returns the ref.
   */
  public String getRef()
  {
    //XXX:
    return null;
  }

  /**
   * Returns the username.
   */
  public String getUsername()
  {
    return _userName;
  }

  /**
   * Returns the password.
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Returns the URL
   */
  public URL getURL()
    throws MalformedURLException
  {
    return new URL(this.fullURL);
  }

  protected void parseString(String url)
  {
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof URLName))
      return false;

    URLName url = (URLName) o;

    return this.fullURL.equals(url.fullURL);
  }

  public int hashCode()
  {
    return this.fullURL.hashCode();
  }

  public String toString()
  {
    return this.fullURL;
  }
}
