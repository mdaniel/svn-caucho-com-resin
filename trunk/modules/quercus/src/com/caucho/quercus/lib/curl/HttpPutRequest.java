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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.env.Callable;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.file.BinaryInput;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ProtocolException;

/**
 * Represents a PUT Http request.
 */
public class HttpPutRequest
  extends CurlHttpRequest
{
  public HttpPutRequest(CurlResource curlResource)
  {
    super(curlResource);
  }

  /**
   * Initializes the connection.
   */
  protected boolean init(Env env)
    throws ProtocolException
  {
    if (! super.init(env)) {
      return false;
    }

    getHttpConnection().setDoOutput(true);

    return true;
  }

  /**
   * Transfer data to the server.
   */
  protected void transfer(Env env)
    throws IOException
  {
    super.transfer(env);

    CurlHttpConnection conn = getHttpConnection();
    OutputStream out = conn.getOutputStream();

    CurlResource curl = getCurlResource();

    try {
      BinaryInput in = curl.getUploadFile();
      long length = curl.getUploadFileSize();

      long totalWritten = 0;

      if (curl.getReadCallback() != null) {
        Callable callback = curl.getReadCallback();

        Value fileV = env.wrapJava(in);
        LongValue lengthV = LongValue.create(length);

        while (totalWritten < length) {
          StringValue str
            = callback.call(env, fileV, lengthV).toStringValue(env);

          int count = str.length();

          if (count == 0) {
            break;
          }

          str.writeTo(out);

          totalWritten += count;
        }
      }
      else {
        byte []buffer = new byte[1024 * 4];

        while (totalWritten < length) {
          int count = in.read(buffer, 0, buffer.length);

          if (count < 0) {
            break;
          }

          out.write(buffer, 0, count);

          totalWritten += count;
        }
      }
    }
    finally {
      out.close();
    }
  }
}
