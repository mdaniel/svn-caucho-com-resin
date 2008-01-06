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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.cfg;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.jms.*;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.jms.queue.AbstractQueue;
import com.caucho.naming.*;
import com.caucho.webbeans.cfg.AbstractBeanConfig;

import com.caucho.util.*;

/**
 * jms-queue configuration
 */
public class JmsQueueConfig extends AbstractBeanConfig
{
  private static final L10N L = new L10N(JmsQueueConfig.class);
  private static final Logger log
    = Logger.getLogger(JmsQueueConfig.class.getName());

  private static HashMap<String,Class> _urlMap
    = new  HashMap<String,Class>();

  private String _url;

  /**
   * Sets the JMS URL for the queue
   */
  public void setUrl(String url)
  {
    _url = url;
  }

  /**
   * Initialize the queue.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (getInstanceClass() != null) {
      register();

      return;
    }
    
    if (_url == null)
      throw new ConfigException(L.l("<jms-queue> requires a url attribute"));

    int p = _url.indexOf(':');
    if (p < 0)
      throw new ConfigException(L.l("'{0}' expects a 'scheme:' syntax.  The <jms-queue> url syntax is 'scheme:prop1=value1;prop2=value2'"));

    String scheme = _url.substring(0, p);
    String param = _url.substring(p + 1);
    Class cl = _urlMap.get(scheme);

    if (cl == null) {
      throw new ConfigException(L.l("'{0}' is an unknown <jms-queue> scheme.",
				    _url));
    }

    AbstractQueue queue = (AbstractQueue) cl.newInstance();

    if (getName() != null)
      queue.setName(getName());
    else if (getJndiName() != null)
      queue.setName(getJndiName());

    for (String paramValue : param.split(";")) {
      if (paramValue.equals(""))
	continue;
      
      p = paramValue.indexOf('=');

      if (p < 0)
	throw new ConfigException(L.l("'{0}' has an incorrect parameter syntax.  The <jms-queue> url syntax is 'scheme:prop1=value1;prop2=value2'"));

      String name = paramValue.substring(0, p);
      String value = paramValue.substring(p + 1);

      Config.setStringAttribute(queue, name, value);
    }

    if (getInit() != null)
      getInit().configure(queue);

    queue.postConstruct();

    register(queue);
  }

  static {
    try {
      Class cl = Class.forName("com.caucho.jms.cluster.ClientQueue");
      _urlMap.put("client", cl);
      
      cl = Class.forName("com.caucho.jms.cluster.ServerQueue");
      _urlMap.put("cluster", cl);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    
    _urlMap.put("file", com.caucho.jms.file.FileQueue.class);
    _urlMap.put("jdbc", com.caucho.jms.jdbc.JdbcQueue.class);
    _urlMap.put("memory", com.caucho.jms.memory.MemoryQueue.class);
  }
}

