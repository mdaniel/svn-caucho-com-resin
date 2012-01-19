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

package com.caucho.db.debug;

import java.io.IOException;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Manager for a basic Java-based database.
 */
public class DebugStore {
  Path _path;
  BlockStore _store;
  
  public DebugStore(Path path)
    throws Exception
  {
    _path = path;

    _store = BlockStore.createNoMmap(path);
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
    
    StringBuilder sb = new StringBuilder();
    
    int lastData = 0;

    for (int i = 0; i < count; i++) {
      int v = allocTable[2 * i];

      int code = v & 0xf;
      
      switch (code) {
      case BlockStore.ALLOC_FREE:
        sb.append('.');
        break;
      case BlockStore.ALLOC_ROW:
        sb.append('r');
        break;
      case BlockStore.ALLOC_DATA:
        sb.append('+');
        break;
      case BlockStore.ALLOC_INODE_PTR:
        sb.append('p');
        break;
      case BlockStore.ALLOC_MINI_FRAG:
        sb.append('f');
        break;
      case BlockStore.ALLOC_INDEX:
        sb.append('i');
        break;
      default:
        sb.append('?');
        break;
      }
      
      if (code != BlockStore.ALLOC_FREE) {
        lastData = sb.length();
      }
      
      if (i % 64 == 63) {
        sb.append("\n");
      }
      else if (i % 8 == 7) {
        sb.append(' ');
      }
    }
    
    sb.setLength(lastData);
    
    out.println(sb);
  }

  private void debugFragments(WriteStream out, byte []allocTable, long count)
    throws Exception
  {
    int totalUsed = 0;
    
    StringBuilder sb = new StringBuilder();
    
    int lastData = 0;
    
    int len = 0;

    for (int i = 0; i < count; i++) {
      int v = allocTable[2 * i];

      int code = v & 0xf;
      
      if (code != BlockStore.ALLOC_MINI_FRAG) {
        continue;
      }
      
      Block block = _store.readBlock(i * BlockStore.BLOCK_SIZE);
      
      try {
        block.read();
        byte []buffer = block.getBuffer();

        for (int j = 0; j < (BlockStore.MINI_FRAG_PER_BLOCK + 7) / 8; j++) {
          int mask = buffer[j + BlockStore.MINI_FRAG_ALLOC_OFFSET] & 0xff;

          sb.append(bits(mask >> 4));
          sb.append(bits(mask));
      
          len++;
          if (len % 32 == 0)
            sb.append("\n");
          else if (len % 4 == 0)
            sb.append(" ");
        }
      } finally {
        block.free();
      }
    }
    
    if (sb.length() > 0) {
      out.println();
      out.println("Fragments:");
      out.println(sb);
    }
    
    out.println();
    out.println("Total-used: " + totalUsed);
  }
  
  private char bits(int b)
  {
    b = b & 0xf;
    
    if (b == 0)
      return '.';
    else if (b == 15)
      return '+';
    else if (b < 10)
      return (char) (b + '0');
    else
      return (char) (b + 'a' - 10);
  }

  private void readBlock(byte []block, long count)
    throws Exception
  {
    ReadStream is = _path.openRead();

    try {
      is.skip(count * BlockStore.BLOCK_SIZE);

      is.read(block, 0, block.length);
    } finally {
      is.close();
    }
  }

  private int readShort(byte []block, int offset)
  {
    return ((block[offset] & 0xff) << 8) + (block[offset + 1] & 0xff);
  }
  
  public void close()
  {
    _store.close();
  }
}
