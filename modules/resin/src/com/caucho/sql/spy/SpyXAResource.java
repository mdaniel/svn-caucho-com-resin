/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.sql.spy;

import java.io.*;
import java.sql.*;
import javax.sql.*;
import java.util.*;
import javax.transaction.xa.*;
import java.util.logging.*;

import com.caucho.util.L10N;
import com.caucho.log.Log;

/**
 * Spying on a connection.
 */
public class SpyXAResource implements XAResource {
  protected final static Logger log = Log.open(XAResource.class);
  protected final static L10N L = new L10N(XAResource.class);

  // The underlying resource
  private XAResource _xaResource;

  private int _id;

  /**
   * Creates a new SpyXAResource
   */
  public SpyXAResource(int id, XAResource resource)
  {
    _xaResource = resource;
    _id = id;
  }

  /**
   * Sets the transaction timeout.
   */
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    try {
      boolean ok = _xaResource.setTransactionTimeout(seconds);
      log.info(_id + ":set-transaction-timeout(" + seconds + ")->" + ok);

      return ok;
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Gets the transaction timeout.
   */
  public int getTransactionTimeout()
    throws XAException
  {
    try {
      int seconds = _xaResource.getTransactionTimeout();
      
      log.info(_id + ":transaction-timeout()->" + seconds);

      return seconds;
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Returns true if the underlying RM is the same.
   */
  public boolean isSameRM(XAResource resource)
    throws XAException
  {
    try {
      boolean same = _xaResource.isSameRM(resource);
      
      log.info(_id + ":is-same-rm(resource=" + resource + ")->" + same);

      return same;
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  public void start(Xid xid, int flags)
    throws XAException
  {
    try {
      String flagName = "";

      if ((flags & TMJOIN) != 0)
        flagName += ",join";
      if ((flags & TMRESUME) != 0)
        flagName += ",resume";
      
      log.info(_id + ":start(xid=" + xid + flagName + ")");

      _xaResource.start(xid, flags);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  public void end(Xid xid, int flags)
    throws XAException
  {
    try {
      String flagName = "";

      if ((flags & TMFAIL) != 0)
        flagName += ",fail";
      if ((flags & TMSUSPEND) != 0)
        flagName += ",suspend";
      
      log.info(_id + ":end(xid=" + xid + flagName + ")");

      _xaResource.end(xid, flags);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public int prepare(Xid xid)
    throws XAException
  {
    try {
      int value = _xaResource.prepare(xid);
      log.info(_id + ":prepare(xid=" + xid + ")->" + value);

      return value;
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Commits the resource
   */
  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    try {
      log.info(_id + ":commit(xid=" + xid + (onePhase ? ",1P" : ",2P)"));

      _xaResource.commit(xid, onePhase);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public void rollback(Xid xid)
    throws XAException
  {
    try {
      log.info(_id + ":rollback(xid=" + xid + ")");

      _xaResource.rollback(xid);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public Xid []recover(int flags)
    throws XAException
  {
    try {
      log.info(_id + ":recover(flags=" + flags + ")");

      return _xaResource.recover(flags);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  /**
   * Forgets the transaction
   */
  public void forget(Xid xid)
    throws XAException
  {
    try {
      log.info(_id + ":forget(xid=" + xid + ")");

      _xaResource.forget(xid);
    } catch (XAException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.INFO, e.toString(), e);
      throw e;
    }
  }

  public String toString()
  {
    return "SpyXAResource[id=" + _id + ",resource=" + _xaResource + "]";
  }
}
