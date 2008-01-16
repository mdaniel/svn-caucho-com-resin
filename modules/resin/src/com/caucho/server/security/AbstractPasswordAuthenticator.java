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
import java.util.*;
import java.util.logging.*;
import java.io.*;

/**
 * Base class for authenticators which lookup passwords from a database.
 *
 * Implementations only need to override the <code>getUser</code> method
 * and return a populated <code>PasswordUser</code>.  Since
 * <code>PasswordUser</code> already contains role information, the
 * abstract authenticator can handle any authentication or authorization.
 */
abstract public class AbstractPasswordAuthenticator
  extends AbstractAuthenticator
{
  private static final Logger log =
    Logger.getLogger(AbstractPasswordAuthenticator.class.getName());

  /**
   * Abstract method to retrn a user based on the name
   *
   * @param userName the string user name
   * @return the populated PasswordUser value
   */
  abstract protected PasswordUser getUser(String userName);

  /**
   * Returns the user based on a principal
   */
  protected PasswordUser getUser(Principal principal)
  {
    return getUser(principal.getName());
  }

  /**
   * Default implementation of basic username/password login
   */
  @Override
  protected Principal loginImpl(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                String userName, String password)
    throws ServletException
  {
    if  (userName == null)
      return null;

    PasswordUser user = getUser(userName);
    if (user == null || user.isDisabled())
      return null;

    if (user.getPassword().equals(password)) {
      return user.getPrincipal();
    }
    else {
      return null;
    }
  }
  
  /**
   * Default implementation of basic username/password login
   */
  protected String getDigestPassword(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ServletContext application,
                                     String userName, String realm)
    throws ServletException
  {
    PasswordUser user = getUser(userName);
    if (user == null || user.isDisabled())
      return null;
    else
      return user.getPassword();
  }

  /**
   * Default implementation to return true if the user is in a role
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

    PasswordUser user = getUser(principal);
    if (user == null)
      return false;

    for (String userRole : user.getRoles()) {
      // server/12h2
      if (userRole.equalsIgnoreCase(role))
        return true;
    }
    
    return false;
  }
}
