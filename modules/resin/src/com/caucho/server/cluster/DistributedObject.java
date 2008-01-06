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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Base class for the session stores.
 */
public interface DistributedObject {
  /**
   * Returns the object's id.
   */
  public String getId();

  /**
   * Returns the object's saved CRC value.
   */
  public long getCRC();
  
  /**
   * Sets the object's saved CRC value.
   */
  public void setCRC(long crc);

  /**
   * Returns the object's update count
   */
  public int getUpdateCount();
  
  /**
   * Sets the object's saved update count
   */
  public void setUpdateCount(int count);

  /**
   * Deserializes the object from the input stream.
   *
   * @param is the input stream to deserialize from.
   */
  public void load(ObjectInputStream is)
    throws IOException;
  
  /**
   * Serializes the object to from the output stream.
   *
   * @param os the output stream to deserialize to.
   */
  public void store(ObjectOutputStream os)
    throws IOException;

  public ObjectBacking getBacking();

  public ClusterServer getOwningServer();
  public ClusterServer getBackupServer();
}
