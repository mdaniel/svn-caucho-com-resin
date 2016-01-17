/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.caucho.v5.store.io.InStore;
import com.caucho.v5.store.io.OutStore;
import com.caucho.v5.store.io.StoreReadWrite;
import com.caucho.v5.util.FreeRing;
import com.caucho.v5.util.L10N;

/**
 * A block in the http cache.
 */
class SegmentCache {
  private static final L10N L = new L10N(SegmentCache.class);
  
  private long _address;
  private int _length;

  private InStore _read;
  private OutStore _write;
  
  private final int _blockCount;
  private final int _bucketCount;
  
  private final FreeRing<InStore> _freeRead = new FreeRing<>(64);
  private final FreeRing<OutStore> _freeWrite = new FreeRing<>(64);
  
  private final AtomicLongArray _allocArray;
  private final AtomicInteger _freeBlockCount = new AtomicInteger();

  SegmentCache(StoreReadWrite store, long address, int length)
  {
    if (_address < 0 || length <= 0 || Integer.bitCount(length) != 1) {
      throw new IllegalArgumentException(L.l("Invalid arguments: address {0} length {1}",
                                             address, length));
      
    }
    _address = address;
    _length = length;
    
    _write = store.openWrite(_address, _length);
    _read = store.openRead(_address, _length);

    _blockCount = (int) (_length / BlockHttpCache.SIZE);
    
    _freeBlockCount.set(_blockCount);
    
    _bucketCount = (_blockCount + 63) / 64;
    
    if (_bucketCount <= 0) {
      throw new IllegalStateException(L.l("Invalid buckets blocks={0}", _blockCount));
    }
    
    _allocArray = new AtomicLongArray(_bucketCount);
  }

  long getAddress()
  {
    return _address;
  }

  int getLength()
  {
    return _length;
  }
  

  public void read(long address, byte[] buffer, int offset, int length)
  {
    InStore read = _freeRead.allocate();
    
    if (read == null) {
      read = _read.clone();
    }
    
    read.read(address, buffer, offset, length);
    
    _freeRead.free(read);
  }

  public void write(long address, byte[] buffer, int offset, int length)
  {
    OutStore write = _freeWrite.allocate();
    
    if (write == null) {
      write = _write.clone();
    }
    
    write.write(address, buffer, offset, length);
    
    _freeWrite.free(write);
  }

  public BlockHttpCache allocate()
  {
    if (_freeBlockCount.get() == 0) {
      return null;
    }
    
    for (int i = 0; i < _bucketCount; i++) {
      long oldMask = _allocArray.get(i);
      long newMask;
      
      if (oldMask != -1L) {
        int bit = Long.numberOfTrailingZeros(~oldMask);
        
        newMask = oldMask + (1L << bit);
        
        if (_allocArray.compareAndSet(i, oldMask, newMask)) {
          int block = i * 64 + bit;
          
          long address = _address + BlockHttpCache.SIZE * block;
          
          _freeBlockCount.addAndGet(-1);
          
          return new BlockHttpCache(this, address);
        }
      }
    }
    
    return null;
  }

  public void free(long address)
  {
    long index = (int) (address - _address) / BlockHttpCache.SIZE;
    
    int bucket = (int) (index >> 8);
    int item = (int) (index & 0x3f);
    
    long oldValue;
    long newValue;
    
    do {
      oldValue = _allocArray.get(bucket);
      newValue = oldValue & ~(1L << item);
    } while (! _allocArray.compareAndSet(bucket, oldValue, newValue));
    
    _freeBlockCount.addAndGet(1);
  }

  void close()
  {
    InStore read = _read;
    _read = null;

    OutStore write = _write;
    _write = null;

    if (read != null) {
      read.close();
    }

    if (write != null) {
      write.close();
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() 
            + "[0x" + Long.toHexString(_address)
            + ",len=0x" + Long.toHexString(_length) + "]");
  }
}
