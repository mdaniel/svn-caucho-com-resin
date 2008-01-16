/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.security;

import com.caucho.config.Config;
import com.caucho.security.BasicPrincipal;
import com.caucho.util.Alarm;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Hashtable;
import java.util.logging.*;

/**
 * The XML authenticator reads a static file for authentication.
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * &lt;authenticator>
 * &lt;user name='Harry Potter' password='quidditch'>
 *   &lt;role>user&lt;/role>
 *   &lt;role>gryffindor&lt;/role>
 * &lt;/user>
 * ...
 * &lt;/authenticator>
 * </pre></code>
 *
 * <p>The authenticator can also be configured in the web.xml:
 *
 * <code><pre>
 * &lt;authenticator class-name='com.caucho.http.security.XmlAuthenticator'>
 *   &lt;init-param user='Harry Potter:quidditch:user,gryffindor'/>
 * &lt;/authenticator>
 * </pre></code>
 */
public class XmlAuthenticator extends AbstractAuthenticator {
  private Path _path;
  private Hashtable<String,User> _userMap = new Hashtable<String,User>();

  private Depend _depend;
  private long _lastCheck;

  /**
   * Sets the path to the XML file.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the path to the XML file.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Adds a user from the configuration.
   *
   * <pre>
   * &lt;init-param user='Harry Potter:quidditch:user,webdav'/>
   * </pre>
   */
  public void addUser(User user)
  {
    _userMap.put(user.getName(), user);
  }

  /**
   * Initialize the XML authenticator.
   */
  @PostConstruct
  public synchronized void init()
    throws ServletException
  {
    super.init();

    reload();
  }

  /**
   * Returns the number of users that are available.
   */
  public int getUserCount()
  {
    return _userMap.size();
  }

  /**
   * Reload the authenticator.
   */
  public synchronized void reload()
    throws ServletException
  {
    if (_path == null)
      return;
    
    try {
      _lastCheck = Alarm.getCurrentTime();
      _depend = new Depend(_path);

      if (log.isLoggable(Level.FINE))
	log.fine(this + " loading users from " + _path);
      
      _userMap = new Hashtable<String,User>();
      
      new Config().configureBean(this, _path);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }
  
  /**
   * Authenticate (login) the user.
   */
  protected Principal loginImpl(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                String userName, String password)
    throws ServletException
  {
    if (isModified())
      reload();

    if  (userName == null)
      return null;

    User user = _userMap.get(userName);
    if (user == null)
      return null;

    if (user.getPassword().equals(password))
      return user.getPrincipal();
    else
      return null;
  }
  
  protected String getDigestPassword(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ServletContext application,
                                     String userName, String realm)
    throws ServletException
  {
    if (isModified())
      reload();

    User user = (User) _userMap.get(userName);
    if (user == null)
      return null;
    else
      return user.getPassword();
  }

  /**
   * Returns true if the user plays the named role.
   *
   * @param request the servlet request
   * @param user the user to test
   * @param role the role to test
   */
  public boolean isUserInRole(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application,
                              Principal principal, String role)
    throws ServletException
  {
    if (principal == null)
      return false;

    String name = principal.getName();

    User user = (User) _userMap.get(name);
    if (user == null)
      return false;

    String []roles = user.getRoles();

    for (int i = roles.length - 1; i >= 0; i--)
      // server/12h2
      if (roles[i].equalsIgnoreCase(role))
        return true;
    
    return false;
  }

  private boolean isModified()
  {
    if (_path == null)
      return false;
    else if (_depend == null)
      return true;
    else if (Alarm.getCurrentTime() < _lastCheck + 5000)
      return false;
    else {
      _lastCheck = Alarm.getCurrentTime();
      return _depend.isModified();
    }
  }

  public static class User {
    private String _name;
    private String _password;
    
    private Principal _principal;
    private String []_roles = new String[0];

    public User()
    {
    }
    
    User(String name, String password, Principal principal)
    {
      _name = name;
      _password = password;
      _principal = principal;
    }

    public void setName(String name)
    {
      _name = name;

      if (_principal == null)
	_principal = new BasicPrincipal(name);
    }

    String getName()
    {
      return _name;
    }

    public void setPassword(String password)
    {
      _password = password;
    }

    String getPassword()
    {
      return _password;
    }

    public void setPrincipal(Principal principal)
    {
      _principal = principal;
    }

    Principal getPrincipal()
    {
      return _principal;
    }

    public void addRoles(String roles)
    {
      int head = 0;
      int length = roles.length();

      while (head < length) {
        int ch;
        
        for (;
             head < length && ((ch = roles.charAt(head)) == ' ' || ch == ',');
             head++) {
        }

        if (head >= length)
          return;

        int tail;
        for (tail = head;
             tail < length &&
               (ch = roles.charAt(tail)) != ' ' &&
               (ch != ',');
             tail++) {
        }

        String role = roles.substring(head, tail);

        addRole(role);

        head = tail;
      }
    }
    
    public void addRole(String role)
    {
      String []newRoles = new String[_roles.length + 1];
      System.arraycopy(_roles, 0, newRoles, 0, _roles.length);
      newRoles[_roles.length] = role;

      _roles = newRoles;
    }

    String []getRoles()
    {
      return _roles;
    }

    public void addText(String userParam)
    {
      int p1 = userParam.indexOf(':');

      if (p1 < 0)
	return;

      String name = userParam.substring(0, p1);
      int p2 = userParam.indexOf(':', p1 + 1);
      String password;
      String roles;

      if (p2 < 0) {
	password = userParam.substring(p1 + 1);
	roles = "user";
      }
      else {
	password = userParam.substring(p1 + 1, p2);
	roles = userParam.substring(p2 + 1);
      }

      setName(name);
      setPassword(password);
      addRoles(roles);
    }
  }
}
