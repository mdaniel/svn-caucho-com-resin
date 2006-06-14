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
  private String _ref;
  private String _userName;
  private String _password;
  
  public URLName(String url)
  {
    this.fullURL = url;

    parseString(url);
  }

  /**
   *  dencode a string according to rfc1738 "escape" coding
   */
  private static String urlDecode(String s) {
    if (s==null) return null;
    if (s.indexOf('%')==-1)
      return s;
    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for(int i=0; i<len; i++) {
      char c = s.charAt(i);
      if (c=='%') {
	sb.append((char)Integer.parseInt(s.substring(i+1, i+3), 16));
	i += 2;
      } else {
	sb.append(c);
      }
    }
    return sb.toString();
  }
  
  /**
   *  encode a string according to rfc1738 "uchar" coding
   */
  private static String urlEncode(String s) {
    if (s==null) return null;
    StringBuilder sb = null;
    int len = s.length();
    for(int i=0; i<len; i++) {
      char c = s.charAt(i);
      if (!(c >= 'a' && c <= 'z') &&
	  !(c >= 'A' && c <= 'Z') &&
	  !(c >= '0' && c <= '9'))
	switch(c) {
	  case '$': case '-': case '_': case '.': case '+':
	  case '!': case '*':  case '\'': case '(': case ')': case ',': {
	    break;
	  }
	  default: {
	    if (sb==null) {
	      sb = new StringBuilder();
	      sb.append(s.substring(0, i));
	    }
	    sb.append('%');
	    sb.append(Integer.toString(c, 16));
	    continue;
	  }
	}
      if (sb!=null)
	sb.append(c);
    }
    return sb==null ? s : sb.toString();
  }
  
  public URLName(String protocol, String  host, int port,
		 String file, String userName, String password)
  {
    _protocol = protocol;            // cannot be %-encoded, see rfc 1738
    _host     = host;                // cannot be %-encoded, see rfc 1738
    _port     = port;
    _file     = parseFile(file);     // URLName leaves this un-decoded
    _ref      = parseRef(file);      // URLName leaves this un-decoded
    _userName = urlDecode(userName);
    _password = urlDecode(password);

    StringBuilder sb = new StringBuilder();

    sb.append(_protocol);
    sb.append("://");

    if (userName != null) {
      sb.append(urlEncode(_userName));
      if (password != null) {
	sb.append(':');
	sb.append(urlEncode(_password));
      }
      sb.append('@');
    }

    sb.append(_host);

    if (_port != -1) {
      sb.append(':');
      sb.append(_port);
    }

    sb.append('/');

    if (_file != null)
      sb.append(_file);

    if (_ref != null) {
      sb.append('#');
      sb.append(_ref);
    }

    this.fullURL = sb.toString();
  }

  private static String userInfoUser(String userInfo)
  {
    if (userInfo==null)
      return null;

    int colon = userInfo.indexOf(':');
    if (colon==-1)
      return userInfo;

    return userInfo.substring(0, colon);
  }

  private static String userInfoPassword(String userInfo)
  {
    if (userInfo==null)
      return null;

    int colon = userInfo.indexOf(':');
    if (colon==-1)
      return null;

    return userInfo.substring(colon+1);
  }

  public URLName(URL url)
  {
    this(url.getProtocol(),
	 url.getHost(),
	 url.getPort(),
	 url.getFile(),
	 userInfoUser(url.getUserInfo()),
	 userInfoPassword(url.getUserInfo()));
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
    return _ref;
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

  /**
   * Parse the URL
   */
  protected void parseString(String url)
  {
    _protocol = parseProtocol(url);
    _host     = parseHost(url);
    _port     = parsePort(url);
    String fileRef = parseFileRef(url);
    _file     = parseFile(fileRef);
    _ref      = parseRef(fileRef);

    String userInfo = parseUserInfo(url);
    _userName = urlDecode(userInfoUser(userInfo));
    _password = urlDecode(userInfoPassword(userInfo));
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


  // Parsing Helpers ////////////////////////////////////////////////////////

  // XXX: refactor these into a unified parseString() method to avoid
  // overhead of multiple redundant indexOf()'s and substring()'s... I
  // just wrote this as distinct methods because it was easier to
  // debug that way.

  private String parseProtocol(String url)
  {
    int colon = url.indexOf(':');
    if (colon==-1)
      throw new IllegalArgumentException("url must contain a colon: "+url);
    
    return url.substring(0, colon);
  }

  private String parseUserInfo(String url)
  {
    String userInfoHostPort = parseUserInfoHostPort(url);

    int atSign = userInfoHostPort.indexOf('@');
    if (atSign==-1)
      return null;

    return userInfoHostPort.substring(0, atSign);
  }

  private int parsePort(String url)
  {
    String hostPort = parseHostPort(url);

    int colon = hostPort.indexOf(':');
    if (colon == -1)
      return -1;
    return Integer.parseInt(hostPort.substring(colon+1));
  }

  private String parseHostPort(String url)
  {
    String userInfoHostPort = parseUserInfoHostPort(url);

    int atSign = userInfoHostPort.indexOf('@');
    if (atSign!=-1)
      userInfoHostPort = userInfoHostPort.substring(atSign+1);

    return userInfoHostPort;
  }

  private String parseHost(String url) {
    String hostPort = parseHostPort(url);

    int colon = hostPort.indexOf(':');
    if (colon != -1)
      hostPort = hostPort.substring(0, colon);

    return hostPort;
  }

  private String parseFile(String fileRef)
  {
    if (fileRef==null)
      return null;

    if (fileRef.indexOf('#')==-1)
      return fileRef;
    return fileRef.substring(0, fileRef.indexOf('#'));
  }

  private String parseRef(String fileRef)
  {
    if (fileRef==null)
      return null;

    if (fileRef.indexOf('#')==-1)
      return null;
    return fileRef.substring(fileRef.indexOf('#')+1);
  }

  private String parseFileRef(String url)
  {
    int colon = url.indexOf(':');
    if (colon==-1)
      throw new IllegalArgumentException("url must contain a colon: " + url);

    int start = colon+1;
    while(url.charAt(start)=='/')
      start++;

    int tail = url.indexOf('/', start);
    return tail==-1
      ? null
      : url.substring(tail+1);
  }

  private String parseUserInfoHostPort(String url)
  {
    int colon = url.indexOf(':');
    if (colon==-1)
      throw new IllegalArgumentException("url must contain a colon: " + url);

    int start = colon+1;
    while(url.charAt(start)=='/')
      start++;

    int tail = url.indexOf('/', start);
    return tail==-1
      ? url.substring(start)
      : url.substring(start, tail);
  }

}
