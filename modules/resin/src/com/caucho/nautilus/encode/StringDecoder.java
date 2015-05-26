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

package com.caucho.nautilus.encode;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.util.Utf8;

/**
 * string decoder from utf-8
 */
public class StringDecoder extends AbstractNautilusDecoder<String>
{
  public static final StringDecoder DECODER = new StringDecoder();
  
  @Override
  public String decode(InputStream is)
    throws IOException
  {
    int length = decodeLength(is);
    
    int ch;
    StringBuilder sb = new StringBuilder();

    while ((ch = Utf8.read(is)) >= 0) {
      sb.append((char) ch);
    }
    
    return sb.toString();
  }
}
