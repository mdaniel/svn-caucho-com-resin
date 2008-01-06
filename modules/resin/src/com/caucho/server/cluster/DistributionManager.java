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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Manages sessions in a web-app.
 */
public class DistributionManager  {
  static protected L10N L = new L10N(DistributionManager.class);
  static protected final Logger log = Log.open(DistributionManager.class);

  // factory for creating objects
  // private ObjectFactory objectFactory;

  // cached objects
  private LruCache _objects;
  
  private Store _store;
  private boolean _reloadEachRequest;
  private boolean _alwaysSave;
  private boolean _saveOnShutdown;

  // List of activation listeners
  private ArrayList _activationListeners;

  // If true, serialization errors should not be logged
  private boolean _ignoreSerializationErrors = false;

  // private Srun []_sessionGroup;

  private boolean _isClosed;

  /**
   * Creates and initializes a new distribution manager
   */
  public DistributionManager(Store store)
    throws Exception
  {
  }

  /**
   * Returns the debug log
   */
  public Logger getDebug()
  {
    return log;
  }

  /**
   * Returns the underlying session store of the session manager.
   */
  Store getStore()
  {
    return _store;
  }

  /**
   * True if the objects should always be saved.
   */
  boolean getAlwaysSave()
  {
    return _alwaysSave;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  boolean getSaveOnShutdown()
  {
    return _saveOnShutdown;
  }

  /**
   * True if serialization errors should just fail silently.
   */
  boolean getIgnoreSerializationErrors()
  {
    return _ignoreSerializationErrors;
  }

  /**
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }
}
