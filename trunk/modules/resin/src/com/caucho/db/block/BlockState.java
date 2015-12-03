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

package com.caucho.db.block;

/**
 * Represents the state of a block.
 */
public enum BlockState {
  INIT {
    @Override
    public BlockState toValid() { return VALID; }
    
    @Override
    public BlockState toDestroy() { return DESTROYED; }
  },
  
  VALID {
    @Override
    public boolean isValid() { return true; }
    
    @Override
    public BlockState toValid() { return VALID; }
    
    @Override
    public BlockState toWrite() { return WRITE_QUEUED; }
    
    @Override
    public BlockState toDestroy() { return DESTROYED; }
  },
  
  WRITE_QUEUED {
    @Override
    public boolean isValid() { return true; }

    @Override
    public boolean isWrite() { return true; }
    
    @Override
    public BlockState toValid() { return VALID; }
    
    @Override
    public BlockState toWrite() { return this; }
    
    @Override
    public BlockState toDestroy() { return this; }
  },
   
  DESTROYED {
    @Override
    public boolean isDestroyed() { return true; }
    
    @Override
    public BlockState toWrite() { return this; }
    
    @Override
    public BlockState toDestroy() { return this; }
  };
  
  public boolean isValid()
  {
    return false;
  }
  
  public boolean isWrite()
  {
    return false;
  }
  
  public boolean isDestroyed()
  {
    return false;
  }
  
  public BlockState toValid()
  {
    throw new IllegalStateException(toString() + ": toValid()");
  }
  
  public BlockState toWrite()
  {
    throw new IllegalStateException(toString() + ": toWrite()");
  }
  
  public BlockState toDestroy()
  {
    throw new IllegalStateException(toString() + ": toDestroy()");
  }
  
  public BlockState toState(BlockState toState)
  {
    switch (toState) {
    case VALID:
      return toValid();
      
    case WRITE_QUEUED:
      return toWrite();
      
    case DESTROYED:
      return toDestroy();
      
    default:
      throw new IllegalStateException(toString()
                                      + ": toState " + String.valueOf(toState));
    }
  }
}
