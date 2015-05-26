/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;


/**
 * service for managing snapshots.
 */
public class SnapshotService
{
  private SnapshotServiceAdmin _admin;
  
  public void init()
  {
    _admin = new SnapshotServiceAdmin();
    _admin.register();
  }
}
