/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.cloud.jmx;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Remote mbean server
 */
public class RemoteMBeanServer {
  private static final Logger log
    = Logger.getLogger(RemoteMBeanServer.class.getName());
  
  private JmxClient _client;

  public RemoteMBeanServer(String serverId)
  {
    _client = new JmxClient(serverId);
  }
  
  public RemoteMBeanServer(String host, int port, String user, String password)
    throws Exception
  {
    _client = new JmxClient(host, port, user, password);
  }

  public RemoteMBeanServer(String host, int port)
    throws Exception
  {
    this(host, port, null, null);
  }

  /**
   * Looks up the given remote mbean.
   */
  public RemoteMBean lookup(String name)
  {
    try {
      HashMap attr = _client.lookup(name);

      if (attr == null)
	return null;
    
      return new RemoteMBean(_client, name, attr);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Looks up the given remote mbean.
   */
  public boolean isRegistered(String name)
  {
    try {
      return _client.lookup(name) != null;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Returns an array of names that match a JMX pattern.
   * If the name contains a ":", it is a query in the global jmx namespace.
   * If the name does not contain a ":", it is a search in the JMX namespace
   * of the current web application.
   */
  public String []query(String pattern)
  {
    try {
      return _client.query(pattern);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String toString()
  {
    return "RemoteMBeanServer[" + _client + "]";
  }
}
