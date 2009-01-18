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

package com.caucho.hemp.pubsub.memory;

import com.caucho.xmpp.pubsub.PubSubPublish;
import com.caucho.bam.BamError;
import com.caucho.bam.QuerySet;
import com.caucho.bam.SimpleBamService;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * pub/sub (xep-0060)
 * http://www.xmpp.org/extensions/xep-0060.html
 */
public class MemoryPubSub extends SimpleBamService
{
  private static final Logger log
    = Logger.getLogger(MemoryPubSub.class.getName());

  public static final String PUBSUB_FEATURE
    = "http://jabber.org/protocol/pubsub";

  private HashMap<String,MemoryNode> _nodeMap
    = new HashMap<String,MemoryNode>();

  /**
   * Adds a node
   */
  public void addNode(MemoryNode node)
  {
    _nodeMap.put(node.getName(), node);

    node.setService(this);
  }

  public MemoryNode getNode(String node)
  {
    return _nodeMap.get(node);
  }

  /**
   * Returns the XMPP discovery catetory
   */
  /*
  @Override
  protected String getDiscoCategory()
  {
    return "pubsub";
  }
  */

  /**
   * Returns the XMPP discovery type
   */
  /*
  @Override
  protected String getDiscoType()
  {
    return "service";
  }
  */

  /**
   * Returns the features supported by this service
   */
  /*
  @Override
  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    super.getDiscoFeatureNames(featureNames);
    
    featureNames.add(PUBSUB_FEATURE);
  }
  */

  /**
   * Implements the queries
   */
  @QuerySet
  public boolean querySet(long id,
			  String to,
			  String from,
			  PubSubPublish publish)
  {
    MemoryNode node = getNode(publish.getNode());

    if (node == null) {
      getBrokerStream().queryError(id, from, to, publish,
				   new BamError(BamError.TYPE_CANCEL,
						"no-node"));
      return true;
    }

    node.publish(publish.getItems());
      
    getBrokerStream().queryResult(id, from, to, null);

    return true;
  }
}
