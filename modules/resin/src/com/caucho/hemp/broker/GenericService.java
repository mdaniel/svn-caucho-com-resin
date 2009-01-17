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

package com.caucho.hemp.broker;

import com.caucho.bam.AbstractBamStream;
import com.caucho.bam.BamBroker;
import com.caucho.bam.BamError;
import com.caucho.bam.BamService;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamConnection;
import com.caucho.bam.QueryGet;
import com.caucho.bam.SimpleBamService;
import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.xmpp.disco.DiscoInfoQuery;
import com.caucho.xmpp.disco.DiscoIdentity;
import com.caucho.xmpp.disco.DiscoItem;
import com.caucho.xmpp.disco.DiscoItemsQuery;
import com.caucho.xmpp.disco.DiscoFeature;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * GenericService implementation to simplify configuring a service.
 */
public class GenericService extends SimpleBamService
{
  private static final L10N L = new L10N(GenericService.class);
  private static final Logger log
    = Logger.getLogger(GenericService.class.getName());
  
  private @In(optional=true) BamBroker _broker;
  
  private String _jid;
  
  private BamStream _brokerStream;
  private BamStream _agentStream;

  public void setName(String name)
  {
    setJid(name);
  }

  public void setJid(String jid)
  {
    _jid = jid;
  }
  
  /**
   * Returns the service's jid.
   */
  public String getJid()
  {
    return _jid;
  }

  public void setBroker(BamBroker broker)
  {
    _broker = broker;
  }

  protected BamBroker getBroker()
  {
    return _broker;
  }
  
  public BamStream getBrokerStream()
  {
    return _brokerStream;
  }

  @PostConstruct
  public void init()
  {
    /*
    if (getJid() == null)
      throw new ConfigException(L.l("{0} requires a jid",
				    getClass().getSimpleName()));

    _agentStream = createQueue(this);

    // _broker.addService(this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");

    _brokerStream = _broker.getBrokerStream();
    */
  }

  protected BamStream createQueue(BamStream stream)
  {
    return new HempMemoryQueue(stream, _broker.getBrokerStream());
  }

  public BamStream getAgentStream()
  {
    return _agentStream;
  }
  
  //
  // BamService API
  //
  
  /**
   * Create a filter for requests sent to the service's agent.
   */
  public BamStream getAgentFilter(BamStream agentStream)
  {
    return agentStream;
  }
  
  /**
   * Create a filter for requests sent by the service to the broker.
   */
  public BamStream getBrokerFilter(BamStream brokerStream)
  {
    return brokerStream;
  }

  /**
   * Callback when a child agent logs in.
   */
  public void onAgentStart(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onAgentStart(" + jid + ")");
  }

  /**
   * Callback when a child agent logs out.
   */
  public void onAgentStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onAgentStop(" + jid + ")");
  }

  /**
   * Requests that a child agent be started.
   */
  public boolean startAgent(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " startAgent(" + jid + ")");
    
    return false;
  }

  /**
   * Requests that a child agent be stopped.
   */
  public boolean stopAgent(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " startAgent(" + jid + ")");
    
    return false;
  }
  
  @QueryGet
  public boolean handleDiscoInfoQuery(long id, String to, String from,
				      DiscoInfoQuery query)
  {
    _brokerStream.queryResult(id, from, to,
                              new DiscoInfoQuery(getDiscoIdentity(),
                                                 getDiscoFeatures()));

    return true;
  }

  @QueryGet
  public boolean handleDiscoItemsQuery(long id, String to, String from,
				       DiscoItemsQuery query)
  {
    DiscoItemsQuery result = new DiscoItemsQuery();
    result.setItems(getDiscoItems());

    _brokerStream.queryResult(id, from, to, result);

    return true;
  }

  protected DiscoItem[] getDiscoItems()
  {
    return new DiscoItem[] {};
  }

  /**
   * Returns the disco identity of the resource
   */
  protected DiscoIdentity []getDiscoIdentity()
  {
    return new DiscoIdentity[] {
      new DiscoIdentity(getDiscoCategory(), getDiscoType()),
    };
  }

  /**
   * Returns the disco features of the resource
   */
  protected DiscoFeature []getDiscoFeatures()
  {
    ArrayList<String> featureNames = new ArrayList<String>();

    getDiscoFeatureNames(featureNames);

    DiscoFeature []features = new DiscoFeature[featureNames.size()];

    for (int i = 0; i < featureNames.size(); i++)
      features[i] = new DiscoFeature(featureNames.get(i));

    return features;
  }

  protected String getDiscoCategory()
  {
    return "x-application";
  }

  protected String getDiscoType()
  {
    return getClass().getSimpleName();
  }

  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    featureNames.add(DiscoInfoQuery.FEATURE);
  }

  @PreDestroy
  protected void destroy()
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }
}
