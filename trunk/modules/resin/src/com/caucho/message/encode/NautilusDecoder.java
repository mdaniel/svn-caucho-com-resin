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

package com.caucho.message.encode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.caucho.message.MessageDecoder;

/**
 * string decoder from utf-8
 */
public class NautilusDecoder extends AbstractNautilusDecoder<Object>
{
  public static final NautilusDecoder DECODER = new NautilusDecoder();
  
  private final MessageDecoder<?> []_decoderArray;
  
  public NautilusDecoder()
  {
    _decoderArray = new MessageDecoder[256];
    
    for (int i = 0; i < _decoderArray.length; i++) {
      _decoderArray[i] = new UnsupportedDecoder("code = 0x" + Integer.toHexString(i));
    }
    
    _decoderArray[NautilusCodes.NULL.getValue()] = NullDecoder.DECODER;
    _decoderArray[NautilusCodes.STRING.getValue()] = StringDecoder.DECODER;
  }
  
  @Override
  public Object decode(InputStream is)
    throws IOException
  {
    int code = is.read();
    
    if (code < 0)
      throw new EOFException();
    
    return _decoderArray[code].decode(is);
  }
}
