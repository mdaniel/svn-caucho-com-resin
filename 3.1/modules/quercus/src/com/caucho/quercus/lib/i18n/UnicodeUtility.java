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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.i18n;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.*;
import com.caucho.vfs.*;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.*;

public class UnicodeUtility {
  private static final Logger log
    = Logger.getLogger(UnicodeUtility.class.getName());

  public static StringValue decodeEncode(Env env,
					StringValue str,
					String inCharset,
					String outCharset)
    throws UnsupportedEncodingException
  {
    return decodeEncode(env, str, inCharset, outCharset, 0, Integer.MAX_VALUE);
  }

  public static StringValue decodeEncode(Env env,
					 StringValue str,
					 String inCharset,
					 String outCharset,
					 int offset)
    throws UnsupportedEncodingException
  {
    return decodeEncode(env, str, inCharset, outCharset,
                        offset, Integer.MAX_VALUE);
  }

  /**
   * Decodes and encodes to specified charsets at the same time.
   */
  public static StringValue decodeEncode(Env env,
					 StringValue str,
					 String inCharset,
					 String outCharset,
					 int offset,
					 int length)
    throws UnsupportedEncodingException
  {
    TempCharBuffer tb = TempCharBuffer.allocate();
    char[] charBuf = tb.getBuffer();

    try {
      Reader in;

      try {
        in = str.toReader(inCharset);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
    
        in = str.toReader("utf-8");
      }

      TempStream ts = new TempStream();
      WriteStream out = new WriteStream(ts);

      try {
        out.setEncoding(outCharset);
      } catch (IOException e) {
        log.log(Level.WARNING, e.toString(), e);
    
        out.setEncoding("utf-8");
      }

      while (offset > 0) {
        if (in.read() < 0)
          break;
        offset--;
      }

      int sublen;

      while (length > 0 &&
             (sublen = in.read(charBuf, 0, charBuf.length)) >= 0) {

        sublen = Math.min(length, sublen);

        out.print(charBuf, 0, sublen);
        length -= sublen;
      }

      out.flush();

      StringValue sb = env.createBinaryBuilder();
      for (TempBuffer ptr = ts.getHead(); ptr != null; ptr = ptr.getNext()) {
        sb.append(ptr.getBuffer(), 0, ptr.getLength());
      }
      
      return sb;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }

    finally {
      TempCharBuffer.free(tb);
    }
  }
}
