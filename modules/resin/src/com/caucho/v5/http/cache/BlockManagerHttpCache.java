/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.store.io.StoreBuilder;
import com.caucho.v5.store.io.StoreReadWrite;
import com.caucho.v5.util.ConcurrentArrayList;

/**
 * A block in the http cache.
 */
class BlockManagerHttpCache
{
  public final int SIZE = 8192;
  public final int SIZE_SEGMENT = 1 << 20;
  
  private StoreReadWrite _store;
  
  private ConcurrentArrayList<SegmentCache> _segmentList
    = new ConcurrentArrayList<>(SegmentCache.class);
  
  private AtomicLong _tail = new AtomicLong();
  
  public BlockManagerHttpCache(Path path)
  {
    StoreBuilder storeBuilder = new StoreBuilder(path);
    storeBuilder.ampManager(AmpSystem.currentManager());
    
    try {
      _store = storeBuilder.build();
      _store.create();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  BlockHttpCache allocate()
  {
    while (true) {
      for (SegmentCache segment : _segmentList) {
        BlockHttpCache block = segment.allocate();
      
        if (block != null) {
          return block;
        }
      }
      
      expandSegment();
    }
    
    /*
    //long address = _tail.addAndGet(SIZE);
    
    SegmentCache segment = getSegment(address);
    
    return new BlockHttpCache(segment, address);
    */
  }
  
  private void expandSegment()
  {
    synchronized (_segmentList) {
      int sizeSegment = SIZE_SEGMENT;
      long addressSegment = _segmentList.size() * sizeSegment;
      
      SegmentCache segment = new SegmentCache(_store, addressSegment, sizeSegment);
  
      _segmentList.add(segment);
    }
  }
  
  private SegmentCache getSegment(long address)
  {
    int indexSegment = (int) (address / SIZE_SEGMENT);
    
    synchronized (_segmentList) {
      if (_segmentList.size() <= indexSegment) {
        int sizeSegment = SIZE_SEGMENT;
        long addressSegment = _segmentList.size() * sizeSegment;
      
        SegmentCache segment = new SegmentCache(_store, addressSegment, sizeSegment);
  
        _segmentList.add(segment);
      }
      
      return _segmentList.get(indexSegment);
    }
    // _store.
    // throw new UnsupportedOperationException(getClass().getName());
  }
  
  void close()
  {
    StoreReadWrite store = _store;
    _store = null;
    
    if (store != null) {
      store.close();
    }
  }
}
