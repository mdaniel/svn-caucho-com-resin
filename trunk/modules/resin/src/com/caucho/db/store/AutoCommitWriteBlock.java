/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.db.store;

import com.caucho.util.L10N;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a write (dirty) block.
 *
 * The AutoCommitWriteBlock 
 */
public class AutoCommitWriteBlock extends WriteBlock {
  private static final Logger log
    = Logger.getLogger(AutoCommitWriteBlock.class.getName());
  private static final L10N L = new L10N(AutoCommitWriteBlock.class);

  public AutoCommitWriteBlock(Block block)
    throws IOException
  {
    super(block);

    Thread.dumpStack();
    
    // The block should already be allocated
    // block.allocate();
    block.read();
  }

  /**
   * The buffer for the auto-commit is the same as the read buffer.
   */
  public byte []getBuffer()
  {
    return _block.getBuffer();
  }

  public void setDirty(int minDirty, int maxDirty)
  {
    _block.setDirty(minDirty, maxDirty);
  }

  public void setFlushDirtyOnCommit(boolean flushOnCommit)
  {
    _block.setFlushDirtyOnCommit(flushOnCommit);
  }

  public void commit()
    throws IOException
  {
    _block.commit();
  }

  /*
  public void free()
  {
    super.free();

    if (isFree())
      destroy();
  }
  */

  /**
   * Closes the write block.
   */
  protected void freeImpl()
  {
    super.freeImpl();
    
    Block block = _block;
    _block = block;

    if (block != null) {
      try {
	block.commit();
      } catch (Throwable e) {
	log.log(Level.FINE, e.toString(), e);
      }
      
      block.free();

      throw new IllegalStateException();
      //close();
    }
  }

  public String toString()
  {
    return "AutoCommitWriteBlock[" + getStore() + "," + getBlockId() / Store.BLOCK_SIZE + "]";
  }
}
