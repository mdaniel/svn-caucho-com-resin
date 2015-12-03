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

import java.io.IOException;
import java.io.OutputStream;

import com.caucho.quercus.env.Callable;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.lib.file.BinaryInput;

abstract public class PostBody
{
  private boolean _isValid = true;

  static PostBody create(Env env, CurlResource curl)
  {
    PostBody post;

    Value data = curl.getPostBody();
    Callable bodyFun = curl.getReadCallback();

    BinaryInput file = curl.getUploadFile();
    long length = curl.getUploadFileSize();

    if (data == null && bodyFun == null) {
      return null;
    }
    else if (bodyFun != null) {
      post = new UserBody(env, curl, bodyFun, file, length);
    }
    else if (data.isArray()) {
      post = new MultipartBody(env, data);
    }
    else {
      post = new UrlEncodedBody(env, data);
    }

    if (post.isValid()) {
      return post;
    }
    else {
      return null;
    }
  }

  public boolean isChunked()
  {
    return false;
  }

  public void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  public boolean isValid()
  {
    return true;
  }

  abstract public long getContentLength();
  abstract public String getContentType();
  abstract public void writeTo(Env env, OutputStream os) throws IOException;
}
