/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.IOException;

import com.caucho.v5.io.OutputStreamWithBuffer;
import com.caucho.v5.util.L10N;

/**
 * A block in the http cache.
 */
class BlockHttpCache
{
  private static final L10N L = new L10N(BlockHttpCache.class);

  public static final int SIZE = 8192;
  
  private final SegmentCache _segment;
  private final long _address;
  
  private boolean _isClosed;
  
  BlockHttpCache(SegmentCache segment, long address)
  {
    _segment = segment;
    _address = address;
    
    if (_address < _segment.getAddress()
        || _segment.getAddress() + segment.getLength() < address + SIZE) {
      throw new IllegalStateException(L.l("Segment size mismatch 0x{0}:0x{1} against {2}",
                                          Long.toHexString(_address),
                                          Long.toHexString(SIZE),
                                          _segment));
    }
  }
  
  /**
   * Read into the output stream
   */
  public void read(int blockOffset, 
                   OutputStreamWithBuffer os, 
                   int length)
    throws IOException
  {
    if (_isClosed) {
      throw new IllegalStateException(L.l("read from closed cache"));
    }
    
    while (length > 0) {
      byte []bufferOut = os.getBuffer();
      int offsetOut = os.getBufferOffset();
      
      int sublen = Math.min(bufferOut.length - offsetOut, length);
      
      if (sublen == 0) {
        os.nextBuffer(offsetOut);
        bufferOut = os.getBuffer();
        offsetOut = os.getBufferOffset();
        
        sublen = Math.min(bufferOut.length - offsetOut, length);
      }
    
      _segment.read(_address + blockOffset, bufferOut, offsetOut, sublen);
      
      length -= sublen;
      offsetOut += sublen;
      blockOffset += sublen;
      
      os.setBufferOffset(offsetOut);
    }
  }

  public void write(int blockOffset, byte[] buffer, int offset, int sublen)
  {
    _segment.write(_address + blockOffset, buffer, offset, sublen);
  }

  public void free()
  {
    if (! _isClosed) { 
      _isClosed = true;
    
      _segment.free(_address);
    }
  }
}
