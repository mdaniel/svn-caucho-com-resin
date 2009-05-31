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

package com.caucho.bam;

/**
 * A BAM Actor sends and receives messages as the core class in a
 * service-oriented architecture.  The Actor API is used only by the
 * {@link com.caucho.bam.Broker} and is implemented by the Actor developer.
 *
 * <h2>Core API</h2>
 *
 * Each actor has a unique JID, which is the address for messages sent to
 * the actor.  JIDs look like email addresses: harry@caucho.com
 * or harry@caucho.com/browser.
 *
 * {@link com.caucho.bam.ActorStream} is the key customizable interface
 * for an agent developer.  Developers will implement callbacks for each
 * packet type the agent understands.
 *
 * Most developers will extend from {@link com.caucho.bam.SimpleActor}
 * instead of implementing Actor directly.  SimpleActor adds an
 * annotation-based message dispatching system to simplify Actor development.
 *
 * <h2>Child Actors</h2>
 *
 * Some specialized Actors will manage child Actors.  For example, an
 * instant messaging application has a parent Actor: harry@caucho.com and
 * children Actors for each login: harry@caucho.com/phone or
 * harry@caucho.com/browser.  The child section of the Actor API manages
 * these child Actors.
 *
 * <h2>Filters</h2>
 *
 * The filter API is a specialized API needed to support some IM protocols.
 */
public interface Actor
{
  //
  // basic Actor API
  //
  
  /**
   * Returns the actor's jid, so the {@link com.caucho.bam.Broker} can
   * deliver messages to this actor.
   */
  public String getJid();

  /**
   * Set the actor's jid, when the actor is
   * registered with the broker {@link com.caucho.bam.Broker}.
   */
  public void setJid(String jid);

  /**
   * Sets the stream to the broker during registration
   */
  public void setBrokerStream(ActorStream brokerStream);

  /**
   * Returns the custom {@link com.caucho.bam.ActorStream} to the
   * {@link com.caucho.bam.Broker}, so the Broker can send messages to
   * the agent.
   *
   * Developers will customize the ActorStream to receive messages from
   * the Broker.
   */
  public ActorStream getActorStream();

  //
  // child actor management
  //

  /**
   * Requests that a client actor with the given jid be started.
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @param jid the requested jid for a new child
   *
   * @return true if the child is started, false if not
   */
  public boolean startChild(String jid);

  /**
   * Requests that a client actor with the given jid be stopped
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @param jid the requested jid for a new child
   *
   * @return true if the child is stopped, false if not
   */
  public boolean stopChild(String jid);

  /**
   * Called when a client actor starts
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @param jid the jid for the logged-in child actor
   */
  public void onChildStart(String jid);

  /**
   * Called when a client actor stops
   *
   * Examples of Actor children are IM login resources, e.g.
   * harry@caucho.com/browser is a child of harry@caucho.com.
   *
   * @param jid the jid for the logged-in child actor
   */
  public void onChildStop(String jid);

  //
  // filtering
  //
  
  /**
   * Returns a filter for messages to the actor.
   */
  public ActorStream getActorFilter(ActorStream stream);

  /**
   * Returns a filter for messages to the broker.
   */
  public ActorStream getBrokerFilter(ActorStream stream);
}
