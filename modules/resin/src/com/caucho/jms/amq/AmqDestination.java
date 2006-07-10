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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.amq;

import java.util.logging.*;

import java.io.*;

import javax.jms.*;

import com.caucho.util.*;

import com.caucho.config.ConfigException;

import com.caucho.jms.*;
import com.caucho.jms.selector.*;
import com.caucho.jms.message.*;
import com.caucho.jms.session.*;

/**
 * An AMQ destination.
 */
public class AmqDestination extends AbstractDestination
  implements Destination
{
  private static final Logger log
    = Logger.getLogger(AmqDestination.class.getName());
  private static final L10N L = new L10N(AmqDestination.class);

  private int _id;
  
  private String _url;
  private String _host;
  private int _port;
  private String _name;

  private AmqClient _client;

  public AmqDestination()
  {
  }

  /**
   * Gets the name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the URL
   */
  public void setUrl(String url)
    throws ConfigException
  {
    _url = url;

    if (! url.startsWith("amq://"))
      throw new ConfigException(L.l("AMQ URL '{0}' must start with 'amq://'",
				    url));

    url = url.substring("amq://".length());
    int p = url.indexOf('/');
    String host;
    String tail;

    if (p >= 0) {
      host = url.substring(0, p);
      tail = url.substring(p + 1);
    }
    else {
      throw new ConfigException(L.l("AMQ URL '{0}' is missing destination name",
				    _url));
    }

    p = host.indexOf(':');
    if (p < 0)
      throw new ConfigException(L.l("AMQ URL '{0}' is missing port name",
				    _url));

    _host = host.substring(0, p);
    _port = Integer.parseInt(host.substring(p + 1));

    _name = tail;
  }

  /**
   * Returns true for a topic.
   */
  public boolean isTopic()
  {
    return false;
  }

  /**
   * Initializes the JdbcQueue
   */
  public void init()
    throws ConfigException
  {
    if (_url == null)
      throw new ConfigException(L.l("URL must be set for an AMQ Queue or Topic."));

    _client = new AmqClient(_host, _port);
  }

  protected AmqClient getClient()
  {
    return _client;
  }

  /**
   * Sends the message to the queue.
   */
  public void send(Message message)
    throws JMSException
  {
    throw new UnsupportedOperationException();
    /*
    try {
      AmqClient client = getClient();

      AmqChannel channel = client.openChannel();
      try {
	System.out.println("CHANNEL: " + channel);
      } finally {
	channel.close();
      }
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
    */
  }

  public void close()
  {
    _client.close();
  }

  public void finalize()
  {
    close();
  }
}

