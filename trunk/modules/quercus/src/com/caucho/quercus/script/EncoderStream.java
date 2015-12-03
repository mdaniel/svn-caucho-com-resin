/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 */

package com.caucho.quercus.script;

import java.io.IOException;
import java.io.Reader;

import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.lib.i18n.Encoder;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;

public class EncoderStream extends StreamImpl
{
  private Encoder _encoder;
  private Reader _reader;

  private StringBuilderValue _sb;
  private StringBuilder _inputSb;

  private EncoderStream(Reader reader, Encoder encoder)
  {
    _encoder = encoder;
    _reader = reader;

    _sb = new StringBuilderValue();
    _inputSb = new StringBuilder();
  }

  public static ReadStream open(Reader reader, String charset)
  {
    Encoder encoder = Encoder.create(charset);

    EncoderStream ss = new EncoderStream(reader, encoder);
    return new ReadStream(ss);
  }

  @Override
  public Path getPath()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean canRead()
  {
    return true;
  }

  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    _inputSb.setLength(0);
    _sb.setLength(0);

    int ch0 = _reader.read();

    if (ch0 < 0) {
      return -1;
    }

    _inputSb.append((char) ch0);

    int ch1 = -1;

    if (Character.isHighSurrogate((char) ch0)) {
      ch1 = _reader.read();
    }

    if (ch1 >= 0) {
      _inputSb.append((char) ch1);
    }

    _encoder.encode(_sb, _inputSb, 0, _inputSb.length());

    int len = _sb.length();

    byte []bytes = _sb.getBuffer();
    for (int i = 0; i < len; i++) {
      buf[offset + i] = bytes[i];
    }

    return len;
  }

  @Override
  public int getAvailable()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
}


