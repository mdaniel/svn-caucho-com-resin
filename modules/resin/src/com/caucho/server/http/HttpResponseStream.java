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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.http;

import com.caucho.server.connection.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.logging.*;

public class HttpResponseStream extends ResponseStream {
  private static final Logger log
    = Logger.getLogger(HttpResponseStream.class.getName());

  private static final L10N L = new L10N(HttpResponseStream.class);

  private static final int _tailChunkedLength = 7;
  private static final byte []_tailChunked
    = new byte[] {'\r', '\n', '0', '\r', '\n', '\r', '\n'};

  private final byte []_buffer = new byte[16];

  private HttpResponse _response;
  private WriteStream _next;

  private boolean _isChunkedEncoding;
  private int _bufferStartOffset;

  HttpResponseStream(HttpResponse response, WriteStream next)
  {
    super(response);

    _response = response;
    _next = next;
  }

  /**
   * initializes the Response stream at the beginning of a request.
   */
  @Override
  public void startRequest()
  {
    super.startRequest();

    _isChunkedEncoding = false;
    _bufferStartOffset = 0;
  }

  //
  // implementations
  //

  @Override
  protected void writeHeaders(int length)
    throws IOException
  {
    super.writeHeaders(length);

    _isChunkedEncoding = _response.isChunkedEncoding();
  }

  @Override
  protected byte []getNextBuffer()
  {
    return _next.getBuffer();
  }

  @Override
  protected int getNextStartOffset()
  {
    if (_isChunkedEncoding) {
      if (_bufferStartOffset == 0) {
        _bufferStartOffset = _next.getBufferOffset() + 8;
        _next.setBufferOffset(_bufferStartOffset);
      }
    }

    return _bufferStartOffset;
  }

  @Override
  protected int getNextBufferOffset()
    throws IOException
  {
    if (_isChunkedEncoding) {
      if (_bufferStartOffset == 0) {
        _bufferStartOffset = _next.getBufferOffset() + 8;
        _next.setBufferOffset(_bufferStartOffset);
      }
    }

    return _next.getBufferOffset();
  }

  @Override
  protected void setNextBufferOffset(int offset)
  {
    _next.setBufferOffset(offset);
  }

  @Override
  protected byte []writeNextBuffer(int offset)
    throws IOException
  {
    WriteStream next = _next;

    int bufferStart = _bufferStartOffset;

    if (log.isLoggable(Level.FINER))
      log.finer(dbgId() + "write-chunk2(" + (offset - bufferStart) + ")");

    if (bufferStart > 0) {
      byte []buffer = next.getBuffer();

      writeChunkHeader(buffer, bufferStart, offset - bufferStart);

      _bufferStartOffset = 0;
    }

    return next.nextBuffer(offset);
  }

  @Override
  public void flushNext()
    throws IOException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "flush()");

    if (_bufferStartOffset > 0) {
      // server/0506
      _next.setBufferOffset(_bufferStartOffset - 8);
    }

    _next.flush();

    _bufferStartOffset = 0;
  }

  @Override
  protected void closeNext()
    throws IOException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "close()");

    _next.close();

    _bufferStartOffset = 0;
  }

  @Override
  protected void writeTail()
    throws IOException
  {
    int bufferStart = _bufferStartOffset;

    int bufferOffset = _next.getBufferOffset();

    if (bufferStart < bufferOffset) {
      if (log.isLoggable(Level.FINER))
        log.finer(dbgId() + "write-chunk-tail(" + (bufferOffset - bufferStart) + ")");
    }

    if (! _isChunkedEncoding)
      return;

    if (bufferStart > 0) {
      byte []buffer = _next.getBuffer();

      writeChunkHeader(buffer, bufferStart, bufferOffset - bufferStart);

      _bufferStartOffset = 0;
    }

    ArrayList<String> footerKeys = _response.getFooterKeys();

    if (footerKeys.size() == 0)
      _next.write(_tailChunked, 0, _tailChunkedLength);
    else {
      ArrayList<String> footerValues = _response.getFooterValues();

      _next.print("\r\n0\r\n");

      for (int i = 0; i < footerKeys.size(); i++) {
        _next.print(footerKeys.get(i));
        _next.print(": ");
        _next.print(footerValues.get(i));
        _next.print("\r\n");
      }

      _next.print("\r\n");
    }

    if (log.isLoggable(Level.FINER))
      log.finer(dbgId() + "write-chunk-tail(" + _tailChunkedLength + ")");
  }

  /**
   * Fills the chunk header.
   */
  private void writeChunkHeader(byte []buffer, int start, int length)
    throws IOException
  {
    buffer[start - 8] = (byte) '\r';
    buffer[start - 7] = (byte) '\n';
    buffer[start - 6] = hexDigit(length >> 12);
    buffer[start - 5] = hexDigit(length >> 8);
    buffer[start - 4] = hexDigit(length >> 4);
    buffer[start - 3] = hexDigit(length);
    buffer[start - 2] = (byte) '\r';
    buffer[start - 1] = (byte) '\n';
  }

  /**
   * Returns the hex digit for the value.
   */
  private static byte hexDigit(int value)
  {
    value &= 0xf;

    if (value <= 9)
      return (byte) ('0' + value);
    else
      return (byte) ('a' + value - 10);
  }
}
