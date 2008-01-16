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

import com.caucho.config.types.InitParam;
import com.caucho.security.BasicPrincipal;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Hashtable;
import java.util.logging.*;

/**
 * The LDAP authenticator uses the underlying LDAP services
 * provided by the JDK.
 *
 * <code><pre>
 * &lt;authenticator url="ldap:url=ldap://localhost:389">
 * &lt;/authenticator>
 * </code></pre>
 */
public class LdapAuthenticator extends AbstractPasswordAuthenticator {
  private static final Logger log
    = Logger.getLogger(LdapAuthenticator.class.getName());
  
  private String _userAttribute = "uid";
  private String _passwordAttribute = "userPassword";
  private String _dnPrefix;
  private String _dnSuffix;
  
  private Hashtable<String,String> _jndiEnv
    = new Hashtable<String,String>();

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

  public void setURL(String url)
  {
    _jndiEnv.put(Context.PROVIDER_URL, url);
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
  public void init()
    throws ServletException
  {
    super.init();
  }
  
  /**
   * Authenticate (login) the user.
   */
  protected PasswordUser getUser(String userName)
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

      Principal principal = new BasicPrincipal(userName);
      
      return new PasswordUser(principal, ldapPassword, false,
			      new String[] { "user" });
    } catch (NamingException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }
  }
}
