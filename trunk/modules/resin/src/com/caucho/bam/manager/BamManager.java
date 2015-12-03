/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.bam.manager;

import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.Agent;
import com.caucho.bam.actor.BamActorRef;
import com.caucho.bam.actor.ManagedActor;
import com.caucho.bam.broker.Broker;
import com.caucho.bam.mailbox.Mailbox;
import com.caucho.bam.mailbox.MailboxType;
import com.caucho.bam.stream.MessageStream;


/**
 * Broker is the hub which routes messages to mailboxes.
 */
public interface BamManager
{
  /**
   * Returns the managed broker
   */
  public Broker getBroker();
  
  /**
   * Adds a mailbox to the broker.
   */
  void addMailbox(String address, Mailbox mailbox);
  
  /**
   * Removes a mailbox
   */
  public void removeMailbox(Mailbox mailbox);
  
  /**
   * Adds an actor and creates a default mailbox
   */
  public void addActor(String address, ManagedActor actor);
  
  /**
   * Creates an agent
   */
  public Agent createAgent(MessageStream actorStream);
    
  /**
   * Creates an agent
   */
  public Agent createAgent(MessageStream actorStream,
                           MailboxType mailboxType);

  /**
   * @param proxyAddress
   * @param deployActorProxyImpl
   * @return
   */
  public Mailbox createService(String address,
                               Object bean);

  public Mailbox createClient(Mailbox next,
                              String uid,
                              String resource);
  
  public BamActorRef createActorRef(String to);
  
  public ActorSender createClient(String uid, String resource);
  
  public <T> T createProxy(Class<T> api, String to);
  
  public <T> T createProxy(Class<T> api, BamActorRef to, ActorSender sender);

  public <T> T createProxy(Class<T> api,
                           String to,
                           ActorSender sender);
}
