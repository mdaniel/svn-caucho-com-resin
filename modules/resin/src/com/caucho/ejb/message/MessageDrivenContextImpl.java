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

package com.caucho.ejb.message;

import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.TransactionContext;
import com.caucho.util.L10N;

import javax.ejb.MessageDrivenContext;
import javax.ejb.EJBHome;
import javax.transaction.UserTransaction;
import java.util.logging.Logger;

/**
 * Server container for a message bean.
 */
public class MessageDrivenContextImpl extends AbstractContext
  implements MessageDrivenContext
{
  protected static final L10N L = new L10N(MessageDrivenContextImpl.class);
  protected static final Logger log
    = Logger.getLogger(MessageDrivenContextImpl.class.getName());

  private MessageServer _server;
  private UserTransaction _ut;
  private boolean _isContainerTransaction;
  private boolean _isRollbackOnly;

  MessageDrivenContextImpl(MessageServer server,
                           UserTransaction ut,
                           boolean isContainerTransaction)
  {
    _server = server;
    _ut = ut;
    _isContainerTransaction = isContainerTransaction;
  }

  public AbstractServer getServer()
  {
    return _server;
  }

  public boolean isCMT()
  {
    return _isContainerTransaction;
  }

  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    throw new IllegalStateException(L.l("Message-driven beans may not use getEJBHome()"));
  }

  /**
   * Returns the current UserTransaction.  Only Session beans with
   * bean-managed transactions may use this.
   */
  public UserTransaction getUserTransaction()
    throws IllegalStateException
  {
    if (isCMT())
      throw new IllegalStateException(L.l("Container-managed message-driven beans may not use getUserTransaction()"));

    return _ut;
  }

  /**
   * Forces a rollback of the current transaction.
   */
  void clearRollbackOnly()
  {
    _isRollbackOnly = false;
  }

  /**
   * Forces a rollback of the current transaction.
   */
  public void setRollbackOnly()
    throws IllegalStateException
  {
    if (! isCMT())
      throw new IllegalStateException("setRollbackOnly may not be called from a bean-managed transaction");

    TransactionContext trans = getServer().getTransaction();

    if (trans != null)
      trans.setRollbackOnly();
    else
      throw new IllegalStateException("invalid transaction");
  }

  /**
   * Forces a rollback of the current transaction.
   */
  public boolean getRollbackOnly()
    throws IllegalStateException
  {
    if (! isCMT())
      throw new IllegalStateException("getRollbackOnly may not be called from a bean-managed transaction");

    TransactionContext trans = getServer().getTransaction();

    if (trans == null)
      throw new IllegalStateException("getRollbackOnly requires a valid container-managed transaction");

    return trans.getRollbackOnly();
  }
}
