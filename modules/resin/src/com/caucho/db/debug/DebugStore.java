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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.debug;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import com.caucho.db.store.Store;

/**
 * Manager for a basic Java-based database.
 */
public class DebugStore {
  private static final Logger log = Log.open(DebugStore.class);
  private static final L10N L = new L10N(DebugStore.class);

  Path _path;
  Store _store;
  
  public DebugStore(Path path)
    throws Exception
  {
    _path = path;
    
    _store = Store.create(path);
    _store.init();
  }

  public static void main(String []args)
    throws Exception
  {
    if (args.length == 0) {
      System.out.println("usage: DebugStore store.db");
      return;
    }

    Path path = Vfs.lookup(args[0]);

    WriteStream out = Vfs.openWrite(System.out);
    
    new DebugStore(path).test(out);

    out.close();
  }
  
  public void test(WriteStream out)
    throws Exception
  {
    out.println("file-size   : " + _store.getFileSize());
    out.println("block-count : " + _store.getBlockCount());

    debugAllocation(out, _store.getAllocationTable(), _store.getBlockCount());
    
    debugFragments(out, _store.getAllocationTable(), _store.getBlockCount());
  }

  private void debugAllocation(WriteStream out, byte []allocTable, long count)
    throws IOException
  {
    out.println();

    for (int i = 0; i < count; i++) {
      int v = allocTable[i / 4];

      int code = (v >> (2 * (i % 4))) & 0x3;

      switch (code) {
      case Store.ALLOC_FREE:
	out.print('.');
	break;
      case Store.ALLOC_ROW:
	out.print('r');
	break;
      case Store.ALLOC_USED:
	out.print('u');
	break;
      case Store.ALLOC_FRAGMENT:
	out.print('f');
	break;
      default:
	out.print('?');
      }
      
      if (i % 64 == 63)
	out.println();
      else if (i % 8 == 7)
	out.print(' ');
    }
    
    out.println();
  }

  private void debugFragments(WriteStream out, byte []allocTable, long count)
    throws Exception
  {
    long totalUsed = 0;
    
    byte []block = new byte[Store.BLOCK_SIZE];
    
    for (int i = 0; i < count; i++) {
      int v = allocTable[i / 4];

      int code = (v >> (2 * (i % 4))) & 0x3;

      if (code == Store.ALLOC_FRAGMENT) {
	readBlock(block, i);

	totalUsed += debugFragmentBlock(out, block, i);
      }
    }

    out.println();
    out.println("Total-used: " + totalUsed);
  }

  private int debugFragmentBlock(WriteStream out, byte []block, long count)
    throws IOException
  {
    int used = readShort(block, 0);
    int fragCount = readShort(block, 2);

    out.println();
    
    out.println("Fragment Block " + count + ": " +
		       used + "/" + fragCount);

    int offset = 4;
    for (int i = 0; offset < used; i++) {
      if ((i & 7) == 0) {
	if (i != 0)
	  out.println();
	
	out.print(readShort(block, offset) + ":");
	offset += 2;
      }

      int len = readShort(block, offset);

      out.print(len + " ");

      offset += 2 + len;
    }
    out.println();

    return used;
  }

  private void readBlock(byte []block, long count)
    throws Exception
  {
    ReadStream is = _path.openRead();

    try {
      is.skip(count * Store.BLOCK_SIZE);

      is.read(block, 0, block.length);
    } finally {
      is.close();
    }
  }

  private int readShort(byte []block, int offset)
  {
    return ((block[offset] & 0xff) << 8) + (block[offset + 1] & 0xff);
  }
}
