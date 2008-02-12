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

package com.caucho.jca.cfg;

import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.webbeans.cfg.*;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.*;
import javax.mail.*;

/**
 * Configuration for a javamail.
 */
public class JavaMailConfig extends AbstractBeanConfig {
  private static final L10N L = new L10N(JavaMailConfig.class);
  private static final Logger log
    = Logger.getLogger(JavaMailConfig.class.getName());

  private Properties _props = new Properties();
  private Authenticator _auth;
  
  public JavaMailConfig()
  {
  }

  /**
   * Sets the authenticator
   */
  public void setAuthenticator(Authenticator auth)
  {
    _auth = auth;
  }

  //
  // well-known attributes
  //

  /**
   * mail.from
   */
  public void setFrom(String from)
  {
    setProperty("mail.from", from);
  }

  /**
   * mail.host
   */
  public void setHost(String host)
  {
    setProperty("mail.host", host);
  }

  /**
   * mail.imap.host
   */
  public void setImapHost(String host)
  {
    setProperty("mail.imap.host", host);
  }

  /**
   * mail.imap.user
   */
  public void setImapUser(String user)
  {
    setProperty("mail.imap.user", user);
  }

  /**
   * mail.pop3.host
   */
  public void setPop3Host(String host)
  {
    setProperty("mail.pop3.host", host);
  }

  /**
   * mail.pop3.user
   */
  public void setPop3User(String user)
  {
    setProperty("mail.pop3.user", user);
  }

  /**
   * mail.smtp.host
   */
  public void setSmtpHost(String host)
  {
    setProperty("mail.smtp.host", host);
  }

  /**
   * mail.smtp.user
   */
  public void setSmtpUser(String user)
  {
    setProperty("mail.smtp.user", user);
  }

  /**
   * mail.store.protocol
   */
  public void setStoreProtocol(String protocol)
  {
    setProperty("mail.store.protocol", protocol);
  }

  /**
   * mail.transport.protocol
   */
  public void setTransportProtocol(String protocol)
  {
    setProperty("mail.transport.protocol", protocol);
  }

  /**
   * mail.user
   */
  public void setUser(String user)
  {
    setProperty("mail.user", user);
  }

  /**
   * Sets an attribute.
   */
  public void setProperty(String name, String value)
  {
    _props.put(name, value);
  }

  public void setProperties(Properties props)
  {
    _props.putAll(props);
  }

  public void setValue(Properties props)
  {
    _props.putAll(props);
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      if (getInit() != null)
	getInit().configure(this);
      
      Session session;
      
      if (_auth != null)
	session = Session.getInstance(_props, _auth);
      else
	session = Session.getInstance(_props);

      register(session);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
