/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.quercus.lib;

import com.caucho.Version;
import com.caucho.naming.Jndi;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import javax.transaction.UserTransaction;
import javax.transaction.SystemException;
import javax.transaction.NotSupportedException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class ResinModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ResinModule.class);

  public final static int XA_STATUS_ACTIVE = 0;
  public final static int XA_STATUS_MARKED_ROLLBACK = 1;
  public final static int XA_STATUS_PREPARED = 2;
  public final static int XA_STATUS_COMMITTED = 3;
  public final static int XA_STATUS_ROLLEDBACK = 4;
  public final static int XA_STATUS_UNKNOWN = 5;
  public final static int XA_STATUS_NO_TRANSACTION = 6;
  public final static int XA_STATUS_PREPARING = 7;
  public final static int XA_STATUS_COMMITTING = 8;
  public final static int XA_STATUS_ROLLING_BACK = 9;

  /**
   * Perform a jndi lookup to retrieve an object.
   *
   * @param name a fully qualified name "java:comp/env/foo", or a short-form "foo".
   *
   * @return the object, or null if it is not found.
   */
  public static Object jndi_lookup(String name)
  {
    return Jndi.lookup(name);
  }


  /**
   * Returns the version of the Resin server software.
   */
  public static String resin_version()
  {
    return Version.FULL_VERSION;
  }

  private static UserTransaction getUserTransaction()
    throws NamingException
  {
    return ((UserTransaction) new InitialContext().lookup("java:comp/UserTransaction"));
  }

  public static void xa_begin()
    throws NamingException, SystemException, NotSupportedException
  {
    getUserTransaction().begin();
  }

  public static void xa_commit()
    throws NamingException, HeuristicMixedException, SystemException,
           HeuristicRollbackException, RollbackException
  {
    getUserTransaction().commit();
  }

  public static void xa_rollback()
    throws NamingException, SystemException
  {
    getUserTransaction().rollback();
  }

  public static void xa_rollback_only()
    throws NamingException, SystemException
  {
    getUserTransaction().setRollbackOnly();
  }

  public static void xa_set_timeout(int timeoutSeconds)
    throws NamingException, SystemException
  {
    getUserTransaction().setTransactionTimeout(timeoutSeconds);
  }

  public static int xa_status()
    throws NamingException, SystemException
  {
    return getUserTransaction().getStatus();
  }
}
