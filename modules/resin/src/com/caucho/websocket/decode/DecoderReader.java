/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.websocket.decode;

import java.io.Reader;
import java.io.IOException;

import javax.websocket.Decoder;
import javax.websocket.MessageHandler;

import com.caucho.v5.util.ModulePrivate;

/**
 * Callback for a binary stream decoder
 */
@ModulePrivate
public class DecoderReader<T> implements MessageHandler.Whole<Reader> {
  private final MessageHandler.Whole<T> _handler;
  private final Decoder.TextStream<T> _decoder;
  
  public DecoderReader(Decoder.TextStream<T> decoder,
                       MessageHandler.Whole<T> handler)
  {
    _decoder = decoder;
    _handler = handler;
  }
  
  @Override
  public void onMessage(Reader is)
  {
    try {
      T value = _decoder.decode(is);
      
      _handler.onMessage(value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
