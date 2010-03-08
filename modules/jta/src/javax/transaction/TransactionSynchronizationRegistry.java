/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package javax.transaction;

/**
 * This interface is intended for use by system level application server
 * components such as persistence managers, resource adapters, as well as EJB
 * and Web application components. This provides the ability to register
 * synchronization objects with special ordering semantics, associate resource
 * objects with the current transaction, get the transaction context of the
 * current transaction, get current transaction status, and mark the current
 * transaction for roll-back. This interface is implemented by the application
 * server by a stateless service object. The same object can be used by any
 * number of components with thread safety.
 * 
 * In standard application server environments, an instance implementing this
 * interface can be looked up by a standard name via JNDI. The standard name is
 * java:comp/TransactionSynchronizationRegistry.
 * 
 * @Since JTA 1.1
 */
public interface TransactionSynchronizationRegistry {

  /**
   * Return an opaque object to represent the transaction bound to the current
   * thread at the time this method is called. This object overrides hashCode
   * and equals to allow its use as the key in a hashMap for use by the caller.
   * If there is no transaction currently active, return null.
   * 
   * This object will return the same hashCode and compare equal to all other
   * objects returned by calling this method from any component executing in the
   * same transaction context in the same application server.
   * 
   * The toString method returns a String that might be usable by a human reader
   * to usefully understand the transaction context. The toString result is
   * otherwise not defined. Specifically, there is no forward or backward
   * compatibility guarantee of the results of toString.
   * 
   * The object is not necessarily serializable, and has no defined behavior
   * outside the virtual machine whence it was obtained.
   * 
   * @return An opaque object representing the transaction bound to the current
   *         thread at the time this method is called.
   */
  public Object getTransactionKey();

  /**
   * Add or replace an object in the Map of resources being managed for the
   * transaction bound to the current thread at the time this method is called.
   * The supplied key should be of an caller-defined class so as not to conflict
   * with other users. The class of the key must guarantee that the hashCode and
   * equals methods are suitable for use as keys in a map. The key and value are
   * not examined or used by the implementation. The general contract of this
   * method is that of Map.put(Object, Object) for a Map that supports non-null
   * keys and null values. For example, if there is already an value associated
   * with the key, it is replaced by the value parameter.
   * 
   * @param key
   *          The key for the Map entry.
   * @param value
   *          The value for the Map entry.
   * @throws IllegalStateException
   *           If no transaction is active.
   * @throws NullPointerException
   *           If the parameter key is null.
   */
  public void putResource(Object key, Object value);

  /**
   * Get an object from the Map of resources being managed for the transaction
   * bound to the current thread at the time this method is called. The key
   * should have been supplied earlier by a call to putResouce in the same
   * transaction. If the key cannot be found in the current resource Map, null
   * is returned. The general contract of this method is that of Map.get(Object)
   * for a Map that supports non-null keys and null values. For example, the
   * returned value is null if there is no entry for the parameter key or if the
   * value associated with the key is actually null.
   * 
   * @param key
   *          The key for the Map entry.
   * @return The value associated with the key.
   * @throws IllegalStateException
   *           If no transaction is active.
   * @throws NullPointerException
   *           If the parameter key is null.
   */
  public Object getResource(Object key);

  /**
   * Register a Synchronization instance with special ordering semantics. Its
   * beforeCompletion will be called after all SessionSynchronization
   * beforeCompletion call-backs and call-backs registered directly with the
   * Transaction, but before the 2-phase commit process starts. Similarly, the
   * afterCompletion callback will be called after 2-phase commit completes but
   * before any SessionSynchronization and Transaction afterCompletion
   * call-backs.
   * 
   * The beforeCompletion callback will be invoked in the transaction context of
   * the transaction bound to the current thread at the time this method is
   * called. Allowable methods include access to resources, e.g. Connectors. No
   * access is allowed to "user components" (e.g. timer services or bean
   * methods), as these might change the state of data being managed by the
   * caller, and might change the state of data that has already been flushed by
   * another caller of registerInterposedSynchronization. The general context is
   * the component context of the caller of registerInterposedSynchronization.
   * 
   * The afterCompletion callback will be invoked in an undefined context. No
   * access is permitted to "user components" as defined above. Resources can be
   * closed but no transactional work can be performed with them.
   * 
   * If this method is invoked without an active transaction context, an
   * IllegalStateException is thrown.
   * 
   * If this method is invoked after the two-phase commit processing has
   * started, an IllegalStateException is thrown.
   * 
   * @param synchronization
   *          The Synchronization instance.
   * @throws IllegalStateException
   *           If no transaction is active.
   */
  public void registerInterposedSynchronization(Synchronization synchronization);

  /**
   * Return the status of the transaction bound to the current thread at the
   * time this method is called. This is the result of executing
   * TransactionManager.getStatus() in the context of the transaction bound to
   * the current thread at the time this method is called.
   * 
   * @return The status of the transaction bound to the current thread at the
   *         time this method is called.
   */
  public int getTransactionStatus();

  /**
   * Set the rollbackOnly status of the transaction bound to the current thread
   * at the time this method is called.
   * 
   * @throws IllegalStateException
   *           If no transaction is active.
   */
  public void setRollbackOnly();

  /**
   * Get the rollbackOnly status of the transaction bound to the current thread
   * at the time this method is called.
   * 
   * @return The rollbackOnly status.
   * @throws IllegalStateException
   *           If no transaction is active.
   */
  public boolean getRollbackOnly();
}