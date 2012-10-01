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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.sql.spy;

import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spying on a connection.
 */
public class SpyXAResource implements XAResource {
  protected final static Logger log 
    = Logger.getLogger(SpyXAResource.class.getName());
  protected final static L10N L = new L10N(SpyXAResource.class);

  // The underlying resource
  private XAResource _xaResource;

  private String _id;

  /**
   * Creates a new SpyXAResource
   */
  public SpyXAResource(String id, XAResource resource)
  {
    _xaResource = resource;
    _id = id;
  }

  /**
   * Returns the underlying resource.
   */
  public XAResource getXAResource()
  {
    return _xaResource;
  }
  
  protected long start()
  {
    return CurrentTime.getExactTime();
  }
  
  protected void log(long start, String msg)
  {
    long delta = CurrentTime.getExactTime() - start;
    
    log.fine("[" + delta + "ms] " + _id + ":" + msg);
  }

  /**
   * Sets the transaction timeout.
   */
  @Override
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    long start = start();
    
    try {
      boolean ok = _xaResource.setTransactionTimeout(seconds);
      
      if (log.isLoggable(Level.FINE))
        log(start, "set-transaction-timeout(" + seconds + ")->" + ok);

      return ok;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Gets the transaction timeout.
   */
  @Override
  public int getTransactionTimeout()
    throws XAException
  {
    long start = start();
    
    try {
      int seconds = _xaResource.getTransactionTimeout();
    
      if (log.isLoggable(Level.FINE))
        log(start, "transaction-timeout()->" + seconds);

      return seconds;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Returns true if the underlying RM is the same.
   */
  @Override
  public boolean isSameRM(XAResource resource)
    throws XAException
  {
    long start = start();
    
    try {
      if (resource instanceof SpyXAResource)
        resource = ((SpyXAResource) resource).getXAResource();

      boolean same = _xaResource.isSameRM(resource);

      if (log.isLoggable(Level.FINE))
        log(start, "is-same-rm(resource=" + resource + ")->" + same);

      return same;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  @Override
  public void start(Xid xid, int flags)
    throws XAException
  {
    long start = start();
    
    try {
      _xaResource.start(xid, flags);
      
      if (log.isLoggable(Level.FINE)) { 
        String flagName = "";

        if ((flags & TMJOIN) != 0)
          flagName += ",join";
        if ((flags & TMRESUME) != 0)
          flagName += ",resume";
      
        log(start, "start(xid=" + xid + flagName + ")");
      }
    } catch (XAException e) {
      log(start, "exn-start(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-start(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  @Override
  public void end(Xid xid, int flags)
    throws XAException
  {
    long start = start();
    
    try {
      _xaResource.end(xid, flags);
      
      if (log.isLoggable(Level.FINE)) { 
        String flagName = "";

        if ((flags & TMFAIL) != 0)
          flagName += ",fail";
        if ((flags & TMSUSPEND) != 0)
          flagName += ",suspend";
      
        log(start, "end(xid=" + xid + flagName + ")");
      }
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      log(start, "exn-end(" + xid + ") -> " + e);
      
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-end(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  @Override
  public int prepare(Xid xid)
    throws XAException
  {
    long start = start();
    
    try {
      int value = _xaResource.prepare(xid);
    
      if (log.isLoggable(Level.FINE))
        log(start, "prepare(xid=" + xid + ")->" + value);

      return value;
    } catch (XAException e) {
      log(start, "exn-prepare(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-prepare(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Commits the resource
   */
  @Override
  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    long start = start();
    
    try {
      _xaResource.commit(xid, onePhase);
      
      if (log.isLoggable(Level.FINE))
        log(start, "commit(xid=" + xid + (onePhase ? ",1P)" : ",2P)"));
    } catch (XAException e) {
      log(start, "exn-commit(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-commit(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  @Override
  public void rollback(Xid xid)
    throws XAException
  {
    long start = start();
    
    try {
      _xaResource.rollback(xid);

      if (log.isLoggable(Level.FINE))
        log(start, "rollback(xid=" + xid + ")");
    } catch (XAException e) {
      log(start, "exn-rollback(xid) -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-rollback(xid) -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  @Override
  public Xid []recover(int flags)
    throws XAException
  {
    long start = start();
    
    try {
      Xid [] xids = _xaResource.recover(flags);
      
      if (log.isLoggable(Level.FINE)){ 
        String flagString = "";

        if ((flags & XAResource.TMSTARTRSCAN) != 0)
          flagString += "start";

        if ((flags & XAResource.TMENDRSCAN) != 0) {
          if (! flagString.equals(""))
            flagString += ",";

          flagString += "end";
        }
      
        log(start, "recover(flags=" + flagString + ")");
      }
      
      return xids;
    } catch (XAException e) {
      log(start, "exn-recover() " + e);
      
      log.fine(e.toString());
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-recover() " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Forgets the transaction
   */
  @Override
  public void forget(Xid xid)
    throws XAException
  {
    long start = start();
    
    try {
      _xaResource.forget(xid);
      
      if (log.isLoggable(Level.FINE))
        log(start, "forget(xid=" + xid + ")");
    } catch (XAException e) {
      log(start, "exn-force(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log(start, "exn-force(" + xid + ") -> " + e);
      
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[id=" + _id + ",resource=" + _xaResource + "]");
  }
}
