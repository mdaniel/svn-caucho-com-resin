/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;

import com.caucho.quercus.resources.StreamContextResource;
import com.caucho.quercus.resources.StreamResource;

import com.caucho.vfs.TempBuffer;

/**
 * Handling the PHP Stream API
 */
public class StreamModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(StreamModule.class);
  private static final Logger log
    = Logger.getLogger(StreamModule.class.getName());

  public static final int STREAM_FILTER_READ = 1;
  public static final int STREAM_FILTER_WRITE = 2;
  public static final int STREAM_FILTER_ALL = 3;

  public static final int PSFS_PASS_ON = 2;
  public static final int PSFS_FEED_ME = 1;
  public static final int PSFS_ERR_FATAL = 0;

  public static final int STREAM_USE_PATH = 1;
  public static final int STREAM_REPORT_ERRORS = 8;

  public static final int STREAM_CLIENT_ASYNC_CONNECT = 2;
  public static final int STREAM_CLIENT_CONNECT = 4;
  public static final int STREAM_CLIENT_PERSISTENT = 1;

  public static final int STREAM_SERVER_BIND = 4;
  public static final int STREAM_SERVER_LISTEN = 8;

  private static final HashMap<String,Value> _constMap =
          new HashMap<String,Value>();

  static {
    _constMap.put("STREAM_FILTER_READ", new LongValue(STREAM_FILTER_READ));
    _constMap.put("STREAM_FILTER_WRITE", new LongValue(STREAM_FILTER_WRITE));
    _constMap.put("STREAM_FILTER_ALL", new LongValue(STREAM_FILTER_ALL));

    _constMap.put("PSFS_PASS_ON", new LongValue(PSFS_PASS_ON));
    _constMap.put("PSFS_FEED_ME", new LongValue(PSFS_FEED_ME));
    _constMap.put("PSFS_ERR_FATAL", new LongValue(PSFS_ERR_FATAL));

    _constMap.put("STREAM_USE_PATH", new LongValue(STREAM_USE_PATH));
    _constMap.put("STREAM_REPORT_ERRORS", new LongValue(STREAM_REPORT_ERRORS));

    _constMap.put("STREAM_CLIENT_ASYNC_CONNECT",
                  new LongValue(STREAM_CLIENT_ASYNC_CONNECT));
    _constMap.put("STREAM_CLIENT_CONNECT",
                  new LongValue(STREAM_CLIENT_CONNECT));
    _constMap.put("STREAM_CLIENT_PERSISTENT",
                  new LongValue(STREAM_CLIENT_PERSISTENT));

    _constMap.put("STREAM_SERVER_BIND",
                  new LongValue(STREAM_SERVER_BIND));
    _constMap.put("STREAM_SERVER_LISTEN",
                  new LongValue(STREAM_SERVER_LISTEN));
  }

  /**
   * Adds the constant to the PHP engine's constant map.
   *
   * @return the new constant chain
   */
  public Map<String,Value> getConstMap()
  {
    return _constMap;
  }

  /**
   * Creates a stream context.
   */
  public Value stream_context_create(@Optional ArrayValue options)
  {
    return new StreamContextResource(options);
  }

  /**
   * Returns the options from a stream context.
   */
  public Value stream_context_get_options(Env env, Value resource)
  {
    if (resource instanceof StreamContextResource) {
      return ((StreamContextResource) resource).getOptions();
    }
    else {
      env.warning(L.l("expected resource at '{0}'", resource));

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the default stream context.
   */
  public Value stream_context_get_default(Env env,
                                          @Optional ArrayValue options)
  {
    StreamContextResource context = env.getDefaultStreamContext();

    if (options != null)
      context.setOptions(options);

    return context;
  }

  /**
   * Set an options for a stream context.
   */
  public boolean stream_context_set_option(Env env,
                                           Value resource,
                                           String wrapper,
                                           String option,
                                           Value value)
  {
    if (resource instanceof StreamContextResource) {
      StreamContextResource context = (StreamContextResource) resource;

      context.setOption(wrapper, option, value);

      return true;
    }
    else {
      env.warning(L.l("expected resource at '{0}'", resource));

      return false;
    }
  }

  /**
   * Sets parameters for the context
   */
  public boolean stream_context_set_params(Env env,
                                           Value resource,
                                           ArrayValue value)
  {
    // XXX: not sure what this does

    return false;
  }

  /**
   * Copies from an input stream to an output stream
   */
  public long stream_copy_to_stream(Env env,
                                    StreamResource in,
                                    StreamResource out,
                                    @Optional("-1") int length,
                                    @Optional int offset)
    throws IOException
  {
    if (in == null) {
      env.warning("first argument must be an input stream");
      return -1;
    }

    if (out == null) {
      env.warning("second argument must be an output stream");
      return -1;
    }

    TempBuffer temp = TempBuffer.allocate();
    byte []buffer = temp.getBuffer();

    while (offset-- > 0)
      in.read();

    if (length < 0)
      length = Integer.MAX_VALUE;

    long bytesWritten = 0;

    while (length > 0) {
      int sublen = buffer.length;

      if (length < sublen)
        sublen = (int) length;

      sublen = in.read(buffer, 0, sublen);
      if (sublen < 0)
        return bytesWritten;

      out.write(buffer, 0, sublen);

      bytesWritten += sublen;
      length -= sublen;
    }

    TempBuffer.free(temp);

    return bytesWritten;
  }

  /**
   * Returns the rest of the file as a string
   *
   * @param filename the file's name
   * @param useIncludePath if true, use the include path
   * @param context the resource context
   */
  public static Value stream_get_contents(Env env,
                                          StreamResource in,
                                          @Optional("-1") long maxLen,
                                          @Optional long offset)
    throws IOException
  {
    if (in == null) {
      env.warning("first argument must be an open stream");
      return BooleanValue.FALSE;
    }

    StringBuilder sb = new StringBuilder();

    int ch;

    if (maxLen < 0)
      maxLen = Integer.MAX_VALUE;

    while (offset-- > 0)
      in.read();

    while (maxLen-- > 0 && (ch = in.read()) >= 0) {
      sb.append((char) ch);
    }

    // XXX: handle offset and maxlen

    return new StringValueImpl(sb.toString());
  }

  /**
   * Returns the next line
   */
  public Value stream_get_line(StreamResource file,
                               @Optional("-1") long length)
    throws IOException
  {
    if (file == null)
      return BooleanValue.FALSE;

    if (length < 0)
      length = Integer.MAX_VALUE;

    String line = file.readLine();

    if (line == null)
      return BooleanValue.FALSE;
    else if (line.endsWith("\r\n"))
      return new StringValueImpl(line.substring(0, line.length() - 2));
    else if (line.endsWith("\r"))
      return new StringValueImpl(line.substring(0, line.length() - 1));
    else if (line.endsWith("\n"))
      return new StringValueImpl(line.substring(0, line.length() - 1));
    else
      return new StringValueImpl(line);
  }

  /**
   * Returns the available transports.
   */
  public Value stream_get_transports()
  {
    ArrayValue value = new ArrayValueImpl();

    value.append(new StringValueImpl("tcp"));
    value.append(new StringValueImpl("udp"));

    return value;
  }

  /**
   * Returns the available wrappers.
   */
  public Value stream_get_wrappers()
  {
    ArrayValue value = new ArrayValueImpl();

    value.append(new StringValueImpl("quercus"));
    value.append(new StringValueImpl("file"));
    value.append(new StringValueImpl("http"));
    value.append(new StringValueImpl("ftp"));

    return value;
  }

  /**
   * Sets the write buffer.
   */
  public static int stream_set_write_buffer(StreamResource stream,
                                            int bufferSize)
  {
    return 0;
  }
}

