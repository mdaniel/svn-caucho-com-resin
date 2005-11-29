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

import java.lang.reflect.Constructor;

import java.util.Properties;

import java.net.InetAddress;

import java.io.PrintStream;

/**
 * Represents a mail session.
 */
public final class Session {
  private static Session _defaultSession;

  private Properties _properties;

  private boolean _isDebug;
  private PrintStream _debugOut;

  private Session(Properties props, Authenticator authenticator)
  {
    if (props != null)
      _properties = new Properties(props);
    else
      _properties = new Properties();
  }
  
  /**
   * Returns a new session object.
   */
  public static Session getInstance(Properties props,
				    Authenticator authenticator)
  {
    return new Session(props, authenticator);
  }
  
  /**
   * Returns a new session object.
   */
  public static Session getInstance(Properties props)
  {
    return getInstance(props, null);
  }
  
  /**
   * Returns the default session object.
   */
  public static Session getDefaultInstance(Properties props,
					   Authenticator authenticator)
  {
    if (_defaultSession == null) {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(ClassLoader.getSystemClassLoader());

	_defaultSession = getInstance(props, authenticator);
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }

    return _defaultSession;
  }
  
  /**
   * Sets the debug value.
   */
  public void setDebug(boolean debug)
  {
    _isDebug = debug;
  }
  
  /**
   * Gets the debug value.
   */
  public boolean getDebug()
  {
    return _isDebug;
  }
  
  /**
   * Sets the debug output stream;
   */
  public void setDebugOut(PrintStream out)
  {
    _debugOut = out;
  }
  
  /**
   * Gets the debug output stream;
   */
  public PrintStream getDebugOut()
  {
    return _debugOut;
  }

  /**
   * Returns an array of all the implementations.
   */
  public Provider []getProviders()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns an array of all the implementations.
   */
  public Provider getProvider(String protocol)
    throws NoSuchProviderException
  {
    Provider provider = new Provider();

    provider.setClassName("com.caucho.mail.smtp.SmtpTransport");

    return provider;
  }
  
  /**
   * Sets a given provider
   */
  public void setProvider(Provider provider)
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the store.
   */
  public Store getStore()
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the store.
   */
  public Store getStore(String protocol)
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the store.
   */
  public Store getStore(URLName url)
    throws NoSuchProviderException
  {
    return getStore(url.getProtocol());
  }

  /**
   * Returns the store.
   */
  public Store getStore(Provider provider)
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a folder for the URL name.
   */
  public Folder getFolder(URLName url)
    throws MessagingException
  {
    Store store = getStore(url);
    
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a transport.
   */
  public Transport getTransport()
    throws NoSuchProviderException
  {
    return getTransport(getProperty("mail.transport.protocol"));
  }

  /**
   * Returns a transport.
   */
  public Transport getTransport(String protocol)
    throws NoSuchProviderException
  {
    Provider provider = getProvider(protocol);

    String className = provider.getClassName();

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      Constructor cons = cl.getConstructor(new Class[] { Session.class, URLName.class });

      Transport transport = (Transport) cons.newInstance(new Object[] { this, null });

      return transport;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a transport.
   */
  public Transport getTransport(URLName url)
    throws NoSuchProviderException
  {
    return getTransport(url.getProtocol());
  }

  /**
   * Returns a transport.
   */
  public Transport getTransport(Provider provider)
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a transport.
   */
  public Transport getTransport(Address address)
    throws NoSuchProviderException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets the password authentication
   */
  public void setPasswordAuthentication(URLName url,
					PasswordAuthentication pw)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the password authentication
   */
  public PasswordAuthentication getPasswordAuthentication(URLName url)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Application callback for the username and password.
   */
  public PasswordAuthentication
    requestPasswordAuthentication(InetAddress addr,
				  int port,
				  String protocol,
				  String prompt,
				  String defaultUserName)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the properties.
   */
  public Properties getProperties()
  {
    return _properties;
  }

  /**
   * Returns the property;
   */
  public String getProperty(String name)
  {
    String prop = getProperties().getProperty(name);

    if (prop == null)
      prop = System.getProperty(name);
    
    return prop;
  }
}
