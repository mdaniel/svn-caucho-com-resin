/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.security;

import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.ServletException;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.InitParam;
import com.caucho.v5.http.security.AbstractAuthenticator;
import com.caucho.v5.http.security.PasswordCredentials;
import com.caucho.v5.http.security.RolePrincipal;
import com.caucho.v5.util.L10N;

/**
 * The JAAS authenticator uses an existing JAAS LoginModule.  Applications
 * which have existing JAAS modules can use the JaasAuthenticator to
 * log users in based on the old login.
 *
 * <code><pre>
 * &lt;authenticator url="jaas:">
 *   &lt;init login-module="example.MyLogin"/>
 * &lt;/authenticator>
 */
@SuppressWarnings("serial")
public class JaasAuthenticator extends AbstractAuthenticator {
  private static final L10N L = new L10N(JaasAuthenticator.class);
  
  private static final Logger log
    = Logger.getLogger(JaasAuthenticator.class.getName());
  
  private Class<?> _loginModuleClass;

  private HashMap<String,String> _options
    = new HashMap<String,String>();

  public JaasAuthenticator()
  {
    setPasswordDigest(null);
  }

  /**
   * Sets the JAAS spi login module class.
   */
  public void setLoginModule(Class<?> loginModuleClass)
    throws ConfigException
  {
    _loginModuleClass = loginModuleClass;

    Config.checkCanInstantiate(loginModuleClass);

    if (! LoginModule.class.isAssignableFrom(loginModuleClass))
      throw new ConfigException(L.l("'{0}' must implement javax.security.auth.spi.LoginModule",
                                    loginModuleClass.getName()));
  }

  public void setInitParam(InitParam init)
  {
    _options.putAll(init.getParameters());
  }

  public void setOptions(InitParam init)
  {
    _options.putAll(init.getParameters());
  }

  /**
   * Initialize the authenticator.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    super.init();
    
    if (_loginModuleClass == null)
      throw new ServletException(L.l("JaasAuthenticator requires login-module"));
  }
  
  /**
   * Authenticate (login) the user.
   */
  @Override
  protected Principal authenticate(Principal principal,
                                   PasswordCredentials cred,
                                   Object details)
  {
    try {
      String userName = principal.getName();
      String password = new String(cred.getPassword());
      
      Set<Principal> principals = getPrincipals(userName, password);

      if (principals == null || principals.size() == 0)
        return null;
      
      Principal userPrincipal = null;
      Group roles = null;

      for (Principal loginPrincipal : principals) {
        if ("roles".equals(loginPrincipal.getName())
            && loginPrincipal instanceof Group) {
          roles = (Group) loginPrincipal;
        }
        else if (userPrincipal == null)
          userPrincipal = loginPrincipal;
      }
      
      if (userPrincipal == null && roles != null)
        userPrincipal = roles;

      if (userPrincipal instanceof RolePrincipal)
        return userPrincipal;
      else if (userPrincipal != null)
        return new JaasPrincipal(userPrincipal, roles);
      else
        return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the user plays the named role.
   *
   * @param request the servlet request
   * @param user the user to test
   * @param role the role to test
   */
  @Override
  public boolean isUserInRole(Principal principal, String role)
  {
    if (principal == null)
      return false;

    if (principal instanceof RolePrincipal)
      return ((RolePrincipal) principal).isUserInRole(role);
    else
      return "user".equals(role);
  }
  
  private Set<Principal> getPrincipals(String userName,
                                       String password)
  {
    try {
      LoginModule login = (LoginModule) _loginModuleClass.newInstance();
      Subject subject = new Subject();

      HashMap<String,String> state = new HashMap<String,String>();

      state.put("javax.security.auth.login.name", userName);
      state.put("javax.security.auth.login.password", password);

      login.initialize(subject,
                       new Handler(userName, password),
                       state, _options);

      try {
        login.login();
      } catch (Exception e) {
        login.abort();
      }

      login.commit();

      Set<Principal> principals = subject.getPrincipals();

      return principals;
    } catch (LoginException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private static class JaasPrincipal implements RolePrincipal {
    private Principal _principal;
    private Group _roles;
    
    JaasPrincipal(Principal principal, Group roles)
    {
      _principal = principal;
      _roles = roles;
    }
    
    @Override
    public String getName()
    {
      return _principal.getName();
    }
    
    @Override
    public boolean isUserInRole(String role)
    {
      if (_roles == null)
        return "user".equals(role);
      
      Enumeration<? extends Principal> e = _roles.members();
      while (e.hasMoreElements()) {
        Principal principal = e.nextElement();
        
        if (role.equals(principal.getName()))
          return true;
      }
      
      return false;
    }
    
    @Override
    public int hashCode()
    {
      return _principal.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
      if (! (obj instanceof JaasPrincipal))
        return false;
      
      JaasPrincipal principal = (JaasPrincipal) obj;
      
      return getName().equals(principal.getName());
    }
    
    public String toString()
    {
      return _principal.toString();
    }
  }

  static class Handler implements CallbackHandler {
    private String _userName;
    private String _password;

    Handler(String userName, String password)
    {
      _userName = userName;
      _password = password;
    }
    
    public void handle(Callback []callbacks)
      throws IOException, UnsupportedCallbackException
    {
      for (int i = 0; i < callbacks.length; i++) {
        Callback cb = callbacks[i];

        if (cb instanceof NameCallback) {
          NameCallback name = (NameCallback) cb;

          name.setName(_userName);
        }
        else if (cb instanceof PasswordCallback) {
          PasswordCallback password = (PasswordCallback) cb;

          password.setPassword(_password.toCharArray());
        }
      }
    }
  }
}
