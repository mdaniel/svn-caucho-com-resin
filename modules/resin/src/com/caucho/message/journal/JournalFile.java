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

package com.caucho.message.journal;

import java.io.IOException;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
import com.caucho.vfs.Path;

/**
 * Interface for the transaction log.
 * 
 * MQueueJournal is not thread safe. It is intended to be used by a
 * single thread.
 */
public final class JournalFile
{
  public static final int BLOCK_BITS = BlockStore.BLOCK_BITS;
  public static final int BLOCK_SIZE = BlockStore.BLOCK_SIZE;
  
  public static final long FILE_HEADER_OFFSET = 2 * BLOCK_SIZE;
  public static final int FILE_HEADER_SIZE = BLOCK_SIZE;
  public static final int MIN_BLOCK_COUNT = 4;
  public static final long FILE_DATA_OFFSET = FILE_HEADER_OFFSET + 2 * FILE_HEADER_SIZE;
  
  public static final int FH_OFF_SEQ = 0;
  public static final int FH_CHECKPOINT_ADDR = 8;
  public static final int FH_CHECKPOINT_OFFSET = 16;
  public static final int FH_END = 24;
  
  public static final int MIN_FLIP_SIZE = 256;
  
  public static final int PAD_SIZE = 128;
  public static final int PAD_MASK = PAD_SIZE - 1;
  
  public static final int HOFF_LENGTH = 0;
  public static final int HOFF_FLAG = 2;
  public static final int HOFF_CODE = 3;
  public static final int HOFF_ID = 4;
  public static final int HOFF_SEQ = 12;
  public static final int HEADER_SIZE = 20;
  
  public static final int FLAG_INIT = 0x80;
  public static final int FLAG_FIN = 0x40;
  public static final int FLAG_SEQ = 0x0f;
  

  
  private final Path _path;
  private BlockStore _blockStore;
  
  private long _flipAddress;
  private boolean _isFlipFree;
  private boolean _isFlipA;
  
  private long _tailAddress;
  private int _tailOffset;
  private Block _tailBlock;
  
  private Block _headerBlockA;
  private Block _headerBlockB;
  
  private int _seq;
  
  public JournalFile(Path path,
                     JournalRecoverListener listener)
  {
    _path = path;
    
    if (path == null)
      throw new NullPointerException();
    
    if (listener == null)
      throw new NullPointerException();
    
    setMinFlipSize(256 * 1024);
    
    init(listener);
  }
  
  public void setMinFlipSize(long size)
  {
    size += (BLOCK_SIZE - size % BLOCK_SIZE) % BLOCK_SIZE;
    
    if (size < 2 * BLOCK_SIZE)
      size = 2 * BLOCK_SIZE;
    
    int count = (int) (size / BLOCK_SIZE);
    
    _flipAddress = 2 * BLOCK_SIZE * count + FILE_DATA_OFFSET;
  }
  
  private void init(JournalRecoverListener listener)
  {
    try {
      _blockStore = BlockStore.create(_path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    _tailAddress = FILE_DATA_OFFSET;
    _tailOffset = 0;
    
    _isFlipFree = true;
    
    try {
      long headerAddrA = FILE_HEADER_OFFSET + 0 * BLOCK_SIZE;
      
      _headerBlockA = _blockStore.readBlock(headerAddrA);
      
      long headerAddrB = FILE_HEADER_OFFSET + BLOCK_SIZE;
      
      _headerBlockB = _blockStore.readBlock(headerAddrB);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  
    try {
      recover(listener);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void recover(JournalRecoverListener listener)
    throws IOException
  {
    long fileSize = _path.getLength();
    
    Block block = null;
    
    int seqA = 0;
    long checkpointAddrA = 0;
    int checkpointOffsetA = 0;
    
    int seqB = 0;
    long checkpointAddrB = 0;
    int checkpointOffsetB = 0;

    block = _headerBlockA;
      
    byte []buffer = block.getBuffer();
      
    seqA = buffer[FH_OFF_SEQ] & FLAG_SEQ;
      
    if ((seqA & 1) != 0)
      seqA = 0;
      
    checkpointAddrA = readLong(buffer, FH_CHECKPOINT_ADDR);
    checkpointOffsetA = readInt(buffer, FH_CHECKPOINT_OFFSET);
      
    if (checkpointAddrA < FILE_DATA_OFFSET)
      checkpointAddrA = FILE_DATA_OFFSET;

    block = _headerBlockB;
      
    buffer = block.getBuffer();
      
    seqB = buffer[FH_OFF_SEQ] & 0xff;
      
    if ((seqB & 1) != 1)
      seqB = 0;
      
    checkpointAddrB = readLong(buffer, FH_CHECKPOINT_ADDR);
    checkpointOffsetB = readInt(buffer, FH_CHECKPOINT_OFFSET);
      
    if (checkpointAddrB < FILE_DATA_OFFSET + BLOCK_SIZE)
      checkpointAddrB = FILE_DATA_OFFSET + BLOCK_SIZE;
    
    int nextA = (seqA + 1) & FLAG_SEQ;
    if (nextA == 0)
      nextA = 2;
    
    boolean isFlipA = (nextA != seqB);
    
    if (seqA == 0) {
      // initial state
      _seq = FLAG_SEQ;
      _isFlipA = false;
      flip();
      _isFlipFree = true;
      return;
    }
    
    boolean isFlipFree = true;
        
    _seq = isFlipA ? seqB : seqA;
    _tailAddress = isFlipA ? checkpointAddrB : checkpointAddrA;
    _tailOffset = isFlipA ? checkpointOffsetB : checkpointOffsetA;
    
    if (_tailOffset < BLOCK_SIZE && _tailAddress < fileSize & _seq != 0) {
      try {
        while (recoverEntry(listener)) {
          isFlipFree = false;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    _seq = isFlipA ? seqA : seqB;
    _tailAddress = isFlipA ? checkpointAddrA : checkpointAddrB;
    _tailOffset = isFlipA ? checkpointOffsetA : checkpointOffsetB;
    
    boolean isRecover = false;
    
    if (_tailOffset < BLOCK_SIZE && _tailAddress < fileSize & _seq != 0) {
      try {
        while (recoverEntry(listener)) {
          isRecover = true;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
    _isFlipFree = isFlipFree;
  }
  
  private boolean recoverEntry(JournalRecoverListener listener)
    throws IOException
  {
    long tailAddress = _tailAddress;
    int i = _tailOffset;
    
    Block block = _blockStore.readBlock(tailAddress);
    try {
      block.read();
      
      byte []buffer = block.getBuffer();
      
      int len = readShort(buffer, i + HOFF_LENGTH);
      
      int flags = buffer[i + HOFF_FLAG] & 0xff;
      int code = buffer[i + HOFF_CODE] & 0xff;
      
      int journalSeq = flags & FLAG_SEQ;
      
      long appId = readLong(buffer, i + HOFF_ID);
      long appSeq = readLong(buffer, i + HOFF_SEQ);
      
      i += HEADER_SIZE;
      
      if (journalSeq != _seq || buffer.length <= len) {
        return false;
      }
      
      int offset = i;
      
      i += len;
      i += (PAD_SIZE - i) & PAD_MASK;
      
      if (i == BLOCK_SIZE) {
        _tailAddress = tailAddress + 2 * BLOCK_SIZE;
        _tailOffset = 0;
      }
      else {
        _tailOffset = i;
      }
      
      listener.onEntry(code, 
                       (flags & FLAG_INIT) != 0,
                       (flags & FLAG_FIN) != 0,
                       appId,
                       appSeq,
                       _blockStore, tailAddress, offset, 
                       len);
    } finally {
      block.free();
    }
    
    return true;
  }
  
  public final void write(int code, boolean isInit, boolean isFin,
                          long id, long seq,
                          byte []buffer, int offset, int length,
                          JournalResult result)
    throws IOException
  {
    boolean isFirst = true;
    
    do {
      int sublen = writeImpl(code, isInit, isFin,
                             id, seq,
                             buffer, offset, length,
                             result, isFirst);
      
      if (length <= sublen && isFirst) {
        result.init2(0, 0, 0);
        break;
      }
      
      isFirst = false;
      
      offset += sublen;
      length -= sublen;
      
      isInit = false;
      
    } while (length > 0);
    
    if (_flipAddress < _tailAddress && _isFlipFree) {
      flip();
    }
  }
  
  private int writeImpl(int code, boolean isInit, boolean isFin,
                        long id, long seq,
                        byte []buffer, int offset, int length,
                        JournalResult result, boolean isFirst)
    throws IOException
  {
    if (_tailBlock == null) {
      _tailBlock = _blockStore.readBlock(_tailAddress);
    }
    
    byte []tailBuffer = _tailBlock.getBuffer();
    int i = _tailOffset;
    
    int sublen = tailBuffer.length - i - HEADER_SIZE;
    
    if (length <= sublen) {
      sublen = length;
    }
    else {
      isFin = false;
    }
    
    tailBuffer[i + HOFF_LENGTH + 0] = (byte) (sublen >> 8);
    tailBuffer[i + HOFF_LENGTH + 1] = (byte) (sublen);
    
    int flags = _seq;
    
    if (isInit) {
      flags |= FLAG_INIT;
    }
    
    if (isFin) {
      flags |= FLAG_FIN;
    }
    
    tailBuffer[i + HOFF_FLAG] = (byte) flags;
    tailBuffer[i + HOFF_CODE] = (byte) code;
    
    writeLong(tailBuffer, i + HOFF_ID, id);
    writeLong(tailBuffer, i + HOFF_SEQ, seq);
    
    i += HEADER_SIZE;

    System.arraycopy(buffer, offset, tailBuffer, i, sublen);
    
    if (isFirst) {
      result.init1(_blockStore, _tailAddress, i, sublen);
    }
    else {
      result.init2(_tailAddress, i, sublen);
    }
    
    i += sublen;
    i += (PAD_SIZE - i) & PAD_MASK;
    
    // _tailBlock.setDirty(_tailOffset, i);
    _tailBlock.setDirtyExact(0, i);
    
    if (i == BLOCK_SIZE) {
      Block block = _tailBlock;
      _tailBlock = null;
      
      block.free();
      block.commit();
      
      _tailAddress += 2 * BLOCK_SIZE;
      _tailOffset = 0;
    }
    else {
      _tailOffset = i;
    }
    
    return sublen;
  }
  
  public void checkpoint(long blockAddr, int offset, int length)
    throws IOException
  {
    int tail = offset + length;
    
    tail += (PAD_SIZE - tail) & PAD_MASK;
    
    if (BLOCK_SIZE <= tail) {
      blockAddr += 2 * BLOCK_SIZE;
      tail = 0;
    }
    
    boolean isCheckpointA = ((blockAddr >> BLOCK_BITS) & 1) == 0;
    
    long headerAddr = FILE_HEADER_OFFSET + (isCheckpointA ? 0 : BLOCK_SIZE);
    
    Block block = isCheckpointA ? _headerBlockA : _headerBlockB;
      
    byte []buffer = block.getBuffer();
      
    writeLong(buffer, FH_CHECKPOINT_ADDR, blockAddr);
    writeInt(buffer, FH_CHECKPOINT_OFFSET, tail);
      
    // block.setDirty(0, FH_END);
    block.setDirtyExact(0, FH_END);
    
    if (isCheckpointA == _isFlipA && ! _isFlipFree) {
      // if checkpoint clears the flip, then clear it as well.
      headerAddr = FILE_HEADER_OFFSET + (isCheckpointA ? BLOCK_SIZE : 0);
      
      block = isCheckpointA ? _headerBlockB : _headerBlockA;
        
      buffer = block.getBuffer();
        
      writeLong(buffer, FH_CHECKPOINT_ADDR, 0);
      writeInt(buffer, FH_CHECKPOINT_OFFSET, Integer.MAX_VALUE / 2);
        
      // block.setDirty(0, FH_END);
      block.setDirtyExact(0, FH_END);
      
      _isFlipFree = true;
      
      _headerBlockA.commit();
      _headerBlockB.commit();
    }
  }
  
  private void flip()
    throws IOException
  {
    Block tailBlock = _tailBlock;
    _tailBlock = null;
    
    if (tailBlock != null) {
      tailBlock.free();
      tailBlock.commit();
    }
    
    int seq = (_seq + 1) & FLAG_SEQ;
    
    if (seq < 2)
      seq = 2;
    
    _seq = seq;
    _isFlipA = (seq & 1) == 0;
    _tailAddress = (FILE_DATA_OFFSET + (_isFlipA ? 0 : BLOCK_SIZE));
    _tailOffset = 0;
    
    _isFlipFree = false;
    
    Block block = _isFlipA ? _headerBlockA : _headerBlockB;
    
    byte []buffer = block.getBuffer();
      
    buffer[FH_OFF_SEQ] = (byte) _seq;
      
    writeLong(buffer, FH_CHECKPOINT_ADDR, 0);
    writeLong(buffer, FH_CHECKPOINT_OFFSET, 0);
      
    // block.setDirty(0, FH_END);
    block.setDirtyExact(0, FH_END);
  }
  
  private static int readShort(byte []buffer, int offset)
  {
    return (((buffer[offset] & 0xff) << 8)
           + (buffer[offset + 1] & 0xff));
  }
  
  private static int readInt(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xff) << 24)
           + ((buffer[offset + 1] & 0xff) << 16)
           + ((buffer[offset + 2] & 0xff) << 8)
           + ((buffer[offset + 3] & 0xff)));
  }
  
  private static void writeInt(byte []buffer, int offset, int value)
  {
    buffer[offset + 0] = (byte) (value >> 24);
    buffer[offset + 1] = (byte) (value >> 16);
    buffer[offset + 2] = (byte) (value >> 8);
    buffer[offset + 3] = (byte) (value >> 0);
  }
  
  private static long readLong(byte []buffer, int offset)
  {
    return (((buffer[offset + 0] & 0xffL) << 56)
           + ((buffer[offset + 1] & 0xffL) << 48)
           + ((buffer[offset + 2] & 0xffL) << 40)
           + ((buffer[offset + 3] & 0xffL) << 32)
           + ((buffer[offset + 4] & 0xffL) << 24)
           + ((buffer[offset + 5] & 0xffL) << 16)
           + ((buffer[offset + 6] & 0xffL) << 8)
           + ((buffer[offset + 7] & 0xffL)));
  }
  
  private static void writeLong(byte []buffer, int offset, long value)
  {
    buffer[offset + 0] = (byte) (value >> 56);
    buffer[offset + 1] = (byte) (value >> 48);
    buffer[offset + 2] = (byte) (value >> 40);
    buffer[offset + 3] = (byte) (value >> 32);
    buffer[offset + 4] = (byte) (value >> 24);
    buffer[offset + 5] = (byte) (value >> 16);
    buffer[offset + 6] = (byte) (value >> 8);
    buffer[offset + 7] = (byte) (value >> 0);
  }
  
  public void close()
  {
    Block tailBlock = _tailBlock;
    _tailBlock = null;
    
    Block headerBlockA = _headerBlockA;
    _headerBlockA = null;
    
    Block headerBlockB = _headerBlockB;
    _headerBlockB = null;
    
    if (tailBlock != null) {
      tailBlock.free();
    }
    
    if (headerBlockA != null) {
      headerBlockA.free();
    }
    
    if (headerBlockB != null) {
      headerBlockB.free();
    }
    
    _blockStore.flush();
    
    _blockStore.close();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
}
