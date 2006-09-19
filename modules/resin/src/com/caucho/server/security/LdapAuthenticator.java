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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import java.io.IOException;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.annotation.*;

import javax.naming.*;
import javax.naming.directory.*;

import java.security.Principal;

import javax.security.auth.Subject;

import javax.security.auth.spi.LoginModule;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;

import com.caucho.config.types.InitParam;

import com.caucho.security.BasicPrincipal;

/**
 * The LDAP authenticator uses the underlying LDAP.
 */
public class LdapAuthenticator extends AbstractAuthenticator {
  private String _userAttribute = "uid";
  private String _passwordAttribute = "userPassword";
  private String _dnPrefix;
  private String _dnSuffix;
  
  private Hashtable<String,String> _jndiEnv =
    new Hashtable<String,String>();

  public LdapAuthenticator()
  {
    _jndiEnv.put(Context.INITIAL_CONTEXT_FACTORY,
		 "com.sun.jndi.ldap.LdapCtxFactory");
    _jndiEnv.put(Context.PROVIDER_URL,
		 "ldap://localhost:389");
  }
  
  public void setDNPrefix(String prefix)
  {
    _dnPrefix = prefix;
  }
  
  public void setDNSuffix(String suffix)
  {
    _dnSuffix = suffix;
  }
  
  public void addJNDIEnv(InitParam init)
  {
    _jndiEnv.putAll(init.getParameters());
  }

  public void setUserAttribute(String user)
  {
    _userAttribute = user;
  }

  public void setPasswordAttribute(String password)
  {
    _passwordAttribute = password;
  }

  /**
   * Initialize the authenticator.
   */
  @PostConstruct
  public synchronized void init()
    throws ServletException
  {
    super.init();
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
    try {
      Hashtable env = new Hashtable();

      env.putAll(_jndiEnv);

      InitialDirContext ic = new InitialDirContext(env);

      String query = _userAttribute + '=' + userName;

      if (_dnPrefix != null && ! _dnPrefix.equals(""))
	query = _dnPrefix + ',' + query;

      if (_dnSuffix != null && ! _dnSuffix.equals(""))
	query = query + ',' + _dnSuffix;

      Attributes attributes = ic.getAttributes(query);

      if (log.isLoggable(Level.FINE))
	log.fine("ldap-authenticator: " + query + "->" + (attributes != null));

      if (attributes == null)
	return null;

      Attribute passwordAttr = attributes.get(_passwordAttribute);

      if (passwordAttr == null)
	return null;
      
      String ldapPassword = (String) passwordAttr.get();

      if (! password.equals(ldapPassword))
	return null;

      return new BasicPrincipal(userName);
    } catch (NamingException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
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
    return principal != null;
  }
}
